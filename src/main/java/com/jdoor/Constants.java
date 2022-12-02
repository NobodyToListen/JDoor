package com.jdoor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constants {
    public static final int IMAGE_BYTES_DIMENSION = 1024 * 62;
    public static final int RESPONSE_TIMEOUT = 60000;
    public static final int TCP_PORT = 8080;
    public static final int UDP_SCREEN_PORT = 8081;
    public static final int UDP_WEBCAM_PORT = 8082;
    public static final int[] MINIMUM_WEBCAM_RESOLUTION = {176,144};
    public static enum FileOperations {
        Send,
        Get
    }

    public static boolean isValidIP(String ip) {
        if (ip == null) {
            return false;
        }else if (ip.equals("localhost")) {
            return true;
        } else {
            String zeroTo255
                    = "(\\d{1,2}|(0|1)\\"
                    + "d{2}|2[0-4]\\d|25[0-5])";


            String regex
                    = zeroTo255 + "\\."
                    + zeroTo255 + "\\."
                    + zeroTo255 + "\\."
                    + zeroTo255;

            Pattern p = Pattern.compile(regex);

            Matcher m = p.matcher(ip);
            return m.matches();
        }
    }
}
