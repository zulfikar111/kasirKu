package com.kasirku.data;

import com.kasirku.session.SessionManager;

/** Pemeriksaan hak akses di lapisan data (lapis kedua setelah layar). */
public final class AccessGuard {

  private AccessGuard() {
  }

  /** true jika admin. Jika bukan, callback langsung diberi pesan penolakan. */
  public static boolean requireAdmin(DataCallback<?> callback) {
    if (!SessionManager.isAdmin()) {
      callback.onError("Anda tidak memiliki izin untuk tindakan ini.");
      return false;
    }
    return true;
  }
}
