package com.kasirku.session;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kasirku.model.User;

/**
 * Menyimpan pengguna yang sedang login (di memori).
 * Login Firebase sendiri tersimpan otomatis di HP; data peran dimuat
 * ulang dari Firestore setiap aplikasi dibuka.
 */
public final class SessionManager {

  private static User currentUser;

  private SessionManager() {
  }

  public static void setUser(User user) {
    currentUser = user;
  }

  public static User getUser() {
    return currentUser;
  }

  public static boolean isLoggedIn() {
    return currentUser != null;
  }

  public static boolean isAdmin() {
    return currentUser != null && currentUser.isAdmin();
  }

  /** UID akun Firebase yang sedang masuk, atau null. */
  public static String getUid() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    return user == null ? null : user.getUid();
  }

  /** Nama yang tampil di layar dan di catatan (nama lengkap, atau username). */
  public static String displayName() {
    if (currentUser == null) {
      return "";
    }
    if (currentUser.fullName != null && !currentUser.fullName.trim().isEmpty()) {
      return currentUser.fullName;
    }
    return currentUser.username == null ? "" : currentUser.username;
  }

  public static void logout() {
    currentUser = null;
    CartManager.clear();
    FirebaseAuth.getInstance().signOut();
  }
}
