package chechelpo.frplm.utils.tokenizers;

import ai.djl.sentencepiece.SpTokenizer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

final class SentencePieceTokenizer
        implements Tokenizer, AutoCloseable {

    private final SpTokenizer delegate;

    SentencePieceTokenizer(String classpathLocation) {
        Objects.requireNonNull(
                classpathLocation,
                "classpathLocation must not be null"
        );

        ClassPathResource resource =
                new ClassPathResource(classpathLocation);

        try (InputStream input = resource.getInputStream()) {
            byte[] modelBytes = input.readAllBytes();
            this.delegate = new SpTokenizer(modelBytes);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load SentencePiece tokenizer from "
                            + classpathLocation,
                    exception
            );
        }
    }

    @Override
    public synchronized int tokenCount(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return delegate.tokenize(text).size();
    }

    @Override
    public void close() {
        delegate.close();
    }
}