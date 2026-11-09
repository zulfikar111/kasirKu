package com.kasirku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.kasirku.data.DataCallback;
import com.kasirku.data.UserRepository;
import com.kasirku.model.User;
import com.kasirku.ui.DashboardActivity;
import com.kasirku.ui.LoginActivity;
import com.kasirku.util.ViewUtil;

/**
 * Pintu masuk aplikasi: memutuskan apakah pengguna dikirim ke Login
 * atau langsung ke beranda (jika sudah pernah masuk).
 */
public class MainActivity extends AppCompatActivity {

  private View progress;
  private TextView tvStatus;
  private MaterialButton btnRetry;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    progress = findViewById(R.id.progress);
    tvStatus = findViewById(R.id.tvStatus);
    btnRetry = findViewById(R.id.btnRetry);
    btnRetry.setOnClickListener(v -> start());

    start();
  }

  private void start() {
    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
      openLogin();
      return;
    }

    progress.setVisibility(View.VISIBLE);
    btnRetry.setVisibility(View.GONE);
    tvStatus.setText("Memuat...");

    new UserRepository().loadCurrentUser(new DataCallback<User>() {
      @Override
      public void onSuccess(User result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        startActivity(new Intent(MainActivity.this, DashboardActivity.class));
        finish();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
          // Akun ditolak dan sudah dikeluarkan (nonaktif, tidak terdaftar, dll.)
          Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
          openLogin();
        } else {
          // Kemungkinan masalah sinyal: beri kesempatan mencoba lagi
          progress.setVisibility(View.GONE);
          tvStatus.setText(message);
          btnRetry.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  private void openLogin() {
    startActivity(new Intent(this, LoginActivity.class));
    finish();
  }
}
