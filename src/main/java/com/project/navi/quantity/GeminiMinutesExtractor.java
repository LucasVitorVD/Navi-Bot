package com.project.navi.quantity;

import java.util.Optional;

public interface GeminiMinutesExtractor {

    Optional<Integer> extractMinutes(String captionText);
}
