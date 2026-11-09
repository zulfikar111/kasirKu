package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.kasirku.R;
import com.kasirku.adapter.ProductAdapter;
import com.kasirku.data.CategoryRepository;
import com.kasirku.data.DataCallback;
import com.kasirku.data.ProductRepository;
import com.kasirku.model.Category;
import com.kasirku.model.Product;
import com.kasirku.session.SessionManager;
import com.kasirku.util.SimpleTextWatcher;

import android.text.Editable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Daftar barang: cari, filter kategori. Admin bisa menambah dan mengedit. */
public class ProductListFragment extends Fragment {

  private final ProductRepository productRepository = new ProductRepository();
  private final CategoryRepository categoryRepository = new CategoryRepository();
  private final List<Product> allProducts = new ArrayList<>();
  private final Map<String, String> categoryNames = new HashMap<>();
  private List<Category> categories = new ArrayList<>();

  private String selectedCategoryId = null; // null = semua kategori
  private boolean productsLoaded = false;
  private boolean rebuildingChips = false;
  private ListenerRegistration registration;

  private TextInputEditText etSearch;
  private ChipGroup chipGroup;
  private CheckBox cbInactive;
  private ProgressBar progress;
  private TextView tvEmpty;
  private ProductAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_product_list, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    boolean isAdmin = SessionManager.isAdmin();

    etSearch = view.findViewById(R.id.etSearch);
    chipGroup = view.findViewById(R.id.chipGroup);
    cbInactive = view.findViewById(R.id.cbInactive);
    progress = view.findViewById(R.id.progress);
    tvEmpty = view.findViewById(R.id.tvEmpty);
    RecyclerView rv = view.findViewById(R.id.rvProducts);
    ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

    if (isAdmin) {
      adapter = new ProductAdapter(product -> openForm(product.id));
      cbInactive.setVisibility(View.VISIBLE);
      cbInactive.setOnCheckedChangeListener((button, checked) -> applyFilter());
      fabAdd.setVisibility(View.VISIBLE);
      fabAdd.setOnClickListener(v -> openForm(null));
    } else {
      adapter = new ProductAdapter(null); // kasir: hanya melihat
    }
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    rv.setAdapter(adapter);

    etSearch.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        applyFilter();
      }
    });

    chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
      if (rebuildingChips) {
        return;
      }
      if (checkedIds.isEmpty()) {
        selectedCategoryId = null;
      } else {
        View chip = group.findViewById(checkedIds.get(0));
        selectedCategoryId = chip == null ? null : (String) chip.getTag();
      }
      applyFilter();
    });

    registration = productRepository.listen(new DataCallback<List<Product>>() {
      @Override
      public void onSuccess(List<Product> result) {
        if (getView() == null) {
          return;
        }
        allProducts.clear();
        allProducts.addAll(result);
        productsLoaded = true;
        applyFilter();
      }

      @Override
      public void onError(String message) {
        if (getView() != null && SessionManager.isLoggedIn()) {
          Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    // Muat ulang kategori (mungkin baru diubah di layar Kategori)
    categoryRepository.loadAll(new DataCallback<List<Category>>() {
      @Override
      public void onSuccess(List<Category> result) {
        if (!isAdded() || getView() == null) {
          return;
        }
        categories = result;
        categoryNames.clear();
        for (Category c : result) {
          categoryNames.put(c.id, c.name == null ? "" : c.name);
        }
        adapter.setCategoryNames(categoryNames);
        buildChips();
      }

      @Override
      public void onError(String message) {
        if (isAdded() && getView() != null) {
          Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  private void buildChips() {
    rebuildingChips = true;
    chipGroup.removeAllViews();
    addChip("Semua", null);
    for (Category c : categories) {
      addChip(c.name == null ? "" : c.name, c.id);
    }

    boolean found = false;
    for (int i = 0; i < chipGroup.getChildCount(); i++) {
      Chip chip = (Chip) chipGroup.getChildAt(i);
      if (Objects.equals(chip.getTag(), selectedCategoryId)) {
        chip.setChecked(true);
        found = true;
        break;
      }
    }
    if (!found) {
      selectedCategoryId = null;
      ((Chip) chipGroup.getChildAt(0)).setChecked(true);
    }
    rebuildingChips = false;
    applyFilter();
  }

  private void addChip(String label, String categoryId) {
    Chip chip = (Chip) getLayoutInflater()
      .inflate(R.layout.item_chip_filter, chipGroup, false);
    chip.setId(View.generateViewId());
    chip.setText(label);
    chip.setTag(categoryId);
    chipGroup.addView(chip);
  }

  private void applyFilter() {
    if (adapter == null) {
      return;
    }
    boolean showInactive = cbInactive.getVisibility() == View.VISIBLE && cbInactive.isChecked();
    String query = etSearch.getText() == null
      ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);

    List<Product> result = new ArrayList<>();
    for (Product p : allProducts) {
      if (!p.isActive && !showInactive) {
        continue;
      }
      if (selectedCategoryId != null && !selectedCategoryId.equals(p.categoryId)) {
        continue;
      }
      if (!query.isEmpty()) {
        String name = p.name == null ? "" : p.name.toLowerCase(Locale.ROOT);
        String code = p.code == null ? "" : p.code.toLowerCase(Locale.ROOT);
        if (!name.contains(query) && !code.contains(query)) {
          continue;
        }
      }
      result.add(p);
    }
    adapter.setItems(result);

    progress.setVisibility(productsLoaded ? View.GONE : View.VISIBLE);
    if (productsLoaded && result.isEmpty()) {
      if (allProducts.isEmpty()) {
        tvEmpty.setText(SessionManager.isAdmin()
          ? "Belum ada barang.\nTekan Tambah Barang untuk membuat barang pertama."
          : "Belum ada barang.\nHubungi admin toko.");
      } else {
        tvEmpty.setText("Tidak ada barang yang cocok.");
      }
      tvEmpty.setVisibility(View.VISIBLE);
    } else {
      tvEmpty.setVisibility(View.GONE);
    }
  }

  private void openForm(@Nullable String productId) {
    Intent intent = new Intent(requireContext(), ProductFormActivity.class);
    if (productId != null) {
      intent.putExtra(ProductFormActivity.EXTRA_PRODUCT_ID, productId);
    }
    startActivity(intent);
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
