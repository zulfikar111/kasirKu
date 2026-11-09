package com.kasirku.model;

import com.google.firebase.firestore.Exclude;

/**
 * Satu baris barang dalam transaksi. Semua nilai adalah SNAPSHOT saat
 * transaksi terjadi, jadi tidak berubah walau harga barang diperbarui.
 */
public class SaleItem {
  public String productId;
  public String productCode;
  public String productName;
  public int quantity;
  public long originalPrice;     // harga jual normal per unit
  public long discountPerUnit;   // potongan per unit (0 jika tidak ada promo)
  public long sellPrice;         // harga setelah diskon per unit
  public long buyPrice;          // modal per unit saat transaksi
  public long subtotal;          // sellPrice x quantity
  public String promotionId;     // null jika tanpa promo
  public String promotionName;

  public SaleItem() {
  }

  @Exclude
  public long getDiscountTotal() {
    return discountPerUnit * quantity;
  }

  @Exclude
  public long getCostTotal() {
    return buyPrice * quantity;
  }
}
