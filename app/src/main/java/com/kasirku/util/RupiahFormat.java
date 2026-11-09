package com.kasirku.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Mengubah angka menjadi tulisan rupiah ("Rp 25.000") dan sebaliknya.
 * Uang selalu disimpan sebagai bilangan bulat (long), tanpa desimal.
 */
public final class RupiahFormat {

  private RupiahFormat() {
  }

  public static String format(long amount) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator('.');
    symbols.setDecimalSeparator(',');
    DecimalFormat df = new DecimalFormat("#,##0", symbols);
    String number = df.format(Math.abs(amount));
    return (amount < 0 ? "-Rp " : "Rp ") + number;
  }

  /** Mengambil angka dari teks, misalnya "Rp 25.000" menjadi 25000. */
  public static long parse(String text) {
    if (text == null) {
      return 0;
    }
    String digits = text.replaceAll("[^0-9]", "");
    if (digits.isEmpty()) {
      return 0;
    }
    try {
      return Long.parseLong(digits);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
