package com.kasirku.data;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/** Mengubah error Firebase menjadi pesan yang mudah dipahami pengguna. */
public final class ErrorMessages {

  public static final String KONEKSI_BERMASALAH =
    "Koneksi internet bermasalah. Data belum tersimpan. "
      + "Periksa sinyal lalu coba lagi.";

  private ErrorMessages() {
  }

  public static String from(Exception e) {
    if (e instanceof FirebaseNetworkException) {
      return KONEKSI_BERMASALAH;
    }
    if (e instanceof FirebaseFirestoreException) {
      FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
      switch (fe.getCode()) {
        case UNAVAILABLE:
        case DEADLINE_EXCEEDED:
          return KONEKSI_BERMASALAH;
        case PERMISSION_DENIED:
          return "Anda tidak memiliki izin untuk tindakan ini.";
        case NOT_FOUND:
          return "Data tidak ditemukan.";
        case ABORTED:
          return "Data sedang diubah dari perangkat lain. Silakan coba lagi.";
        case ALREADY_EXISTS:
          return "Kode barang sudah dipakai barang lain.";
        default:
          break;
      }
    }
    return "Terjadi kesalahan. Silakan coba lagi.";
  }
}
