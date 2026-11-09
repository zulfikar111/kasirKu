package com.kasirku.session;

import com.kasirku.model.Product;
import com.kasirku.util.Constants;

import java.util.LinkedHashMap;
import java.util.Map;

/** Keranjang belanja di memori: hanya menyimpan id barang dan jumlahnya. */
public final class CartManager {

  private static final Map<String, Integer> items = new LinkedHashMap<>();

  private CartManager() {
  }

  /** Salinan isi keranjang (aman diubah-ubah). */
  public static Map<String, Integer> snapshot() {
    return new LinkedHashMap<>(items);
  }

  public static int quantityOf(String productId) {
    Integer qty = items.get(productId);
    return qty == null ? 0 : qty;
  }

  public static boolean isEmpty() {
    return items.isEmpty();
  }

  /** Menambah satu. Mengembalikan pesan penolakan, atau null jika berhasil. */
  public static String add(Product p) {
    if (p == null || p.id == null) {
      return "Barang tidak ditemukan.";
    }
    if (!p.isActive) {
      return "Barang ini tidak aktif.";
    }
    if (p.stock <= 0) {
      return "Stok habis.";
    }
    int current = quantityOf(p.id);
    if (current + 1 > p.stock) {
      return "Stok tidak cukup. Tersedia " + p.stock + " pcs.";
    }
    if (current == 0 && items.size() >= Constants.MAX_CART_ITEMS) {
      return "Keranjang penuh (maksimal " + Constants.MAX_CART_ITEMS + " jenis barang).";
    }
    items.put(p.id, current + 1);
    return null;
  }

  /** Mengubah jumlah barang yang sudah ada. Jumlah 0 atau kurang menghapus barang. */
  public static void setQuantity(String productId, int quantity) {
    if (quantity <= 0) {
      items.remove(productId);
    } else if (items.containsKey(productId)) {
      items.put(productId, quantity);
    }
  }

  public static void remove(String productId) {
    items.remove(productId);
  }

  public static void clear() {
    items.clear();
  }
}
