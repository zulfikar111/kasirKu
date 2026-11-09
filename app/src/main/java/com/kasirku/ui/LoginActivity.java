package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kasirku.R;
import com.kasirku.data.DataCallback;
import com.kasirku.data.UserRepository;
import com.kasirku.model.User;
import com.kasirku.util.NetworkUtil;
import com.kasirku.util.UsernameUtil;
import com.kasirku.util.ViewUtil;

public class LoginActivity extends AppCompatActivity {

  private TextInputLayout layoutUsername;
  private TextInputLayout layoutPassword;
  private TextInputEditText etUsername;
  private TextInputEditText etPassword;
  private MaterialButton btnLogin;
  private ProgressBar progress;
  private TextView tvError;

  private final UserRepository userRepository = new UserRepository();
  private boolean isSubmitting = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_login);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    layoutUsername = findViewById(R.id.layoutUsername);
    layoutPassword = findViewById(R.id.layoutPassword);
    etUsername = findViewById(R.id.etUsername);
    etPassword = findViewById(R.id.etPassword);
    btnLogin = findViewById(R.id.btnLogin);
    progress = findViewById(R.id.progress);
    tvError = findViewById(R.id.tvError);

    btnLogin.setOnClickListener(v -> attemptLogin());
    etPassword.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        attemptLogin();
        return true;
      }
      return false;
    });
  }

  private void attemptLogin() {
    if (isSubmitting) {
      return; // cegah tombol ditekan berulang
    }

    layoutUsername.setError(null);
    layoutPassword.setError(null);
    tvError.setVisibility(View.GONE);

    String username = etUsername.getText() == null ? "" : etUsername.getText().toString();
    String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

    boolean valid = true;
    if (UsernameUtil.normalize(username).isEmpty()) {
      layoutUsername.setError("Username wajib diisi");
      valid = false;
    } else if (!UsernameUtil.isValid(username)) {
      layoutUsername.setError("Username 3-30 karakter: huruf, angka, titik, atau garis bawah");
      valid = false;
    }
    if (password.isEmpty()) {
      layoutPassword.setError("Password wajib diisi");
      valid = false;
    }
    if (!valid) {
      return;
    }

    if (!NetworkUtil.isOnline(this)) {
      showError("Tidak ada koneksi internet. Periksa sinyal lalu coba lagi.");
      return;
    }

    setLoading(true);
    userRepository.login(username, password, new DataCallback<User>() {
      @Override
      public void onSuccess(User result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        setLoading(false);
        showError(message);
      }
    });
  }

  private void setLoading(boolean loading) {
    isSubmitting = loading;
    btnLogin.setEnabled(!loading);
    etUsername.setEnabled(!loading);
    etPassword.setEnabled(!loading);
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
  }

  private void showError(String message) {
    tvError.setText(message);
    tvError.setVisibility(View.VISIBLE);
  }
}
