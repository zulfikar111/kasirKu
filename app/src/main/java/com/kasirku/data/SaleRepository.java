package com.kasirku.data;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.kasirku.model.CartLine;
import com.kasirku.model.Product;
import com.kasirku.model.Promotion;
import com.kasirku.model.Sale;
import com.kasirku.session.SessionManager;
import com.kasirku.util.CartCalculator;
import com.kasirku.util.Constants;
import com.kasirku.util.DateUtil;
import com.kasirku.util.PromoResolver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Menyimpan dan membaca transaksi penjualan. */
public class SaleRepository {

  private final PromotionRepository promotionRepository = new PromotionRepository();

  /** ID baru untuk satu percobaan transaksi (dipakai agar tidak tersimpan dua kali). */
  public String newSaleId() {
    return FirestoreRefs.collection(FirestoreRefs.SALES).document().getId();
  }

  public void getById(String id, DataCallback<Sale> callback) {
    FirestoreRefs.collection(FirestoreRefs.SALES).document(id).get()
      .addOnSuccessListener(doc -> {
        Sale sale = doc.exists() ? doc.toObject(Sale.class) : null;
        if (sale == null) {
          callback.onError("Transaksi tidak ditemukan.");
        } else {
          callback.onSuccess(sale);
        }
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  /**
   * Menyimpan transaksi. Hasil callback: ID transaksi.
   *
   * @param cart          isi keranjang (id barang dan jumlah)
   * @param expectedTotal total yang dilihat kasir di layar; jika harga di database
   *                      berubah sehingga totalnya berbeda, transaksi ditolak
   */
  public void createSale(String saleId, Map<String, Integer> cart, long paidAmount,
                         long expectedTotal, DataCallback<String> callback) {
    String uid = SessionManager.getUid();
    if (!SessionManager.isLoggedIn() || uid == null) {
      callback.onError("Sesi berakhir. Silakan masuk lagi.");
      return;
    }
    if (cart == null || cart.isEmpty()) {
      callback.onError("Keranjang kosong.");
      return;
    }
    if (cart.size() > Constants.MAX_CART_ITEMS) {
      callback.onError("Keranjang terlalu banyak jenis barang.");
      return;
    }
    for (Integer qty : cart.values()) {
      if (qty == null || qty <= 0) {
        callback.onError("Jumlah barang tidak valid.");
        return;
      }
    }
    String payProblem = CartCalculator.validatePayment(expectedTotal, paidAmount);
    if (payProblem != null) {
      callback.onError(payProblem);
      return;
    }

    // Promo dibaca dulu (query tidak boleh di dalam transaction), lalu transaction dijalankan
    promotionRepository.loadAll(new DataCallback<List<Promotion>>() {
      @Override
      public void onSuccess(List<Promotion> promos) {
        runSaleTransaction(saleId, cart, paidAmount, expectedTotal, promos, uid, callback);
      }

      @Override
      public void onError(String message) {
        callback.onError(message);
      }
    });
  }

  private void runSaleTransaction(String saleId, Map<String, Integer> cart, long paidAmount,
                                  long expectedTotal, List<Promotion> promos, String uid,
                                  DataCallback<String> callback) {
    final String cashierName = SessionManager.displayName();
    final String[] businessError = new String[1];
    final String dateKey = DateUtil.invoiceDateKey(new Date());

    FirebaseFirestore db = FirestoreRefs.db();
    DocumentReference saleRef = db.collection(FirestoreRefs.SALES).document(saleId);
    DocumentReference counterRef = db.collection(FirestoreRefs.COUNTERS).document(dateKey);

    final List<String> ids = new ArrayList<>(cart.keySet());
    final List<DocumentReference> productRefs = new ArrayList<>();
    for (String id : ids) {
      productRefs.add(db.collection(FirestoreRefs.PRODUCTS).document(id));
    }

    db.runTransaction(tx -> {
        // ===== 1. SEMUA PEMBACAAN DULU =====
        if (tx.get(saleRef).exists()) {
          return saleId; // sudah tersimpan oleh percobaan sebelumnya, jangan dobel
        }
        List<DocumentSnapshot> snaps = new ArrayList<>();
        for (DocumentReference ref : productRefs) {
          snaps.add(tx.get(ref));
        }
        DocumentSnapshot counterSnap = tx.get(counterRef);

        // ===== 2. PERIKSA DAN HITUNG DARI DATA TERBARU =====
        Date now = new Date();
        List<CartLine> lines = new ArrayList<>();
        List<Integer> stocksBefore = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
          DocumentSnapshot snap = snaps.get(i);
          Product p = snap.exists() ? snap.toObject(Product.class) : null;
          if (p == null) {
            throw reject(businessError,
              "Ada barang yang sudah tidak ditemukan. Periksa keranjang.");
          }
          if (!p.isActive) {
            throw reject(businessError,
              "Barang \"" + p.name + "\" sudah dinonaktifkan. Hapus dari keranjang.");
          }
          int qty = cart.get(ids.get(i));
          if (p.stock < qty) {
            throw reject(businessError, "Stok \"" + p.name
              + "\" tidak cukup. Tersedia " + p.stock + " pcs.");
          }
          Promotion best = PromoResolver.findBest(p, promos, now);
          lines.add(CartCalculator.buildLine(p, qty, best));
          stocksBefore.add(p.stock);
        }

        long gross = CartCalculator.grossTotal(lines);
        long discount = CartCalculator.discountTotal(lines);
        long total = CartCalculator.total(lines);
        long cost = CartCalculator.costTotal(lines);

        if (total != expectedTotal) {
          throw reject(businessError,
            "Harga atau promo barang berubah. Periksa total di layar lalu coba lagi.");
        }
        String payProblem = CartCalculator.validatePayment(total, paidAmount);
        if (payProblem != null) {
          throw reject(businessError, payProblem);
        }

        // ===== 3. NOMOR TRANSAKSI =====
        Long lastValue = counterSnap.exists() ? counterSnap.getLong("last") : null;
        long next = (lastValue == null ? 0 : lastValue) + 1;
        String invoiceNo = "INV-" + dateKey + "-" + String.format(Locale.ROOT, "%04d", next);

        // ===== 4. SEMUA PENULISAN =====
        if (counterSnap.exists()) {
          tx.update(counterRef, "last", next);
        } else {
          Map<String, Object> counter = new HashMap<>();
          counter.put("last", 1L);
          tx.set(counterRef, counter);
        }

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
          CartLine line = lines.get(i);
          int before = stocksBefore.get(i);
          int after = before - line.quantity;

          itemMaps.add(itemMap(line));

          tx.update(productRefs.get(i),
            "stock", after,
            "updatedAt", FieldValue.serverTimestamp());

          Map<String, Object> move = new HashMap<>();
          move.put("productId", line.productId);
          move.put("productName", line.productName);
          move.put("changeQty", -line.quantity);
          move.put("stockBefore", before);
          move.put("stockAfter", after);
          move.put("type", Constants.MovementType.PENJUALAN);
          move.put("reason", "Penjualan " + invoiceNo);
          move.put("referenceSaleId", saleId);
          move.put("userId", uid);
          move.put("userName", cashierName);
          move.put("createdAt", FieldValue.serverTimestamp());
          tx.set(db.collection(FirestoreRefs.STOCK_MOVEMENTS).document(), move);
        }

        Map<String, Object> sale = new HashMap<>();
        sale.put("invoiceNo", invoiceNo);
        sale.put("cashierId", uid);
        sale.put("cashierName", cashierName);
        sale.put("grossTotal", gross);
        sale.put("discountTotal", discount);
        sale.put("total", total);
        sale.put("costTotal", cost);
        sale.put("paidAmount", paidAmount);
        sale.put("changeAmount", paidAmount - total);
        sale.put("status", Constants.SaleStatus.SELESAI);
        sale.put("items", itemMaps);
        sale.put("createdAt", FieldValue.serverTimestamp());
        tx.set(saleRef, sale);

        return saleId;
      })
      .addOnSuccessListener(id -> callback.onSuccess(id))
      .addOnFailureListener(e -> callback.onError(
        businessError[0] != null ? businessError[0] : ErrorMessages.from(e)));
  }

  /** Satu baris barang transaksi sebagai SNAPSHOT (harga dan modal saat transaksi). */
  private static Map<String, Object> itemMap(CartLine l) {
    Map<String, Object> m = new HashMap<>();
    m.put("productId", l.productId);
    m.put("productCode", l.productCode);
    m.put("productName", l.productName);
    m.put("quantity", l.quantity);
    m.put("originalPrice", l.originalPrice);
    m.put("discountPerUnit", l.discountPerUnit);
    m.put("sellPrice", l.unitPrice);
    m.put("buyPrice", l.buyPrice);
    m.put("subtotal", l.subtotal());
    m.put("promotionId", l.promotionId);
    m.put("promotionName", l.promotionName);
    return m;
  }

  private static FirebaseFirestoreException reject(String[] holder, String message) {
    holder[0] = message;
    return new FirebaseFirestoreException(message,
      FirebaseFirestoreException.Code.FAILED_PRECONDITION);
  }
}
