package com.kasirku.data;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.kasirku.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Operasi data kategori barang. */
public class CategoryRepository {

  public void loadAll(DataCallback<List<Category>> callback) {
    FirestoreRefs.collection(FirestoreRefs.CATEGORIES).get()
      .addOnSuccessListener(snap -> {
        List<Category> list = new ArrayList<>();
        for (DocumentSnapshot d : snap.getDocuments()) {
          Category c = d.toObject(Category.class);
          if (c != null) {
            list.add(c);
          }
        }
        Collections.sort(list, (a, b) -> nameOf(a).compareToIgnoreCase(nameOf(b)));
        callback.onSuccess(list);
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  public void add(String name, List<Category> existing, DataCallback<Void> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    String error = validateName(name, existing, null);
    if (error != null) {
      callback.onError(error);
      return;
    }
    Map<String, Object> data = new HashMap<>();
    data.put("name", name.trim());
    data.put("createdAt", FieldValue.serverTimestamp());
    FirestoreRefs.collection(FirestoreRefs.CATEGORIES).add(data)
      .addOnSuccessListener(ref -> callback.onSuccess(null))
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  public void rename(String id, String name, List<Category> existing,
                     DataCallback<Void> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    String error = validateName(name, existing, id);
    if (error != null) {
      callback.onError(error);
      return;
    }
    FirestoreRefs.collection(FirestoreRefs.CATEGORIES).document(id)
      .update("name", name.trim())
      .addOnSuccessListener(unused -> callback.onSuccess(null))
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  /** Kategori yang masih dipakai barang (termasuk yang nonaktif) tidak bisa dihapus. */
  public void delete(String id, DataCallback<Void> callback) {
    if (!AccessGuard.requireAdmin(callback)) {
      return;
    }
    FirestoreRefs.collection(FirestoreRefs.PRODUCTS)
      .whereEqualTo("categoryId", id).limit(1).get()
      .addOnSuccessListener(snap -> {
        if (!snap.isEmpty()) {
          callback.onError("Kategori masih dipakai oleh barang, jadi tidak bisa dihapus.");
          return;
        }
        FirestoreRefs.collection(FirestoreRefs.CATEGORIES).document(id).delete()
          .addOnSuccessListener(unused -> callback.onSuccess(null))
          .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  private String validateName(String name, List<Category> existing, String excludeId) {
    String clean = name == null ? "" : name.trim();
    if (clean.isEmpty()) {
      return "Nama kategori wajib diisi.";
    }
    if (clean.length() > 40) {
      return "Nama kategori maksimal 40 karakter.";
    }
    for (Category c : existing) {
      boolean sameItem = excludeId != null && excludeId.equals(c.id);
      if (!sameItem && clean.equalsIgnoreCase(nameOf(c))) {
        return "Kategori dengan nama itu sudah ada.";
      }
    }
    return null;
  }

  private static String nameOf(Category c) {
    return c.name == null ? "" : c.name;
  }
}
