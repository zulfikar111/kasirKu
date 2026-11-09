package com.kasirku.model;

/** Data isian form barang (belum tentu sudah tersimpan). */
public class ProductInput {
  public String code;
  public String name;
  public String categoryId;
  public long buyPrice;
  public long sellPrice;
  public int stock;      // hanya dipakai saat tambah barang
  public int minStock;
}
