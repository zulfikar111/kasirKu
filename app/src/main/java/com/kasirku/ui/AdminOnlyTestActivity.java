package com.kasirku.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kasirku.util.ViewUtil;

/** Layar uji SEMENTARA: hanya admin yang boleh membukanya. */
public class AdminOnlyTestActivity extends BaseActivity {

  @Override
  protected boolean requiresAdmin() {
    return true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!accessGranted) {
      return;
    }

    int pad = (int) (24 * getResources().getDisplayMetrics().density);
    TextView tv = new TextView(this);
    tv.setText("Halaman khusus ADMIN.\nJika Anda melihat ini, hak akses admin berfungsi.");
    tv.setTextSize(18);
    tv.setGravity(Gravity.CENTER);
    tv.setPadding(pad, pad, pad, pad);

    FrameLayout root = new FrameLayout(this);
    root.addView(tv, new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    setContentView(root);
    ViewUtil.applySystemBarPadding(root);
  }
}
