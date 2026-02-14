package com.jd.oxygent.oxybank.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash utility class
 * Convert string to MD5 hash value
 * Converted from oxybank/utils/hash_util.py
 */
@Slf4j
public class HashUtil {
    
    /**
     * Convert string to MD5 hash value
     * @param inputStr Input string
     * @return MD5 hash string
     */
    public static String strToMd5(String inputStr) {
        try {
            // Create MD5 hash object
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // Update hash object with bytes
            md.update(inputStr.getBytes());
            
            // Get hash bytes
            byte[] hashBytes = md.digest();
            
            // Convert to hexadecimal representation
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found", e);
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}