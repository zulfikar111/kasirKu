package com.kasirku.data;

/**
 * Firestore bekerja di latar belakang (tidak langsung selesai).
 * Callback ini dipanggil saat hasilnya sudah datang.
 */
public interface DataCallback<T> {
  void onSuccess(T result);

  /** message sudah berupa kalimat yang aman ditampilkan ke pengguna. */
  void onError(String message);
}
