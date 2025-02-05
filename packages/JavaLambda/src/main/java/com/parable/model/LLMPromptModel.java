package com.parable.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LLMPromptModel {
    private final String prompt;
    private final String modelId;
}
