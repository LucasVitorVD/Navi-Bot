package com.project.navi.quote;

import java.util.List;
import java.util.Map;

record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {

    record Content(String role, List<Part> parts) {
    }

    record Part(String text) {
    }

    record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {
    }
}
