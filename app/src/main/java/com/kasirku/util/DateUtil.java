package com.kasirku.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Pembantu tanggal dan waktu. Semua tampilan memakai zona waktu HP.
 */
public final class DateUtil {

  private static final Locale ID = Locale.forLanguageTag("id-ID");

  private DateUtil() {
  }

  public static String formatDateTime(Date date) {
    return new SimpleDateFormat("d MMM yyyy, HH:mm", ID).format(date);
  }

  public static String formatDate(Date date) {
    return new SimpleDateFormat("d MMM yyyy", ID).format(date);
  }

  /** Kunci tanggal untuk nomor transaksi, contoh: 20261015. */
  public static String invoiceDateKey(Date date) {
    return new SimpleDateFormat("yyyyMMdd", ID).format(date);
  }

  /** Awal hari (pukul 00:00:00.000) pada zona waktu HP. */
  public static Date startOfDay(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  /** Akhir hari (pukul 23:59:59.999) pada zona waktu HP. */
  public static Date endOfDay(Date date) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
    return c.getTime();
  }

  public static Date addDays(Date date, int days) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DAY_OF_MONTH, days);
    return c.getTime();
  }
}
