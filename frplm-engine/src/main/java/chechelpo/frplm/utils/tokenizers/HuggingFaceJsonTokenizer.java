package chechelpo.frplm.utils.tokenizers;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

final class HuggingFaceJsonTokenizer
        implements Tokenizer, AutoCloseable {

    private final HuggingFaceTokenizer delegate;
    private final Path temporaryDirectory;
    private final Path temporaryTokenizerFile;

    HuggingFaceJsonTokenizer(String classpathLocation) {
        Objects.requireNonNull(
                classpathLocation,
                "classpathLocation must not be null"
        );

        try {
            ClassPathResource resource =
                    new ClassPathResource(classpathLocation);

            String filename = resource.getFilename();
            if (filename == null || filename.isBlank()) {
                filename = "glm.json";
            }

            this.temporaryDirectory =
                    Files.createTempDirectory("frplm-tokenizer-");

            this.temporaryTokenizerFile =
                    temporaryDirectory.resolve(filename);

            try (InputStream input = resource.getInputStream()) {
                Files.copy(
                        input,
                        temporaryTokenizerFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            this.delegate = HuggingFaceTokenizer.builder()
                    .optTokenizerPath(temporaryTokenizerFile)
                    /*
                     * Count only the supplied text. Do not automatically
                     * inject BOS, EOS, CLS, SEP, or similar tokens.
                     */
                    .optAddSpecialTokens(false)
                    .build();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load Hugging Face tokenizer from "
                            + classpathLocation,
                    exception
            );
        }
    }

    @Override
    public synchronized int tokenCount(String text) {
        Objects.requireNonNull(text, "text must not be null");

        return delegate.encode(text)
                .getIds()
                .length;
    }

    @Override
    public void close() {
        delegate.close();

        try {
            Files.deleteIfExists(temporaryTokenizerFile);
            Files.deleteIfExists(temporaryDirectory);
        } catch (IOException ignored) {
            // Temporary files are also eligible for OS cleanup.
        }
    }
}