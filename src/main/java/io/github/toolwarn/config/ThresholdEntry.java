package io.github.toolwarn.config;

public record ThresholdEntry(double percentage, String message) {

    public ThresholdEntry {
        if (percentage <= 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("percentage must be in (0.0, 1.0]: " + percentage);
        }
    }
}
