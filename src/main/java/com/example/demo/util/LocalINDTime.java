package com.example.demo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/** Single place for "now" in Asia/Kolkata so every timestamp in the app is IST. */
public final class LocalINDTime {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private LocalINDTime() {}
    public static LocalDateTime now() { return LocalDateTime.now(IST); }

    /** Current IST time as epoch millis (used for JWT iat/exp). */
    public static long nowMillis() { return now().atZone(IST).toInstant().toEpochMilli(); }

    /** Current IST time as java.util.Date (used by jjwt builder). */
    public static Date nowDate() { return new Date(nowMillis()); }
}
