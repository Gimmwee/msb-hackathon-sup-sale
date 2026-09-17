package com.msb.supsale.util;

import java.util.regex.Pattern;

public class PhoneValidator {
    private static final Pattern PHONE_RE = Pattern.compile("^(0|\\+84)[3-9][0-9]{8}$");

    public static boolean isValid(String phone) {
        return phone != null && PHONE_RE.matcher(phone).matches();
    }

    public static String normalize(String phone) {
        if (phone == null) return null;
        phone = phone.trim().replaceAll("[\\s\\-\\.]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }
}
