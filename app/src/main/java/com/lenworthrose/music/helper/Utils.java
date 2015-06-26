package com.lenworthrose.music.helper;

/**
 * Created by Lenny on 2015-06-26.
 */
public class Utils {
    /**
     * Converts a long millisecond value to a displayable time format. Calls intToTimeDisplay() internally.
     *
     * @param value time in milliseconds
     * @return String representing the time in (hh:)mm:ss format
     */
    public static String longToTimeDisplay(long value) {
        return intToTimeDisplay((int)(value / 1000));
    }

    /**
     * Converts an integer second value to a displayable time format.
     *
     * @param value time in milliseconds
     * @return String representing the time in (hh:)mm:ss format
     */
    public static String intToTimeDisplay(int value) {
        StringBuilder sb = new StringBuilder();

        if (value < 0) {
            sb.append("-");
            value = Math.abs(value);
        }

        int numSeconds = value % 60;
        int numMinutes = value / 60;
        int numHours = numMinutes / 60;

        if (numHours > 0) {
            numMinutes %= 60;
            sb.append(numHours).append(":").append(String.format("%02d", numMinutes));
        } else {
            sb.append(String.format("%d", numMinutes));
        }

        sb.append(":").append(String.format("%02d", numSeconds));

        return sb.toString();
    }
}
