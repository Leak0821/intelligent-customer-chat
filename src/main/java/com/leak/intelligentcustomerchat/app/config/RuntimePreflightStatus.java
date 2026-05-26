package com.leak.intelligentcustomerchat.app.config;

import java.util.List;

public record RuntimePreflightStatus(
        String mode,
        boolean ready,
        List<FeatureCheck> checks,
        List<String> errors,
        List<String> warnings
) {
    public record FeatureCheck(String feature, String status, String detail) {
        public static FeatureCheck ok(String feature, String detail) {
            return new FeatureCheck(feature, "OK", detail);
        }

        public static FeatureCheck warn(String feature, String detail) {
            return new FeatureCheck(feature, "WARN", detail);
        }

        public static FeatureCheck error(String feature, String detail) {
            return new FeatureCheck(feature, "ERROR", detail);
        }

        public static FeatureCheck skipped(String feature, String detail) {
            return new FeatureCheck(feature, "SKIPPED", detail);
        }
    }
}
