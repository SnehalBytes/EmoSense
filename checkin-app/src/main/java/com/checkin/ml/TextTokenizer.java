package com.checkin.ml;

import java.util.List;

/**
 * Tokenizer interface for transformer-based text emotion recognition models.
 * Responsible for turning raw strings into input_ids, attention_mask, and token_type_ids.
 */
public interface TextTokenizer {

    record TokenizedInput(
            long[] inputIds,
            long[] attentionMask,
            long[] tokenTypeIds,
            List<String> tokens
    ) {
    }

    TokenizedInput tokenize(String text, int maxSequenceLength);

    boolean isAvailable();

    String getTokenizerSource();
}
