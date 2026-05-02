package chechelpo.frplm.utils.IO;

import chechelpo.frplm.exceptions.types.IllegalFileState;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FileReaders {
    private FileReaders() {}

    /**
     * Reads image bytes directly (useful when you don't need BufferedImage processing).
     */
    public static @NotNull byte[] readImageBytes(@NotNull MultipartFile file)
            throws IOException, IllegalArgumentException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        return file.getBytes();  // simplest & most efficient
        // or: file.getInputStream().readAllBytes() if you prefer (Java 9+)
    }

    public static @NotNull BufferedImage readImage(@NotNull MultipartFile file) throws IOException {
        if (file.isEmpty())  throw new IllegalArgumentException("Null file");

        BufferedImage image;
        InputStream inputStream = file.getInputStream();
        image = ImageIO.read(inputStream);

        if (image == null) throw new IllegalArgumentException("Uploaded file is not a recognised image format");

        return image;
    }

    /**
     * Reads a BufferedImage from path.
     * @param path to the img to read.
     * @return the image as a BufferedImage
     * @throws IllegalFileState if invalid image
     */
    public static @NotNull Optional<BufferedImage> readImageFromPath(@NotNull Path path) throws IllegalFileState {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            return Optional.empty();
        }

        try (InputStream in = Files.newInputStream(path)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IllegalFileState("File not a valid image: " + path);
            }
            return Optional.of(img);
        } catch (IOException e) {
            System.err.println("Error reading file: " + path + ": " + e.getMessage());
            throw new IllegalFileState("Error reading file: " + path + ": " + e.getMessage());
        }
    }
}
