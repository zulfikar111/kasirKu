package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.kasirku.util.Constants;

import java.util.Date;

/** Data pengguna. ID dokumen = UID akun Firebase Authentication. */
public class User {
  @DocumentId
  public String id;
  public String username;
  public String fullName;
  public String role;          // Constants.Role.ADMIN atau KASIR
  public boolean isActive = true;
  @ServerTimestamp
  public Date createdAt;

  public User() {
    // Wajib ada untuk Firestore
  }

  @Exclude
  public boolean isAdmin() {
    return Constants.Role.ADMIN.equals(role);
  }
}
