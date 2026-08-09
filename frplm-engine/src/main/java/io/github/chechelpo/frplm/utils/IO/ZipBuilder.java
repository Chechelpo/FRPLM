package io.github.chechelpo.frplm.utils.IO;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipBuilder implements AutoCloseable {

    private final ZipOutputStream zip;
    private final ObjectMapper objectMapper;
    private final Set<String> entries = new HashSet<>();

    public ZipBuilder(
            OutputStream outputStream,
            ObjectMapper objectMapper
    ) {
        this.zip = new ZipOutputStream(outputStream);
        this.objectMapper = objectMapper;
    }

    public ZipBuilder addJson(
            String zipPath,
            Object value
    ) throws IOException {
        String entryName = normalizeFilePath(zipPath);

        addEntry(entryName);

        zip.putNextEntry(new ZipEntry(entryName));

        try {
            byte[] json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(value);

            zip.write(json);
        } finally {
            zip.closeEntry();
        }

        return this;
    }

    public ZipBuilder addFile(
            Path sourceFile,
            String zipPath
    ) throws IOException {
        Path source = sourceFile.toAbsolutePath().normalize();

        if (!Files.isRegularFile(
                source,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IllegalArgumentException(
                    "Source is not a regular file: " + source
            );
        }

        if (Files.isSymbolicLink(source)) {
            throw new IllegalArgumentException(
                    "Symbolic links are not allowed: " + source
            );
        }

        String entryName = normalizeFilePath(zipPath);

        addEntry(entryName);

        zip.putNextEntry(new ZipEntry(entryName));

        try {
            Files.copy(source, zip);
        } finally {
            zip.closeEntry();
        }

        return this;
    }

    public ZipBuilder addDirectory(
            Path sourceDirectory,
            String zipDirectory
    ) throws IOException {
        Path sourceRoot = sourceDirectory
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(
                sourceRoot,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IllegalArgumentException(
                    "Source is not a directory: " + sourceRoot
            );
        }

        String archiveRoot =
                normalizeDirectoryPath(zipDirectory);

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException(
                            "Symbolic links are not allowed: " + path
                    );
                }

                Path relative = sourceRoot.relativize(path);

                String entryName = relative.toString().isEmpty()
                        ? archiveRoot
                        : archiveRoot + normalizeRelativePath(relative);

                if (Files.isDirectory(
                        path,
                        LinkOption.NOFOLLOW_LINKS
                )) {
                    addDirectoryEntry(entryName);
                } else if (Files.isRegularFile(
                        path,
                        LinkOption.NOFOLLOW_LINKS
                )) {
                    addFile(path, entryName);
                }
            }
        }

        return this;
    }

    private void addDirectoryEntry(
            String entryName
    ) throws IOException {
        String normalized =
                normalizeDirectoryPath(entryName);

        addEntry(normalized);

        zip.putNextEntry(new ZipEntry(normalized));
        zip.closeEntry();
    }

    private void addEntry(String entryName) {
        if (!entries.add(entryName)) {
            throw new IllegalArgumentException(
                    "Duplicate ZIP entry: " + entryName
            );
        }
    }

    private static String normalizeRelativePath(Path path) {
        String value = path
                .toString()
                .replace('\\', '/');

        return normalizeFilePath(value);
    }

    private static String normalizeFilePath(String path) {
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
        zip.close();
    }
}