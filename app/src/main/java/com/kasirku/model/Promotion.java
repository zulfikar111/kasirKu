package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.kasirku.util.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Promo acara (misalnya acara tahunan). Diterapkan otomatis saat promo aktif. */
public class Promotion {
  @DocumentId
  public String id;
  public String name;
  public String discountType;    // Constants.DiscountType.PERSEN atau NOMINAL
  public long discountValue;     // persen (1-100) atau rupiah per unit
  public Date startDate;         // awal hari mulai
  public Date endDate;           // akhir hari selesai (23:59:59.999)
  public String scope;           // Constants.PromoScope.*
  public List<String> productIds = new ArrayList<>();
  public List<String> categoryIds = new ArrayList<>();
  public boolean isActive = true;
  public String createdBy;
  @ServerTimestamp
  public Date createdAt;

  public Promotion() {
  }

  /** Apakah promo sedang berjalan pada waktu tertentu. */
  @Exclude
  public boolean isRunningAt(Date now) {
    return isActive
      && startDate != null
      && endDate != null
      && !now.before(startDate)
      && !now.after(endDate);
  }

  /** Apakah barang ini ikut promo. */
  @Exclude
  public boolean appliesTo(Product product) {
    if (Constants.PromoScope.SEMUA.equals(scope)) {
      return true;
    }
    if (Constants.PromoScope.BARANG.equals(scope)) {
      return productIds != null && productIds.contains(product.id);
    }
    if (Constants.PromoScope.KATEGORI.equals(scope)) {
      return categoryIds != null && categoryIds.contains(product.categoryId);
    }
    return false;
  }

  /**
   * Potongan per unit untuk harga tertentu.
   * Persen dibulatkan ke bawah. Potongan tidak pernah melebihi harga,
   * jadi harga akhir tidak bisa negatif.
   */
  @Exclude
  public long discountPerUnit(long price) {
    long discount;
    if (Constants.DiscountType.PERSEN.equals(discountType)) {
      discount = (price * discountValue) / 100;
    } else {
      discount = discountValue;
    }
    if (discount < 0) {
      discount = 0;
    }
    return Math.min(discount, price);
  }
}
