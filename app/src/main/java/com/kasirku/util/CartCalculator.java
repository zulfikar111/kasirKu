package com.kasirku.util;

import com.kasirku.model.CartLine;
import com.kasirku.model.Product;
import com.kasirku.model.Promotion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Semua rumus hitung keranjang di satu tempat, supaya mudah diuji. */
public final class CartCalculator {

  private CartCalculator() {
  }

  /** Membuat satu baris keranjang. promo boleh null (tanpa promo). */
  public static CartLine buildLine(Product p, int quantity, Promotion promo) {
    long discount = promo == null ? 0 : promo.discountPerUnit(p.sellPrice);
    boolean hasPromo = promo != null && discount > 0;
    return new CartLine(p.id, p.code, p.name, quantity, p.sellPrice, discount, p.buyPrice,
      hasPromo ? promo.id : null, hasPromo ? promo.name : null, p.stock);
  }

  /**
   * Membuat semua baris keranjang dari isi keranjang (id barang dan jumlah).
   * Barang yang tidak ada atau nonaktif dilewati.
   */
  public static List<CartLine> buildLines(Map<String, Integer> cart,
                                          Map<String, Product> products,
                                          List<Promotion> promotions, Date now) {
    List<CartLine> lines = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : cart.entrySet()) {
      Product p = products.get(entry.getKey());
      int qty = entry.getValue();
      if (p == null || !p.isActive || qty <= 0) {
        continue;
      }
      Promotion best = PromoResolver.findBest(p, promotions, now);
      lines.add(buildLine(p, qty, best));
    }
    return lines;
  }

  public static long grossTotal(List<CartLine> lines) {
    long sum = 0;
    for (CartLine l : lines) {
      sum += l.grossSubtotal();
    }
    return sum;
  }

  public static long discountTotal(List<CartLine> lines) {
    long sum = 0;
    for (CartLine l : lines) {
      sum += l.discountTotal();
    }
    return sum;
  }

  /** Total belanja = jumlah subtotal setelah diskon. */
  public static long total(List<CartLine> lines) {
    long sum = 0;
    for (CartLine l : lines) {
      sum += l.subtotal();
    }
    return sum;
  }

  public static long costTotal(List<CartLine> lines) {
    long sum = 0;
    for (CartLine l : lines) {
      sum += l.costTotal();
    }
    return sum;
  }

  public static int itemCount(List<CartLine> lines) {
    int sum = 0;
    for (CartLine l : lines) {
      sum += l.quantity;
    }
    return sum;
  }

  /** Pesan jika ada barang yang jumlahnya melebihi stok, atau null jika aman. */
  public static String findStockProblem(List<CartLine> lines) {
    for (CartLine l : lines) {
      if (l.quantity > l.availableStock) {
        return "Stok \"" + l.productName + "\" tidak cukup. Tersedia "
          + l.availableStock + " pcs.";
      }
    }
    return null;
  }

  /** Pembayaran tunai tidak boleh kurang dari total. Null jika valid. */
  public static String validatePayment(long total, long paid) {
    if (paid < total) {
      return "Uang yang diterima kurang " + RupiahFormat.format(total - paid) + ".";
    }
    return null;
  }

  public static long change(long total, long paid) {
    return paid - total;
  }
}
