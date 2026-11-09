package com.kasirku.data;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.kasirku.model.Product;
import com.kasirku.model.ProductInput;
import com.kasirku.session.SessionManager;
import com.kasirku.util.Constants;
import com.kasirku.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Operasi data barang. Perubahan penting memakai Firestore transaction. */
public class ProductRepository {

  /** Mendengarkan semua barang. Berubah otomatis saat ada perubahan (juga dari HP lain). */
  public ListenerRegistration listen(DataCallback<List<Product>> callback) {
    return FirestoreRefs.collection(FirestoreRefs.PRODUCTS)
      .addSnapshotListener((snap, e) -> {
        if (e != null) {
          callback.onError(ErrorMessages.from(e));
          return;
        }
        if (snap == null) {
          return;
        }
        List<Product> list = new ArrayList<>();
        for (DocumentSnapshot d : snap.getDocuments()) {
          Product p = d.toObject(Product.class);
          if (p != null) {
            list.add(p);
          }
        }
        Collections.sort(list, (a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));
        callback.onSuccess(list);
      });
  }

  public void getById(String id, DataCallback<Product> callback) {
    FirestoreRefs.collection(FirestoreRefs.PRODUCTS).document(id).get()
      .addOnSuccessListener(doc -> {
        Product p = doc.exists() ? doc.toObject(Product.class) : null;
        if (p == null) {
          callback.onError("Barang tidak ditemukan.");
        } else {
          callback.onSuccess(p);
        }
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  /**
   * Menambah barang. Dalam satu transaction: cek kode belum dipakai, simpan barang,
   * simpan indeks kode, dan catat stok awal (jika lebih dari 0).
   */
  public void addProduct(ProductInput input, DataCallback<Void> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    ProductInput in = normalized(input);
    String problem = Validator.validateProduct(in, true);
    if (problem != null) {
      callback.onError(problem);
      return;
    }
    String uid = SessionManager.getUid();
    if (uid == null) {
      callback.onError("Sesi berakhir. Silakan masuk lagi.");
      return;
    }
    String userName = SessionManager.displayName();

    FirebaseFirestore db = FirestoreRefs.db();
    DocumentReference productRef = db.collection(FirestoreRefs.PRODUCTS).document();
    DocumentReference codeRef = db.collection(FirestoreRefs.PRODUCT_CODES).document(in.code);
    DocumentReference moveRef = db.collection(FirestoreRefs.STOCK_MOVEMENTS).document();

    db.runTransaction(tx -> {
        // 1. Semua pembacaan dulu
        DocumentSnapshot codeSnap = tx.get(codeRef);
        if (codeSnap.exists()) {
          throw new FirebaseFirestoreException("Kode sudah dipakai",
            FirebaseFirestoreException.Code.ALREADY_EXISTS);
        }

        // 2. Lalu penulisan
        Map<String, Object> product = new HashMap<>();
        product.put("code", in.code);
        product.put("name", in.name);
        product.put("categoryId", in.categoryId);
        product.put("buyPrice", in.buyPrice);
        product.put("sellPrice", in.sellPrice);
        product.put("stock", in.stock);
        product.put("minStock", in.minStock);
        product.put("isActive", true);
        product.put("createdAt", FieldValue.serverTimestamp());
        product.put("updatedAt", FieldValue.serverTimestamp());
        tx.set(productRef, product);

        Map<String, Object> codeIndex = new HashMap<>();
        codeIndex.put("productId", productRef.getId());
        tx.set(codeRef, codeIndex);

        if (in.stock > 0) {
          Map<String, Object> move = new HashMap<>();
          move.put("productId", productRef.getId());
          move.put("productName", in.name);
          move.put("changeQty", in.stock);
          move.put("stockBefore", 0);
          move.put("stockAfter", in.stock);
          move.put("type", Constants.MovementType.STOK_AWAL);
          move.put("reason", "Stok awal");
          move.put("userId", uid);
          move.put("userName", userName);
          move.put("createdAt", FieldValue.serverTimestamp());
          tx.set(moveRef, move);
        }
        return null;
      })
      .addOnSuccessListener(result -> callback.onSuccess(null))
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  /** Mengubah data barang (BUKAN stok). Jika kode berubah, indeks kode ikut dipindah. */
  public void updateProduct(String productId, ProductInput input, DataCallback<Void> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    ProductInput in = normalized(input);
    String problem = Validator.validateProduct(in, false);
    if (problem != null) {
      callback.onError(problem);
      return;
    }

    FirebaseFirestore db = FirestoreRefs.db();
    DocumentReference productRef = db.collection(FirestoreRefs.PRODUCTS).document(productId);
    DocumentReference newCodeRef = db.collection(FirestoreRefs.PRODUCT_CODES).document(in.code);

    db.runTransaction(tx -> {
        DocumentSnapshot snap = tx.get(productRef);
        if (!snap.exists()) {
          throw new FirebaseFirestoreException("Barang tidak ditemukan",
            FirebaseFirestoreException.Code.NOT_FOUND);
        }
        String oldCode = snap.getString("code");
        boolean codeChanged = oldCode == null || !oldCode.equals(in.code);
        if (codeChanged) {
          DocumentSnapshot codeSnap = tx.get(newCodeRef);
          if (codeSnap.exists()) {
            throw new FirebaseFirestoreException("Kode sudah dipakai",
              FirebaseFirestoreException.Code.ALREADY_EXISTS);
          }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("code", in.code);
        data.put("name", in.name);
        data.put("categoryId", in.categoryId);
        data.put("buyPrice", in.buyPrice);
        data.put("sellPrice", in.sellPrice);
        data.put("minStock", in.minStock);
        data.put("updatedAt", FieldValue.serverTimestamp());
        tx.update(productRef, data);

        if (codeChanged) {
          if (oldCode != null && !oldCode.isEmpty()) {
            tx.delete(db.collection(FirestoreRefs.PRODUCT_CODES).document(oldCode));
          }
          Map<String, Object> codeIndex = new HashMap<>();
          codeIndex.put("productId", productId);
          tx.set(newCodeRef, codeIndex);
        }
        return null;
      })
      .addOnSuccessListener(result -> callback.onSuccess(null))
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  /** Barang tidak dihapus, hanya dinonaktifkan, agar riwayat transaksi tetap utuh. */
  public void setActive(String productId, boolean active, DataCallback<Void> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    Map<String, Object> data = new HashMap<>();
    data.put("isActive", active);
    data.put("updatedAt", FieldValue.serverTimestamp());
    FirestoreRefs.collection(FirestoreRefs.PRODUCTS).document(productId).update(data)
      .addOnSuccessListener(unused -> callback.onSuccess(null))
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  private ProductInput normalized(ProductInput input) {
    ProductInput in = new ProductInput();
    in.code = Validator.normalizeCode(input.code);
    in.name = input.name == null ? "" : input.name.trim();
    in.categoryId = input.categoryId;
    in.buyPrice = input.buyPrice;
    in.sellPrice = input.sellPrice;
    in.stock = input.stock;
    in.minStock = input.minStock;
    return in;
  }

  private static String nameOf(Product p) {
    return p.name == null ? "" : p.name;
  }
}
