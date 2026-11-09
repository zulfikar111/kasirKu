package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kasirku.R;
import com.kasirku.data.CategoryRepository;
import com.kasirku.data.DataCallback;
import com.kasirku.data.ProductRepository;
import com.kasirku.model.Category;
import com.kasirku.model.Product;
import com.kasirku.model.ProductInput;
import com.kasirku.util.NetworkUtil;
import com.kasirku.util.RupiahFormat;
import com.kasirku.util.Validator;
import com.kasirku.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/** Form tambah/edit barang (khusus admin). Tanpa EXTRA_PRODUCT_ID = tambah barang baru. */
public class ProductFormActivity extends BaseActivity {

  public static final String EXTRA_PRODUCT_ID = "product_id";

  private final ProductRepository productRepository = new ProductRepository();
  private final CategoryRepository categoryRepository = new CategoryRepository();
  private List<Category> categories = new ArrayList<>();

  private String productId;          // null = tambah baru
  private Product editing;           // terisi setelah barang yang diedit selesai dimuat
  private String selectedCategoryId;
  private boolean categoriesLoaded = false;
  private boolean isSubmitting = false;

  private TextInputLayout layoutCode, layoutName, layoutCategory, layoutBuy, layoutSell,
    layoutStock, layoutMinStock;
  private TextInputEditText etCode, etName, etBuy, etSell, etStock, etMinStock;
  private MaterialAutoCompleteTextView actCategory;
  private TextView tvNoCategory, tvStockInfo, tvError;
  private MaterialButton btnGoCategory, btnSave, btnToggleActive, btnAdjustStock;
  private ProgressBar progress;

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
    setContentView(R.layout.activity_product_form);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> finish());

    layoutCode = findViewById(R.id.layoutCode);
    layoutName = findViewById(R.id.layoutName);
    layoutCategory = findViewById(R.id.layoutCategory);
    layoutBuy = findViewById(R.id.layoutBuy);
    layoutSell = findViewById(R.id.layoutSell);
    layoutStock = findViewById(R.id.layoutStock);
    layoutMinStock = findViewById(R.id.layoutMinStock);
    etCode = findViewById(R.id.etCode);
    etName = findViewById(R.id.etName);
    etBuy = findViewById(R.id.etBuy);
    etSell = findViewById(R.id.etSell);
    etStock = findViewById(R.id.etStock);
    etMinStock = findViewById(R.id.etMinStock);
    actCategory = findViewById(R.id.actCategory);
    tvNoCategory = findViewById(R.id.tvNoCategory);
    tvStockInfo = findViewById(R.id.tvStockInfo);
    tvError = findViewById(R.id.tvError);
    btnGoCategory = findViewById(R.id.btnGoCategory);
    btnSave = findViewById(R.id.btnSave);
    btnToggleActive = findViewById(R.id.btnToggleActive);
    btnAdjustStock = findViewById(R.id.btnAdjustStock);
    progress = findViewById(R.id.progress);

    actCategory.setOnItemClickListener((parent, view, position, id) ->
      selectedCategoryId = categories.get(position).id);
    btnGoCategory.setOnClickListener(v ->
      startActivity(new Intent(this, CategoryActivity.class)));
    btnSave.setOnClickListener(v -> trySave());
    btnToggleActive.setOnClickListener(v -> confirmToggleActive());
    btnAdjustStock.setOnClickListener(v -> {
      Intent intent = new Intent(this, StockAdjustActivity.class);
      intent.putExtra(StockAdjustActivity.EXTRA_PRODUCT_ID, productId);
      startActivity(intent);
    });

    productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
    if (productId == null) {
      toolbar.setTitle("Tambah Barang");
      etMinStock.setText("5");
    } else {
      toolbar.setTitle("Edit Barang");
      layoutStock.setVisibility(View.GONE);
      tvStockInfo.setVisibility(View.VISIBLE);
      tvStockInfo.setText("Memuat data barang...");
      loadProduct();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (accessGranted) {
      loadCategories();   // juga dipanggil lagi setelah kembali dari layar Kategori
      refreshStockInfo(); // stok mungkin baru diubah di layar Penyesuaian Stok
    }
  }

  private String stockInfoText(int stock) {
    return "Stok saat ini: " + stock + " pcs. Stok tidak diubah di sini, tetapi lewat "
      + "tombol Sesuaikan Stok agar tercatat.";
  }

  private void loadProduct() {
    progress.setVisibility(View.VISIBLE);
    productRepository.getById(productId, new DataCallback<Product>() {
      @Override
      public void onSuccess(Product p) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        progress.setVisibility(View.GONE);
        editing = p;
        etCode.setText(p.code);
        etName.setText(p.name);
        etBuy.setText(String.valueOf(p.buyPrice));
        etSell.setText(String.valueOf(p.sellPrice));
        etMinStock.setText(String.valueOf(p.minStock));
        tvStockInfo.setText(stockInfoText(p.stock));
        selectedCategoryId = p.categoryId;
        btnAdjustStock.setVisibility(View.VISIBLE);
        btnToggleActive.setVisibility(View.VISIBLE);
        btnToggleActive.setText(p.isActive ? "Nonaktifkan barang" : "Aktifkan barang");
        if (categoriesLoaded) {
          refreshCategoryDropdown();
        }
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        progress.setVisibility(View.GONE);
        Toast.makeText(ProductFormActivity.this, message, Toast.LENGTH_LONG).show();
        finish();
      }
    });
  }

  /** Memperbarui informasi stok saja, tanpa menimpa isian yang sedang diketik. */
  private void refreshStockInfo() {
    if (productId == null || editing == null) {
      return;
    }
    productRepository.getById(productId, new DataCallback<Product>() {
      @Override
      public void onSuccess(Product p) {
        if (isFinishing() || isDestroyed() || editing == null) {
          return;
        }
        editing.stock = p.stock;
        tvStockInfo.setText(stockInfoText(p.stock));
      }

      @Override
      public void onError(String message) {
        // Diabaikan: info stok lama tetap tampil
      }
    });
  }

  private void loadCategories() {
    categoryRepository.loadAll(new DataCallback<List<Category>>() {
      @Override
      public void onSuccess(List<Category> result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        categories = result;
        categoriesLoaded = true;
        refreshCategoryDropdown();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        showError(message);
      }
    });
  }

  private void refreshCategoryDropdown() {
    List<String> labels = new ArrayList<>();
    String selectedName = null;
    for (Category c : categories) {
      String name = c.name == null ? "" : c.name;
      labels.add(name);
      if (c.id.equals(selectedCategoryId)) {
        selectedName = name;
      }
    }
    actCategory.setAdapter(new ArrayAdapter<>(this,
      android.R.layout.simple_list_item_1, labels));

    if (selectedName == null) {
      selectedCategoryId = null;
      actCategory.setText("", false);
    } else {
      actCategory.setText(selectedName, false);
    }

    boolean none = categories.isEmpty();
    layoutCategory.setVisibility(none ? View.GONE : View.VISIBLE);
    tvNoCategory.setVisibility(none ? View.VISIBLE : View.GONE);
    btnGoCategory.setVisibility(none ? View.VISIBLE : View.GONE);
  }

  private void trySave() {
    if (isSubmitting) {
      return; // cegah tombol ditekan berulang
    }
    boolean isNew = productId == null;
    if (!isNew && editing == null) {
      showError("Data barang belum selesai dimuat.");
      return;
    }

    clearErrors();
    String codeRaw = text(etCode);
    String name = text(etName);
    String buyText = text(etBuy);
    String sellText = text(etSell);
    String stockText = text(etStock);
    String minText = text(etMinStock);

    boolean ok = true;
    if (codeRaw.isEmpty()) {
      layoutCode.setError("Kode barang wajib diisi");
      ok = false;
    }
    if (name.isEmpty()) {
      layoutName.setError("Nama barang wajib diisi");
      ok = false;
    }
    if (selectedCategoryId == null) {
      layoutCategory.setError("Pilih kategori");
      ok = false;
    }
    if (buyText.isEmpty()) {
      layoutBuy.setError("Harga beli wajib diisi");
      ok = false;
    }
    if (sellText.isEmpty()) {
      layoutSell.setError("Harga jual wajib diisi");
      ok = false;
    }
    if (isNew && stockText.isEmpty()) {
      layoutStock.setError("Stok awal wajib diisi (isi 0 jika kosong)");
      ok = false;
    }
    if (minText.isEmpty()) {
      layoutMinStock.setError("Stok minimum wajib diisi");
      ok = false;
    }
    if (!ok) {
      return;
    }

    ProductInput in = new ProductInput();
    in.code = Validator.normalizeCode(codeRaw);
    in.name = name;
    in.categoryId = selectedCategoryId;
    in.buyPrice = RupiahFormat.parse(buyText);
    in.sellPrice = RupiahFormat.parse(sellText);
    in.stock = isNew ? (int) RupiahFormat.parse(stockText) : editing.stock;
    in.minStock = (int) RupiahFormat.parse(minText);

    String problem = Validator.validateProduct(in, isNew);
    if (problem != null) {
      showError(problem);
      return;
    }
    if (!NetworkUtil.isOnline(this)) {
      showError(NetworkUtil.PESAN_TIDAK_ADA_INTERNET);
      return;
    }

    if (in.sellPrice < in.buyPrice) {
      new MaterialAlertDialogBuilder(this)
        .setTitle("Harga jual di bawah harga beli")
        .setMessage("Barang ini akan rugi setiap kali terjual. Tetap simpan?")
        .setNegativeButton("Periksa lagi", null)
        .setPositiveButton("Tetap simpan", (dialog, which) -> save(in, isNew))
        .show();
    } else {
      save(in, isNew);
    }
  }

  private void save(ProductInput in, boolean isNew) {
    setLoading(true);
    DataCallback<Void> callback = new DataCallback<Void>() {
      @Override
      public void onSuccess(Void result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        Toast.makeText(ProductFormActivity.this,
          "Barang disimpan.", Toast.LENGTH_SHORT).show();
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
    };
    if (isNew) {
      productRepository.addProduct(in, callback);
    } else {
      productRepository.updateProduct(productId, in, callback);
    }
  }

  private void confirmToggleActive() {
    if (editing == null || isSubmitting) {
      return;
    }
    boolean newState = !editing.isActive;
    new MaterialAlertDialogBuilder(this)
      .setTitle(newState ? "Aktifkan barang" : "Nonaktifkan barang")
      .setMessage(newState
        ? "Barang akan muncul lagi di daftar dan bisa dijual."
        : "Barang tidak akan muncul di daftar penjualan. Riwayat transaksi tetap tersimpan.")
      .setNegativeButton("Batal", null)
      .setPositiveButton("Ya", (dialog, which) -> {
        setLoading(true);
        productRepository.setActive(productId, newState, new DataCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            if (isFinishing() || isDestroyed()) {
              return;
            }
            Toast.makeText(ProductFormActivity.this,
              "Status barang diperbarui.", Toast.LENGTH_SHORT).show();
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
      })
      .show();
  }

  private String text(TextInputEditText et) {
    return et.getText() == null ? "" : et.getText().toString().trim();
  }

  private void clearErrors() {
    layoutCode.setError(null);
    layoutName.setError(null);
    layoutCategory.setError(null);
    layoutBuy.setError(null);
    layoutSell.setError(null);
    layoutStock.setError(null);
    layoutMinStock.setError(null);
    tvError.setVisibility(View.GONE);
  }

  private void setLoading(boolean loading) {
    isSubmitting = loading;
    btnSave.setEnabled(!loading);
    btnToggleActive.setEnabled(!loading);
    btnAdjustStock.setEnabled(!loading);
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
  }

  private void showError(String message) {
    tvError.setText(message);
    tvError.setVisibility(View.VISIBLE);
  }
}
