package com.kasirku.model;

/** Satu baris di keranjang. Hasil hitung, tidak disimpan langsung ke Firestore. */
public class CartLine {
  public final String productId;
  public final String productCode;
  public final String productName;
  public final int quantity;
  public final long originalPrice;    // harga jual normal per unit
  public final long discountPerUnit;  // potongan per unit
  public final long unitPrice;        // harga setelah diskon per unit
  public final long buyPrice;         // modal per unit
  public final String promotionId;    // null jika tanpa promo
  public final String promotionName;
  public final int availableStock;

  public CartLine(String productId, String productCode, String productName, int quantity,
                  long originalPrice, long discountPerUnit, long buyPrice,
                  String promotionId, String promotionName, int availableStock) {
    this.productId = productId;
    this.productCode = productCode;
    this.productName = productName;
    this.quantity = quantity;
    this.originalPrice = originalPrice;
    this.discountPerUnit = discountPerUnit;
    this.unitPrice = originalPrice - discountPerUnit;
    this.buyPrice = buyPrice;
    this.promotionId = promotionId;
    this.promotionName = promotionName;
    this.availableStock = availableStock;
  }

  public long subtotal() {
    return unitPrice * quantity;
  }

  public long grossSubtotal() {
    return originalPrice * quantity;
  }

  public long discountTotal() {
    return discountPerUnit * quantity;
  }

  public long costTotal() {
    return buyPrice * quantity;
  }
}
