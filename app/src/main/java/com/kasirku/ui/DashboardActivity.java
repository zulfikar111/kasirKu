package com.kasirku.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kasirku.R;
import com.kasirku.session.SessionManager;
import com.kasirku.util.ViewUtil;

/** Kerangka utama: navigasi bawah dan tempat fragment. Menu menyesuaikan peran. */
public class DashboardActivity extends BaseActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!accessGranted) {
      return;
    }
    setContentView(R.layout.activity_dashboard);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    BottomNavigationView nav = findViewById(R.id.bottomNav);
    nav.inflateMenu(SessionManager.isAdmin()
      ? R.menu.menu_bottom_admin : R.menu.menu_bottom_kasir);
    nav.setOnItemSelectedListener(item -> {
      show(fragmentFor(item.getItemId()));
      return true;
    });

    if (savedInstanceState == null) {
      nav.setSelectedItemId(R.id.nav_home);
    }
  }

  private Fragment fragmentFor(int id) {
    if (id == R.id.nav_home) {
      return new DashboardFragment();
    }
    if (id == R.id.nav_products) {
      return new ProductListFragment();
    }
    if (id == R.id.nav_transaction) {
      return new TransactionFragment();
    }
    if (id == R.id.nav_report) {
      return PlaceholderFragment.newInstance("Laporan",
        "Laporan keuangan dibuat di Tahap 8.");
    }
    return PlaceholderFragment.newInstance("Riwayat",
      "Riwayat transaksi dibuat di Tahap 7.");
  }

  private void show(Fragment fragment) {
    getSupportFragmentManager().beginTransaction()
      .replace(R.id.container, fragment)
      .commit();
  }
}
