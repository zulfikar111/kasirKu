package com.kasirku.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Pembantu untuk memeriksa apakah HP sedang terhubung ke internet.
 * Dipakai nanti sebelum menyimpan transaksi.
 */
public class NetworkUtil {

  public static final String PESAN_TIDAK_ADA_INTERNET =
    "Tidak ada koneksi internet. Transaksi belum disimpan. "
      + "Periksa sinyal lalu coba lagi.";

  private NetworkUtil() {
    // Kelas utilitas, tidak untuk dibuat objeknya
  }

  public static boolean isOnline(Context context) {
    ConnectivityManager cm = (ConnectivityManager)
      context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) {
      return false;
    }
    Network network = cm.getActiveNetwork();
    if (network == null) {
      return false;
    }
    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
    return caps != null
      && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }
}
