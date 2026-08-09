package io.github.chechelpo.frplm.utils.converters;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Objects;

/**
 * Decodes an uploaded raster image and encodes it as WebP.
 *
 * <p>This class depends only on the standard ImageIO API at compile time.
 * A WebP-capable ImageIO writer must be present at runtime.</p>
 */
public final class WebpImageConverter {

    public static final float DEFAULT_QUALITY = 0.90f;

    /**
     * Prevents excessively large decoded images from consuming unbounded
     * memory. This is a decoded-pixel limit, not a compressed-file-size limit.
     */
    public static final long DEFAULT_MAX_PIXELS = 100_000_000L;

    private static final String WEBP_FORMAT_NAME = "webp";
    private static final String WEBP_MEDIA_TYPE = "image/webp";

    private WebpImageConverter() {
    }

    /**
     * Converts the first image contained in {@code source} to WebP.
     *
     * <p>The caller retains ownership of {@code source}. This method does not
     * close it.</p>
     */
    public static void convert(
            InputStream source,
            Path target
    ) throws IOException {
        convert(
                source,
                target,
                DEFAULT_QUALITY,
                DEFAULT_MAX_PIXELS
        );
    }

    /**
     * Converts the first image contained in {@code source} to WebP.
     *
     * @param source    encoded source image
     * @param target    destination WebP file
     * @param quality   compression quality in the range {@code [0.0, 1.0]}
     * @param maxPixels maximum permitted decoded pixel count
     */
    public static void convert(
            InputStream source,
            Path target,
            float quality,
            long maxPixels
    ) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");

        validateOptions(quality, maxPixels);

        BufferedImage decoded =
                decodeAndValidate(source, maxPixels);

        BufferedImage normalized =
                normalizeImage(decoded);

