package com.kasirku.util;

import com.kasirku.model.Product;
import com.kasirku.model.Promotion;

import java.util.Date;
import java.util.List;

/**
 * Memilih promo yang berlaku untuk sebuah barang.
 * Aturan: satu barang hanya memakai SATU promo, yaitu yang potongannya
 * paling besar. Promo tidak ditumpuk.
 */
public final class PromoResolver {

  private PromoResolver() {
  }

  /** Mengembalikan promo terbaik, atau null jika tidak ada promo yang berlaku. */
  public static Promotion findBest(Product product, List<Promotion> promotions, Date now) {
    Promotion best = null;
    long bestDiscount = 0;
    for (Promotion promo : promotions) {
      if (!promo.isRunningAt(now) || !promo.appliesTo(product)) {
        continue;
      }
      long discount = promo.discountPerUnit(product.sellPrice);
      if (discount > bestDiscount) {
        best = promo;
        bestDiscount = discount;
      }
    }
    return best;
  }
}
