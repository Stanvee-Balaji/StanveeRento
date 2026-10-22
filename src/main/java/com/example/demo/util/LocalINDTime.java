package com.example.demo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Single place for "now" in Asia/Kolkata so every timestamp in the app is IST. */
public final class LocalINDTime {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private LocalINDTime() {}
    public static LocalDateTime now() { return LocalDateTime.now(IST); }
}
