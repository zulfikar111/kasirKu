package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.kasirku.MainActivity;
import com.kasirku.session.SessionManager;

/**
 * Induk semua layar yang butuh login. Memeriksa hak akses SEBELUM layar
 * ditampilkan, jadi bukan sekadar menyembunyikan tombol.
 *
 * Cara pakai di layar turunan:
 *   super.onCreate(savedInstanceState);
 *   if (!accessGranted) return;
 */
public abstract class BaseActivity extends AppCompatActivity {

  /** true jika pengguna boleh melihat layar ini. */
  protected boolean accessGranted = false;

  /** Ganti menjadi true di layar khusus admin. */
  protected boolean requiresAdmin() {
    return false;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);

    if (!SessionManager.isLoggedIn()) {
      // Sesi hilang (misalnya aplikasi dimatikan sistem): muat ulang dari awal
      Intent intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
      finish();
      return;
    }

    if (requiresAdmin() && !SessionManager.isAdmin()) {
      Toast.makeText(this, "Anda tidak memiliki akses ke halaman ini.",
        Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    accessGranted = true;
  }
}
