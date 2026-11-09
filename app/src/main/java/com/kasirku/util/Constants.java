package com.kasirku.util;

/**
 * Kumpulan nilai tetap yang dipakai di seluruh aplikasi.
 * Memakai konstanta mencegah salah ketik seperti "kasir" vs "KASIR".
 */
public final class Constants {

  private Constants() {
  }
  public static final String STORE_NAME = "Tokoku";
  public static final int MAX_CART_ITEMS = 50;
  public static final class Role {
    public static final String ADMIN = "ADMIN";
    public static final String KASIR = "KASIR";
  }

  public static final class SaleStatus {
    public static final String SELESAI = "SELESAI";
    public static final String BATAL = "BATAL";
  }

  public static final class MovementType {
    public static final String STOK_AWAL = "STOK_AWAL";
    public static final String PENJUALAN = "PENJUALAN";
    public static final String PENYESUAIAN = "PENYESUAIAN";
    public static final String PEMBATALAN = "PEMBATALAN";
  }

  public static final class DiscountType {
    public static final String PERSEN = "PERSEN";
    public static final String NOMINAL = "NOMINAL";
  }

  public static final class PromoScope {
    public static final String BARANG = "BARANG";
    public static final String KATEGORI = "KATEGORI";
    public static final String SEMUA = "SEMUA";
  }
}
