package io.github.chechelpo.frplm.utils.IO;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Random-access counterpart to {@link ZipBuilder}.
 *
 * <p>The reader validates archive entry names when it is opened, rejects
 * duplicate/colliding paths, and protects filesystem extraction against ZIP
 * traversal. Callers that only need an entity asset should prefer
 * {@link #openFile(String)} and pass the returned stream to the relevant asset
 * store rather than extracting directly into application storage.</p>
 */
public final class ZipReader implements AutoCloseable {

    private final ZipFile zip;
    private final ObjectMapper objectMapper;
    private final Path temporaryArchive;

    /**
     * Opens an archive already available as a file.
     */
    public ZipReader(
            Path archive,
            ObjectMapper objectMapper
    ) throws IOException {
        Objects.requireNonNull(archive, "archive");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.temporaryArchive = null;
        this.zip = new ZipFile(
                archive.toAbsolutePath().normalize().toFile()
        );

        try {
            validateArchive();
        } catch (IOException | RuntimeException exception) {
            zip.close();
            throw exception;
        }
    }

    private ZipReader(
            Path temporaryArchive,
            ObjectMapper objectMapper,
            boolean deleteOnClose
    ) throws IOException {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.temporaryArchive = deleteOnClose
                ? temporaryArchive
                : null;
        this.zip = new ZipFile(temporaryArchive.toFile());

        try {
            validateArchive();
        } catch (IOException | RuntimeException exception) {
            try {
                zip.close();
            } finally {
                if (deleteOnClose) {
                    Files.deleteIfExists(temporaryArchive);
                }
            }
            throw exception;
        }
    }

    /**
     * Opens an archive from a stream.
     *
     * <p>The stream is copied to a temporary file so entries can later be read
     * in arbitrary order. The supplied stream remains owned by the caller and
     * is not closed by this method.</p>
     */
    public static ZipReader open(
            InputStream inputStream,
            ObjectMapper objectMapper
    ) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        Objects.requireNonNull(objectMapper, "objectMapper");

        Path temporaryArchive = Files.createTempFile(
                "frplm-archive-",
                ".zip"
        );

        try {
            Files.copy(
                    inputStream,
                    temporaryArchive,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new ZipReader(
                    temporaryArchive,
                    objectMapper,
                    true
            );
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporaryArchive);
            throw exception;
        }
    }

    /**
     * Deserializes a required JSON entry.
     */
    public <T> T readJson(
            String zipPath,
            Class<T> valueType
    ) throws IOException {
        Objects.requireNonNull(valueType, "valueType");

        String entryName = normalizeFilePath(zipPath);
        ZipEntry entry = requireFileEntry(entryName);

        try (InputStream input = zip.getInputStream(entry)) {
            return objectMapper.readValue(input, valueType);
        }
    }

    /**
     * Opens a file entry when present.
     *
     * <p>The returned stream must be closed by the caller and must not outlive
     * this {@code ZipReader}.</p>
     */
    public Optional<InputStream> openFile(
            String zipPath
    ) throws IOException {
        String entryName = normalizeFilePath(zipPath);
        ZipEntry entry = zip.getEntry(entryName);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isDirectory()) {
            throw new IOException(
                    "ZIP entry is a directory, not a file: " + entryName
            );
        }

        return Optional.of(zip.getInputStream(entry));
    }

    public boolean containsFile(String zipPath) {
        String entryName = normalizeFilePath(zipPath);
        ZipEntry entry = zip.getEntry(entryName);

        return entry != null && !entry.isDirectory();
    }

    public boolean containsDirectory(String zipDirectory) {
        String archiveRoot = normalizeDirectoryPath(zipDirectory);

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(archiveRoot)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts one required file entry.
     */
    public ZipReader extractFile(
            String zipPath,
            Path targetFile
    ) throws IOException {
        Objects.requireNonNull(targetFile, "targetFile");

        String entryName = normalizeFilePath(zipPath);
        ZipEntry entry = requireFileEntry(entryName);
        Path target = targetFile.toAbsolutePath().normalize();
        Path parent = target.getParent();

        if (parent == null) {
            throw new IllegalArgumentException(
                    "Target file has no parent: " + target
            );
        }

        createSafeDirectories(parent);

        if (Files.isSymbolicLink(target)) {
            throw new IOException(
                    "Refusing to overwrite symbolic link: " + target
            );
        }

        try (InputStream input = zip.getInputStream(entry)) {
            Files.copy(
                    input,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        return this;
    }

    /**
     * Extracts every entry below {@code zipDirectory} into
     * {@code targetDirectory}. An absent archive directory is an error; use
     * {@link #containsDirectory(String)} when the directory is optional.
     */
    public ZipReader extractDirectory(
            String zipDirectory,
            Path targetDirectory
    ) throws IOException {
        Objects.requireNonNull(targetDirectory, "targetDirectory");

        String archiveRoot = normalizeDirectoryPath(zipDirectory);
        Path targetRoot = targetDirectory
                .toAbsolutePath()
                .normalize();

        createSafeDirectories(targetRoot);

        boolean found = false;
        Enumeration<? extends ZipEntry> entries = zip.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (!entryName.startsWith(archiveRoot)) {
                continue;
            }

            found = true;

            String relativeName = entryName.substring(
                    archiveRoot.length()
            );

            if (relativeName.isEmpty()) {
                continue;
            }

            Path output = resolveExtractionTarget(
                    targetRoot,
                    relativeName
            );

            if (entry.isDirectory()) {
                createSafeDirectories(output);
                continue;
            }

            Path parent = output.getParent();
            if (parent == null) {
                throw new IOException(
                        "Invalid extraction target: " + output
                );
            }

            createSafeDirectories(parent);

            if (Files.isSymbolicLink(output)) {
                throw new IOException(
                        "Refusing to overwrite symbolic link: " + output
                );
            }

            try (InputStream input = zip.getInputStream(entry)) {
                Files.copy(
                        input,
                        output,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }

        if (!found) {
            throw new IOException(
                    "ZIP directory does not exist: " + archiveRoot
            );
        }

        return this;
    }

    private ZipEntry requireFileEntry(
            String entryName
    ) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);

        if (entry == null) {
            throw new IOException(
                    "ZIP entry does not exist: " + entryName
            );
        }

        if (entry.isDirectory()) {
            throw new IOException(
                    "ZIP entry is a directory, not a file: " + entryName
            );
        }

        return entry;
    }

    /**
     * Rejects unsafe paths and aliases such as {@code a//b} or
     * {@code a/../b}. ZipBuilder never produces those aliases, and rejecting
     * them here prevents two archive entries from resolving to the same path.
     */
    private void validateArchive() throws IOException {
        Set<String> entriesSeen = new HashSet<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String original = entry.getName();
            String normalized = entry.isDirectory()
                    ? normalizeDirectoryPath(original)
                    : normalizeFilePath(original);

            if (!original.equals(normalized)) {
                throw new IOException(
                        "Non-canonical ZIP entry: " + original
                );
            }

            if (!entriesSeen.add(normalized)) {
                throw new IOException(
                        "Duplicate ZIP entry: " + normalized
                );
            }
        }
    }

    private static Path resolveExtractionTarget(
            Path targetRoot,
            String relativeName
    ) throws IOException {
        String normalizedRelative = normalizeFilePath(relativeName);
        Path output = targetRoot
                .resolve(normalizedRelative)
                .normalize();

        if (!output.startsWith(targetRoot)) {
            throw new IOException(
                    "ZIP entry escapes extraction directory: "
                            + relativeName
            );
        }

        return output;
    }

    /**
     * Creates a directory tree while refusing to traverse symbolic links.
     */
    private static void createSafeDirectories(
            Path directory
    ) throws IOException {
        Path absolute = directory.toAbsolutePath().normalize();
        Path current = absolute.getRoot();

        if (current == null) {
            throw new IOException(
                    "Directory has no filesystem root: " + directory
            );
        }

        for (Path segment : absolute) {
            current = current.resolve(segment);

            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current)) {
                    throw new IOException(
                            "Symbolic links are not allowed in extraction path: "
                                    + current
                    );
                }

                if (!Files.isDirectory(
                        current,
                        LinkOption.NOFOLLOW_LINKS
                )) {
                    throw new IOException(
                            "Extraction path component is not a directory: "
                                    + current
                    );
                }

                continue;
            }

            Files.createDirectory(current);
        }
    }

    private static String normalizeFilePath(String path) {
        Objects.requireNonNull(path, "path");

        String normalized = path
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+", "/");

        validateZipPath(normalized);

        return normalized;
    }

    private static String normalizeDirectoryPath(String path) {
        String normalized = normalizeFilePath(path);

        return normalized.endsWith("/")
                ? normalized
                : normalized + "/";
    }

    private static void validateZipPath(String path) {
        if (
                path.isBlank()
                        || path.equals(".")
                        || path.equals("..")
                        || path.startsWith("../")
                        || path.contains("/../")
        ) {
            throw new IllegalArgumentException(
                    "Unsafe ZIP path: " + path
            );
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;

        try {
            zip.close();
        } catch (IOException exception) {
            failure = exception;
        }

        if (temporaryArchive != null) {
            try {
                Files.deleteIfExists(temporaryArchive);
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}