        writeWebp(
                normalized,
                target,
                quality
        );
    }

    /**
     * Returns whether the current runtime has an ImageIO WebP writer.
     */
    public static boolean isWebpWriterAvailable() {
        return findWebpWriter().hasNext();
    }

    private static BufferedImage decodeAndValidate(
            InputStream source,
            long maxPixels
    ) throws IOException {
        /*
         * ImageInputStream.close() may close its underlying stream.
         * Wrap the caller-owned stream so that close() is ignored.
         */
        InputStream nonClosingSource =
                new NonClosingInputStream(source);

        try (ImageInputStream imageInput =
                     ImageIO.createImageInputStream(nonClosingSource)) {
            if (imageInput == null) {
                throw new InvalidImageException(
                        "Could not create an image input stream"
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw new InvalidImageException(
                        "The uploaded file is not a supported image"
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(
                        imageInput,
                        true,
                        true
                );

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                validateDimensions(
                        width,
                        height,
                        maxPixels
                );

                BufferedImage image =
                        reader.read(0);

                if (image == null) {
                    throw new InvalidImageException(
                            "The uploaded image could not be decoded"
                    );
                }

                return image;
            } catch (IndexOutOfBoundsException exception) {
                throw new InvalidImageException(
                        "The uploaded file contains no readable image",
                        exception
                );
            } finally {
                reader.dispose();
            }
        }
    }

    private static void validateDimensions(
            int width,
            int height,
            long maxPixels
    ) throws InvalidImageException {
        if (width <= 0 || height <= 0) {
            throw new InvalidImageException(
                    "Image dimensions must be positive"
            );
        }

        long pixelCount =
                (long) width * (long) height;

        if (pixelCount > maxPixels) {
            throw new InvalidImageException(
                    "Decoded image exceeds the maximum pixel count"
            );
        }
    }

    /**
     * Converts custom and indexed image types into a predictable RGB or ARGB
     * representation accepted by most WebP ImageIO implementations.
     */
    private static BufferedImage normalizeImage(
            BufferedImage source
    ) {
        boolean hasAlpha =
                source.getColorModel().hasAlpha();

        int targetType =
                hasAlpha
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB;

        if (source.getType() == targetType) {
            return source;
        }

        BufferedImage normalized =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        targetType
                );

        Graphics2D graphics =
                normalized.createGraphics();

        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return normalized;
    }

    private static void writeWebp(
            BufferedImage image,
            Path target,
            float quality
    ) throws IOException {
        ImageWriter writer =
                requireWebpWriter();

        try (
                OutputStream output =
                        Files.newOutputStream(
                                target,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.TRUNCATE_EXISTING
                        )
        ) {
            ImageOutputStream imageOutput =
                    ImageIO.createImageOutputStream(output);

            if (imageOutput == null) {
                throw new IOException(
                        "Could not create an ImageIO output stream"
                );
            }

            try (imageOutput) {
                writer.setOutput(imageOutput);

                ImageWriteParam parameters =
                        createWriteParameters(
                                writer,
                                quality
                        );

                writer.write(
                        null,
                        new IIOImage(
                                image,
                                null,
                                null
                        ),
                        parameters
                );

                imageOutput.flush();
            }
        } finally {
            writer.dispose();
        }
    }

    private static ImageWriteParam createWriteParameters(
            ImageWriter writer,
            float quality
    ) {
        ImageWriteParam parameters =
                writer.getDefaultWriteParam();

        if (!parameters.canWriteCompressed()) {
            return parameters;
        }

        try {
            parameters.setCompressionMode(
                    ImageWriteParam.MODE_EXPLICIT
            );

            /*
             * Some ImageIO implementations require a compression type before
             * compression quality can be configured.
             */
            String[] compressionTypes =
                    parameters.getCompressionTypes();

            if (compressionTypes != null
                    && compressionTypes.length > 0) {
                String preferredType =
                        findLossyCompressionType(
                                compressionTypes
                        );

                parameters.setCompressionType(
                        preferredType
                );
            }

            parameters.setCompressionQuality(
                    quality
            );

            return parameters;
        } catch (
                IllegalArgumentException
                | IllegalStateException
                | UnsupportedOperationException exception
        ) {
            /*
             * The installed provider does not expose compression configuration
             * in the conventional ImageIO form. Use its defaults.
             */
            return writer.getDefaultWriteParam();
        }
    }

    private static String findLossyCompressionType(
            String[] compressionTypes
    ) {
        for (String compressionType : compressionTypes) {
            if (compressionType != null
                    && compressionType.equalsIgnoreCase("lossy")) {
                return compressionType;
            }
        }

        return compressionTypes[0];
    }

    private static ImageWriter requireWebpWriter()
            throws IOException {
        Iterator<ImageWriter> writers =
                findWebpWriter();

        if (!writers.hasNext()) {
            throw new IOException(
                    "No WebP ImageIO writer is installed"
            );
        }

        return writers.next();
    }

    private static Iterator<ImageWriter> findWebpWriter() {
        Iterator<ImageWriter> byMediaType =
                ImageIO.getImageWritersByMIMEType(
                        WEBP_MEDIA_TYPE
                );

        if (byMediaType.hasNext()) {
            return byMediaType;
        }

        return ImageIO.getImageWritersByFormatName(
                WEBP_FORMAT_NAME
        );
    }

    private static void validateOptions(
            float quality,
            long maxPixels
    ) {
        if (!Float.isFinite(quality)
                || quality < 0.0f
                || quality > 1.0f) {
            throw new IllegalArgumentException(
                    "WebP quality must be between 0.0 and 1.0"
            );
        }

        if (maxPixels <= 0) {
            throw new IllegalArgumentException(
                    "Maximum pixel count must be positive"
            );
        }
    }

    /**
     * Signals that the supplied bytes are not a valid or permitted image.
     *
     * <p>Controllers should normally map this exception to HTTP 400 rather
     * than HTTP 500.</p>
     */
    public static final class InvalidImageException
            extends IOException {

        public InvalidImageException(
                String message
        ) {
            super(message);
        }

        public InvalidImageException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }

    /**
     * Prevents ImageIO from assuming ownership of a caller-owned stream.
     */
    private static final class NonClosingInputStream
            extends FilterInputStream {

        private NonClosingInputStream(
                InputStream input
        ) {
            super(input);
        }

        @Override
        public void close() {
            // Ownership remains with the caller.
        }
    }
}