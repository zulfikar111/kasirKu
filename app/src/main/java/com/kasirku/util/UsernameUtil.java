package com.kasirku.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pembantu username. Firebase Authentication butuh email, jadi username
 * diubah menjadi email internal (contoh: "admin" menjadi "admin@kasirku.app").
 * Email ini tidak perlu benar-benar ada.
 */
public final class UsernameUtil {

  public static final String EMAIL_DOMAIN = "@kasirku.app";

  // huruf kecil, angka, titik, garis bawah; 3-30 karakter
  private static final Pattern VALID = Pattern.compile("^[a-z0-9._]{3,30}$");

  private UsernameUtil() {
  }

  public static String normalize(String input) {
    return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
  }

  public static boolean isValid(String username) {
    return VALID.matcher(normalize(username)).matches();
  }

  public static String toEmail(String username) {
    return normalize(username) + EMAIL_DOMAIN;
  }
}
