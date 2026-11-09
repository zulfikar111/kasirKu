package com.kasirku.data;

import com.google.firebase.firestore.DocumentSnapshot;
import com.kasirku.model.Promotion;

import java.util.ArrayList;
import java.util.List;

/** Membaca promo. Pembuatan promo (admin) dikerjakan di Tahap 7C. */
public class PromotionRepository {

  public void loadAll(DataCallback<List<Promotion>> callback) {
    FirestoreRefs.collection(FirestoreRefs.PROMOTIONS).get()
      .addOnSuccessListener(snap -> {
        List<Promotion> list = new ArrayList<>();
        for (DocumentSnapshot d : snap.getDocuments()) {
          Promotion p = d.toObject(Promotion.class);
          if (p != null) {
            list.add(p);
          }
        }
        callback.onSuccess(list);
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }
}
