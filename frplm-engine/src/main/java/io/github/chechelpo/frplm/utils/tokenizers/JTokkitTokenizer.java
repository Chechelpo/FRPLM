package io.github.chechelpo.frplm.utils.tokenizers;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import java.util.Objects;

final class JTokkitTokenizer implements Tokenizer {

    private static final EncodingRegistry REGISTRY =
            Encodings.newDefaultEncodingRegistry();

    private final Encoding encoding;

    public JTokkitTokenizer(EncodingType encodingType) {
        Objects.requireNonNull(
                encodingType,
                "encodingType must not be null"
        );

        this.encoding = REGISTRY.getEncoding(encodingType);
    }

    @Override
    public int tokenCount(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return encoding.encode(text).size();
    }
}