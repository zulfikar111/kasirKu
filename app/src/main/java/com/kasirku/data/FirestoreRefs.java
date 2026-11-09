package com.kasirku.data;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/** Nama-nama collection Firestore di satu tempat. */
public final class FirestoreRefs {

  public static final String USERS = "users";
  public static final String CATEGORIES = "categories";
  public static final String PRODUCTS = "products";
  public static final String PRODUCT_CODES = "product_codes";
  public static final String SALES = "sales";
  public static final String STOCK_MOVEMENTS = "stock_movements";
  public static final String EXPENSES = "expenses";
  public static final String PROMOTIONS = "promotions";
  public static final String COUNTERS = "counters";
  public static final String SETTINGS = "settings";

  private FirestoreRefs() {
  }

  public static FirebaseFirestore db() {
    return FirebaseFirestore.getInstance();
  }

  public static CollectionReference collection(String name) {
    return db().collection(name);
  }
}
