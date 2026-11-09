package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;
import com.kasirku.R;
import com.kasirku.data.DataCallback;
import com.kasirku.data.ProductRepository;
import com.kasirku.model.Product;
import com.kasirku.session.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Beranda. Angka berasal dari database; ringkasan penjualan menyusul di Tahap 7-8. */
public class DashboardFragment extends Fragment {

  private final ProductRepository productRepository = new ProductRepository();
  private ListenerRegistration registration;

  private TextView tvAvailable;
  private TextView tvLow;
  private TextView tvLowList;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_dashboard, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    TextView tvGreeting = view.findViewById(R.id.tvGreeting);
    TextView tvRole = view.findViewById(R.id.tvRole);
    tvAvailable = view.findViewById(R.id.tvAvailable);
    tvLow = view.findViewById(R.id.tvLow);
    tvLowList = view.findViewById(R.id.tvLowList);
    View cardAdminMenu = view.findViewById(R.id.cardAdminMenu);
    MaterialButton btnCategories = view.findViewById(R.id.btnCategories);
    MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

    boolean isAdmin = SessionManager.isAdmin();
    tvGreeting.setText("Halo, " + SessionManager.displayName());
    tvRole.setText("Peran: " + (isAdmin ? "Admin" : "Kasir"));

    cardAdminMenu.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    btnCategories.setOnClickListener(v ->
      startActivity(new Intent(requireContext(), CategoryActivity.class)));

    btnLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
      .setTitle("Keluar")
      .setMessage("Keluar dari akun ini?")
      .setNegativeButton("Batal", null)
      .setPositiveButton("Keluar", (dialog, which) -> {
        SessionManager.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
      })
      .show());

    registration = productRepository.listen(new DataCallback<List<Product>>() {
      @Override
      public void onSuccess(List<Product> result) {
        if (getView() != null) {
          bind(result);
        }
      }

      @Override
      public void onError(String message) {
        if (getView() != null && SessionManager.isLoggedIn()) {
          Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  private void bind(List<Product> products) {
    int available = 0;
    List<Product> lows = new ArrayList<>();
    for (Product p : products) {
      if (!p.isActive) {
        continue;
      }
      if (p.stock > 0) {
        available++;
      }
      if (p.isLowStock()) {
        lows.add(p);
      }
    }
    Collections.sort(lows, (a, b) -> Integer.compare(a.stock, b.stock));

    tvAvailable.setText(String.valueOf(available));
    tvLow.setText(String.valueOf(lows.size()));

    if (products.isEmpty()) {
      tvLowList.setText("Belum ada barang.");
    } else if (lows.isEmpty()) {
      tvLowList.setText("Tidak ada barang yang stoknya menipis.");
    } else {
      StringBuilder sb = new StringBuilder();
      int shown = Math.min(5, lows.size());
      for (int i = 0; i < shown; i++) {
        Product p = lows.get(i);
        if (i > 0) {
          sb.append('\n');
        }
        sb.append("\u2022 ").append(p.name == null ? "" : p.name)
          .append(p.stock <= 0 ? " - habis" : " - sisa " + p.stock);
      }
      if (lows.size() > shown) {
        sb.append("\n... dan ").append(lows.size() - shown).append(" lainnya");
      }
      tvLowList.setText(sb.toString());
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (registration != null) {
      registration.remove();
      registration = null;
    }
  }
}
