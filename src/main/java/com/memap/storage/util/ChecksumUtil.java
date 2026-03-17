package com.memap.storage.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for computing file checksums.
 */
@UtilityClass
@Slf4j
public class ChecksumUtil {

  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  /**
   * Compute MD5 checksum of an input stream.
   *
   * @param inputStream the input stream to read
   * @return hex-encoded MD5 checksum (32 characters)
   * @throws IOException if reading fails
   */
  public static String computeMd5(InputStream inputStream) throws IOException {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] buffer = new byte[8192];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        md.update(buffer, 0, bytesRead);
      }

      byte[] digest = md.digest();
      return bytesToHex(digest);

    } catch (NoSuchAlgorithmException e) {
      // MD5 is always available in Java
      log.error("MD5 algorithm not available", e);
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Convert byte array to hex string.
   *
   * @param bytes the byte array
   * @return hex-encoded string
   */
  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      hexChars[i * 2] = HEX_CHARS[v >>> 4];
      hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
    }
    return new String(hexChars);
  }
}
