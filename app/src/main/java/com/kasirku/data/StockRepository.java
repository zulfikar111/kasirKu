package com.kasirku.data;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.kasirku.model.StockMovement;
import com.kasirku.session.SessionManager;
import com.kasirku.util.Constants;
import com.kasirku.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Penyesuaian stok dan pembacaan riwayat perubahan stok. */
public class StockRepository {

  /**
   * Menambah (delta positif) atau mengurangi (delta negatif) stok.
   * Dalam satu transaction: baca stok terbaru, hitung, perbarui barang, catat riwayat.
   * Hasil callback: stok baru.
   */
  public void adjustStock(String productId, int delta, String reason,
                          DataCallback<Integer> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    String inputProblem = Validator.validateAdjustmentInput(delta, reason);
    if (inputProblem != null) {
      callback.onError(inputProblem);
      return;
    }
    String uid = SessionManager.getUid();
    if (uid == null) {
      callback.onError("Sesi berakhir. Silakan masuk lagi.");
      return;
    }
    final String userName = SessionManager.displayName();
    final String cleanReason = reason.trim();

    // Menyimpan pesan penolakan yang sengaja dibuat (bukan error jaringan)
    final String[] businessError = new String[1];

    FirebaseFirestore db = FirestoreRefs.db();
    DocumentReference productRef = db.collection(FirestoreRefs.PRODUCTS).document(productId);
    DocumentReference moveRef = db.collection(FirestoreRefs.STOCK_MOVEMENTS).document();

    db.runTransaction(tx -> {
        DocumentSnapshot snap = tx.get(productRef);
        if (!snap.exists()) {
          businessError[0] = "Barang tidak ditemukan.";
          throw new FirebaseFirestoreException("Barang tidak ditemukan",
            FirebaseFirestoreException.Code.NOT_FOUND);
        }

        Long stockValue = snap.getLong("stock");
        int before = stockValue == null ? 0 : stockValue.intValue();

        String problem = Validator.validateAdjustmentResult(before, delta);
        if (problem != null) {
          businessError[0] = problem;
          throw new FirebaseFirestoreException("Penyesuaian ditolak",
            FirebaseFirestoreException.Code.INVALID_ARGUMENT);
        }
        int after = before + delta;

        tx.update(productRef, "stock", after, "updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> move = new HashMap<>();
        move.put("productId", productId);
        move.put("productName", snap.getString("name"));
        move.put("changeQty", delta);
        move.put("stockBefore", before);
        move.put("stockAfter", after);
        move.put("type", Constants.MovementType.PENYESUAIAN);
        move.put("reason", cleanReason);
        move.put("userId", uid);
        move.put("userName", userName);
        move.put("createdAt", FieldValue.serverTimestamp());
        tx.set(moveRef, move);

        return after;
      })
      .addOnSuccessListener(newStock -> callback.onSuccess(newStock))
      .addOnFailureListener(e -> callback.onError(
        businessError[0] != null ? businessError[0] : ErrorMessages.from(e)));
  }

  /** Riwayat perubahan stok satu barang, terbaru di atas. Khusus admin. */
  public void loadMovements(String productId, DataCallback<List<StockMovement>> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    FirestoreRefs.collection(FirestoreRefs.STOCK_MOVEMENTS)
      .whereEqualTo("productId", productId).get()
      .addOnSuccessListener(snap -> {
        List<StockMovement> list = new ArrayList<>();
        for (DocumentSnapshot d : snap.getDocuments()) {
          StockMovement m = d.toObject(StockMovement.class);
          if (m != null) {
            list.add(m);
          }
        }
        // Terbaru di atas. Catatan yang belum punya waktu server dianggap paling baru.
        Collections.sort(list, (a, b) -> {
          long ta = a.createdAt == null ? Long.MAX_VALUE : a.createdAt.getTime();
          long tb = b.createdAt == null ? Long.MAX_VALUE : b.createdAt.getTime();
          return Long.compare(tb, ta);
        });
        callback.onSuccess(list);
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }
}
