package chechelpo.frplm.utils.IO;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileWriters {
    private FileWriters() {}

    public static void writeImgToPath(@NotNull Path path, MultipartFile file) throws IOException {
        Files.createDirectories(path.getParent());

        try (OutputStream out = Files.newOutputStream(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            BufferedImage img = FileReaders.readImage(file);
            if (!ImageIO.write(img, "png", out)) {
                throw new IOException("No ImageIO writer for PNG");
            }
        }
    }
}
