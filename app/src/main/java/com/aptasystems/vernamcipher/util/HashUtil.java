package com.aptasystems.vernamcipher.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    public static String hashPassword(String password) {
        return sha256(password);
    }

    public static String hashPassword(byte[] password) {
        return sha256(password);
    }

    public static String sha256(String input) {
        try {
            byte[] bytes = input.getBytes("UTF-8");
            return sha256(bytes);
        } catch (UnsupportedEncodingException e) {
            // Swallow the exception.
            return null;
        }
    }

    private static String sha256(byte[] input) {
        try {
            MessageDigest digest = null;
            digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            byte[] byteArray = digest.digest(input);
            return String.format("%0" + (byteArray.length * 2) + 'x', new BigInteger(1, byteArray));
        } catch (NoSuchAlgorithmException e) {
            // Swallow the exception.
            return null;
        }
    }
}
