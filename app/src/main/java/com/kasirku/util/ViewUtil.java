package com.kasirku.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Pembantu tampilan yang dipakai banyak layar. */
public final class ViewUtil {

  private ViewUtil() {
  }

  /**
   * Memberi jarak pada layar agar isinya tidak tertutup status bar
   * dan tombol navigasi HP. Panggil pada view paling luar.
   */
  public static void applySystemBarPadding(View root) {
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
      Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return windowInsets;
    });
  }
}
