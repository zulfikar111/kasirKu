package com.kasirku.util;

import com.kasirku.model.ProductInput;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pemeriksaan input yang dipakai form dan lapisan data. */
public final class Validator {

  public static final long MAX_PRICE = 1_000_000_000L;
  public static final int MAX_STOCK = 1_000_000;

  // Dimulai huruf/angka, boleh berisi huruf besar, angka, titik, minus, garis bawah
  private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,29}$");

  private Validator() {
  }

  public static String normalizeCode(String code) {
    return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
  }

  public static boolean isValidCode(String code) {
    return CODE.matcher(normalizeCode(code)).matches();
  }

  /** Mengembalikan pesan kesalahan, atau null jika data valid. */
  public static String validateProduct(ProductInput in, boolean isNew) {
    if (!isValidCode(in.code)) {
      return "Kode barang 1-30 karakter: huruf, angka, titik, minus, atau garis bawah.";
    }
    if (in.name == null || in.name.trim().isEmpty()) {
      return "Nama barang wajib diisi.";
    }
    if (in.name.trim().length() > 80) {
      return "Nama barang maksimal 80 karakter.";
    }
    if (in.categoryId == null || in.categoryId.isEmpty()) {
      return "Pilih kategori barang.";
    }
    if (in.buyPrice < 0 || in.buyPrice > MAX_PRICE) {
      return "Harga beli tidak valid atau terlalu besar.";
    }
    if (in.sellPrice <= 0 || in.sellPrice > MAX_PRICE) {
      return "Harga jual harus lebih dari 0 dan tidak terlalu besar.";
    }
    if (isNew && (in.stock < 0 || in.stock > MAX_STOCK)) {
      return "Stok awal tidak valid.";
    }
    if (in.minStock < 0 || in.minStock > MAX_STOCK) {
      return "Stok minimum tidak valid.";
    }
    return null;
  }
  /** Pemeriksaan isian penyesuaian stok (sebelum tahu stok terbaru). Null jika valid. */
  public static String validateAdjustmentInput(int delta, String reason) {
    if (delta == 0) {
      return "Jumlah penyesuaian tidak boleh 0.";
    }
    if (Math.abs((long) delta) > MAX_STOCK) {
      return "Jumlah terlalu besar.";
    }
    String clean = reason == null ? "" : reason.trim();
    if (clean.length() < 3) {
      return "Alasan wajib diisi (minimal 3 karakter).";
    }
    if (clean.length() > 200) {
      return "Alasan maksimal 200 karakter.";
    }
    return null;
  }

  /** Pemeriksaan hasil akhir stok. Null jika valid. */
  public static String validateAdjustmentResult(int currentStock, int delta) {
    long after = (long) currentStock + delta;
    if (after < 0) {
      return "Stok tidak cukup. Stok sekarang: " + currentStock + " pcs.";
    }
    if (after > MAX_STOCK) {
      return "Stok melebihi batas yang diizinkan.";
    }
    return null;
  }
}
