package dev.zerek.featherjoindate.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class TimeFormatterUtility {
    private static final ZoneId ZONE_ID = ZoneId.of("America/Toronto");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, uuuu").withZone(ZONE_ID);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZONE_ID);

    /**
     * Formats a timestamp into a date string.
     * @param epochMillis The timestamp in milliseconds since epoch
     * @return Formatted date string (e.g., "Jan 01, 2023") or "Unknown" if timestamp is invalid
     */
    public static String formatDate(long epochMillis) {
        return isValidTimestamp(epochMillis) ? DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis)) : "Unknown";
    }

    /**
     * Formats a timestamp into a time string.
     * @param epochMillis The timestamp in milliseconds since epoch
     * @return Formatted time string (e.g., "03:30 PM") or "Unknown" if timestamp is invalid
     */
    public static String formatTime(long epochMillis) {
        return isValidTimestamp(epochMillis) ? TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis)) : "Unknown";
    }

    /**
     * Formats a timestamp into a relative time string with parentheses (e.g., "(Today)" or "(5d ago)") or empty string if timestamp is invalid
     * @param epochMillis The timestamp in milliseconds since epoch
     * @return Formatted relative time string with parentheses (e.g., "(Today)" or "(5d ago)") or empty string if timestamp is invalid
     */
    public static String formatRelativeTime(long epochMillis) {
        if (!isValidTimestamp(epochMillis)) return "";
        
        Instant instant = Instant.ofEpochMilli(epochMillis);
        Instant now = Instant.now();
        long days = ChronoUnit.DAYS.between(instant, now);
        
        if (days == 0) {
            return "(Today)";
        } else if (days == 1) {
            return "(1d ago)";
        } else {
            return String.format("(%dd ago)", days);
        }
    }

    /**
     * Calculates and formats the time elapsed since a given login timestamp.
     * @param lastLoginMillis The login timestamp in milliseconds since epoch
     * @return Formatted duration string or "Unknown" if timestamp is invalid
     */
    public static String formatOnlineTime(long lastLoginMillis) {
        if (!isValidTimestamp(lastLoginMillis)) return "Unknown";
        Duration duration = Duration.between(Instant.ofEpochMilli(lastLoginMillis), Instant.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%d hours, %d minutes", hours, minutes);
    }

    /**
     * Checks if any of the provided timestamps are invalid (less than or equal to 0).
     * @param timestamps Variable number of timestamps to check
     * @return true if any timestamp is invalid, false if all are valid
     */
    public static boolean hasUnknownValues(long... timestamps) {
        return Arrays.stream(timestamps).anyMatch(timestamp -> !isValidTimestamp(timestamp));
    }
    
    /**
     * Helper method to check if a timestamp is valid (greater than zero)
     */
    private static boolean isValidTimestamp(long timestamp) {
        return timestamp > 0;
    }
}
