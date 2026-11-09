package com.kasirku.data;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.kasirku.model.User;
import com.kasirku.session.SessionManager;
import com.kasirku.util.Constants;
import com.kasirku.util.UsernameUtil;

/** Login dan pemuatan data pengguna yang sedang masuk. */
public class UserRepository {

  private static final String PESAN_OFFLINE =
    "Tidak ada koneksi internet. Periksa sinyal lalu coba lagi.";

  /** Masuk dengan username dan password. */
  public void login(String username, String password, DataCallback<User> callback) {
    FirebaseAuth.getInstance()
      .signInWithEmailAndPassword(UsernameUtil.toEmail(username), password)
      .addOnSuccessListener(result -> {
        FirebaseUser firebaseUser = result.getUser();
        if (firebaseUser == null) {
          callback.onError("Gagal masuk. Silakan coba lagi.");
          return;
        }
        loadProfile(firebaseUser.getUid(), callback);
      })
      .addOnFailureListener(e -> callback.onError(authErrorMessage(e)));
  }

  /** Memuat data pengguna yang sudah login sebelumnya (saat aplikasi dibuka). */
  public void loadCurrentUser(DataCallback<User> callback) {
    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    if (firebaseUser == null) {
      callback.onError("Belum masuk.");
      return;
    }
    loadProfile(firebaseUser.getUid(), callback);
  }

  private void loadProfile(String uid, DataCallback<User> callback) {
    FirestoreRefs.collection(FirestoreRefs.USERS).document(uid).get()
      .addOnSuccessListener(doc -> {
        if (!doc.exists()) {
          SessionManager.logout();
          callback.onError("Akun belum terdaftar di toko. Hubungi admin.");
          return;
        }
        User user = doc.toObject(User.class);
        if (user == null || !isRoleValid(user.role)) {
          SessionManager.logout();
          callback.onError("Data akun tidak lengkap. Hubungi admin.");
          return;
        }
        if (!user.isActive) {
          SessionManager.logout();
          callback.onError("Akun Anda dinonaktifkan. Hubungi admin.");
          return;
        }
        SessionManager.setUser(user);
        callback.onSuccess(user);
      })
      .addOnFailureListener(e -> callback.onError(ErrorMessages.from(e)));
  }

  private boolean isRoleValid(String role) {
    return Constants.Role.ADMIN.equals(role) || Constants.Role.KASIR.equals(role);
  }

  private String authErrorMessage(Exception e) {
    if (e instanceof FirebaseNetworkException) {
      return PESAN_OFFLINE;
    }
    if (e instanceof FirebaseAuthInvalidUserException
      || e instanceof FirebaseAuthInvalidCredentialsException) {
      // Sengaja tidak menyebut mana yang salah (username atau password)
      return "Username atau password salah.";
    }
    if (e instanceof FirebaseTooManyRequestsException) {
      return "Terlalu banyak percobaan. Tunggu beberapa menit lalu coba lagi.";
    }
    return "Gagal masuk. Silakan coba lagi.";
  }
}
