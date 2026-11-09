package com.kasirku.ui;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kasirku.R;
import com.kasirku.data.DataCallback;
import com.kasirku.data.ProductRepository;
import com.kasirku.data.StockRepository;
import com.kasirku.model.Product;
import com.kasirku.model.StockMovement;
import com.kasirku.util.Constants;
import com.kasirku.util.DateUtil;
import com.kasirku.util.NetworkUtil;
import com.kasirku.util.RupiahFormat;
import com.kasirku.util.SimpleTextWatcher;
import com.kasirku.util.Validator;
import com.kasirku.util.ViewUtil;

import java.util.List;

/** Penyesuaian stok (tambah/kurangi) dengan alasan wajib, plus riwayat perubahan stok. */
public class StockAdjustActivity extends BaseActivity {

  public static final String EXTRA_PRODUCT_ID = "product_id";

  private static final int MAX_HISTORY_SHOWN = 50;

  private static final String[] ADD_REASONS = {
    "Barang masuk dari supplier", "Koreksi hasil hitung stok"};
  private static final String[] REDUCE_REASONS = {
    "Barang rusak", "Barang hilang", "Barang kedaluwarsa", "Koreksi hasil hitung stok"};

  private final ProductRepository productRepository = new ProductRepository();
  private final StockRepository stockRepository = new StockRepository();

  private String productId;
  private Product product;
  private boolean isSubmitting = false;

  private TextView tvProductName, tvProductMeta, tvCurrentStock, tvPreview, tvError,
    tvHistoryEmpty;
  private MaterialButtonToggleGroup toggleGroup;
  private TextInputLayout layoutQty, layoutReason;
  private TextInputEditText etQty, etReason;
  private ChipGroup chipReasons;
  private MaterialButton btnSave;
  private ProgressBar progress;
  private LinearLayout historyContainer;

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
    productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
    if (productId == null) {
      Toast.makeText(this, "Barang tidak ditemukan.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    setContentView(R.layout.activity_stock_adjust);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> finish());

    tvProductName = findViewById(R.id.tvProductName);
    tvProductMeta = findViewById(R.id.tvProductMeta);
    tvCurrentStock = findViewById(R.id.tvCurrentStock);
    tvPreview = findViewById(R.id.tvPreview);
    tvError = findViewById(R.id.tvError);
    tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
    toggleGroup = findViewById(R.id.toggleGroup);
    layoutQty = findViewById(R.id.layoutQty);
    layoutReason = findViewById(R.id.layoutReason);
    etQty = findViewById(R.id.etQty);
    etReason = findViewById(R.id.etReason);
    chipReasons = findViewById(R.id.chipReasons);
    btnSave = findViewById(R.id.btnSave);
    progress = findViewById(R.id.progress);
    historyContainer = findViewById(R.id.historyContainer);

    toggleGroup.check(R.id.btnAdd);
    toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (isChecked) {
        buildReasonChips();
        updatePreview();
      }
    });
    etQty.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        updatePreview();
      }
    });
    btnSave.setOnClickListener(v -> trySave());

    buildReasonChips();
    loadProduct();
    loadHistory();
  }

  private boolean isAddMode() {
    return toggleGroup.getCheckedButtonId() == R.id.btnAdd;
  }

  private void buildReasonChips() {
    chipReasons.removeAllViews();
    String[] reasons = isAddMode() ? ADD_REASONS : REDUCE_REASONS;
    for (String reason : reasons) {
      Chip chip = new Chip(this);
      chip.setText(reason);
      chip.setOnClickListener(v -> {
        etReason.setText(reason);
        etReason.setSelection(reason.length());
      });
      chipReasons.addView(chip);
    }
  }

  private void loadProduct() {
    productRepository.getById(productId, new DataCallback<Product>() {
      @Override
      public void onSuccess(Product p) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        product = p;
        tvProductName.setText(p.name == null ? "" : p.name);
        tvProductMeta.setText("Kode: " + (p.code == null ? "-" : p.code));
        tvCurrentStock.setText("Stok sekarang: " + p.stock + " pcs");
        updatePreview();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        Toast.makeText(StockAdjustActivity.this, message, Toast.LENGTH_LONG).show();
        finish();
      }
    });
  }

  private void loadHistory() {
    tvHistoryEmpty.setText("Memuat riwayat...");
    tvHistoryEmpty.setVisibility(View.VISIBLE);
    stockRepository.loadMovements(productId, new DataCallback<List<StockMovement>>() {
      @Override
      public void onSuccess(List<StockMovement> result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        renderHistory(result);
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        tvHistoryEmpty.setText("Riwayat gagal dimuat. " + message);
        tvHistoryEmpty.setVisibility(View.VISIBLE);
      }
    });
  }

  private void renderHistory(List<StockMovement> list) {
    historyContainer.removeAllViews();
    if (list.isEmpty()) {
      tvHistoryEmpty.setText("Belum ada riwayat perubahan stok.");
      tvHistoryEmpty.setVisibility(View.VISIBLE);
      return;
    }
    tvHistoryEmpty.setVisibility(View.GONE);

    int shown = Math.min(list.size(), MAX_HISTORY_SHOWN);
    for (int i = 0; i < shown; i++) {
      StockMovement m = list.get(i);
      View row = getLayoutInflater().inflate(R.layout.item_stock_movement,
        historyContainer, false);
      TextView tvType = row.findViewById(R.id.tvMoveType);
      TextView tvReason = row.findViewById(R.id.tvMoveReason);
      TextView tvMeta = row.findViewById(R.id.tvMoveMeta);
      TextView tvQty = row.findViewById(R.id.tvMoveQty);
      TextView tvStock = row.findViewById(R.id.tvMoveStock);

      tvType.setText(typeLabel(m.type));
      tvReason.setText(m.reason == null ? "" : m.reason);
      String when = m.createdAt == null ? "Menyimpan..." : DateUtil.formatDateTime(m.createdAt);
      tvMeta.setText(when + " \u2022 " + (m.userName == null ? "" : m.userName));
      tvQty.setText(m.changeQty > 0 ? "+" + m.changeQty : String.valueOf(m.changeQty));
      tvQty.setTextColor(ContextCompat.getColor(this,
        m.changeQty >= 0 ? R.color.success : R.color.error));
      tvStock.setText(m.stockBefore + " \u2192 " + m.stockAfter);
      historyContainer.addView(row);
    }

    if (list.size() > shown) {
      TextView more = new TextView(this);
      more.setText("Menampilkan " + shown + " riwayat terbaru dari " + list.size() + ".");
      more.setTextSize(12);
      more.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
      historyContainer.addView(more);
    }
  }

  private String typeLabel(String type) {
    if (Constants.MovementType.STOK_AWAL.equals(type)) {
      return "Stok awal";
    }
    if (Constants.MovementType.PENJUALAN.equals(type)) {
      return "Penjualan";
    }
    if (Constants.MovementType.PEMBATALAN.equals(type)) {
      return "Pembatalan transaksi";
    }
    return "Penyesuaian";
  }

  private void updatePreview() {
    if (product == null) {
      tvPreview.setText("");
      return;
    }
    long qty = RupiahFormat.parse(text(etQty));
    if (qty == 0) {
      tvPreview.setText("Masukkan jumlah untuk melihat stok sesudah penyesuaian.");
      tvPreview.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
      return;
    }
    long after = product.stock + (isAddMode() ? qty : -qty);
    if (after < 0) {
      tvPreview.setText("Stok tidak cukup. Stok sekarang: " + product.stock + " pcs.");
      tvPreview.setTextColor(ContextCompat.getColor(this, R.color.error));
    } else {
      tvPreview.setText("Stok sesudah: " + after + " pcs");
      tvPreview.setTextColor(ContextCompat.getColor(this, R.color.green_dark));
    }
  }

  private void trySave() {
    if (isSubmitting) {
      return; // cegah tombol ditekan berulang
    }
    if (product == null) {
      showError("Data barang belum selesai dimuat.");
      return;
    }

    layoutQty.setError(null);
    layoutReason.setError(null);
    tvError.setVisibility(View.GONE);

    long qty = RupiahFormat.parse(text(etQty));
    String reason = text(etReason);

    boolean ok = true;
    if (qty <= 0) {
      layoutQty.setError("Jumlah wajib diisi dan lebih dari 0");
      ok = false;
    } else if (qty > Validator.MAX_STOCK) {
      layoutQty.setError("Jumlah terlalu besar");
      ok = false;
    }
    if (reason.length() < 3) {
      layoutReason.setError("Alasan wajib diisi (minimal 3 karakter)");
      ok = false;
    }
    if (!ok) {
      return;
    }

    int delta = (int) (isAddMode() ? qty : -qty);
    String problem = Validator.validateAdjustmentResult(product.stock, delta);
    if (problem != null) {
      showError(problem);
      return;
    }
    if (!NetworkUtil.isOnline(this)) {
      showError(NetworkUtil.PESAN_TIDAK_ADA_INTERNET);
      return;
    }

    int after = product.stock + delta;
    String signed = delta > 0 ? "+" + delta : String.valueOf(delta);
    new MaterialAlertDialogBuilder(this)
      .setTitle("Simpan penyesuaian stok?")
      .setMessage(product.name + "\nStok: " + product.stock + " \u2192 " + after
        + " (" + signed + ")\nAlasan: " + reason)
      .setNegativeButton("Batal", null)
      .setPositiveButton("Simpan", (dialog, which) -> save(delta, reason))
      .show();
  }

  private void save(int delta, String reason) {
    setLoading(true);
    stockRepository.adjustStock(productId, delta, reason, new DataCallback<Integer>() {
      @Override
      public void onSuccess(Integer newStock) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        setLoading(false);
        Toast.makeText(StockAdjustActivity.this,
          "Stok diperbarui: " + newStock + " pcs.", Toast.LENGTH_SHORT).show();
        etQty.setText("");
        etReason.setText("");
        loadProduct();
        loadHistory();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        setLoading(false);
        showError(message);
        loadProduct(); // stok mungkin sudah diubah dari HP lain
      }
    });
  }

  private String text(TextInputEditText et) {
    return et.getText() == null ? "" : et.getText().toString().trim();
  }

  private void setLoading(boolean loading) {
    isSubmitting = loading;
    btnSave.setEnabled(!loading);
    progress.setVisibility(loading ? View.VISIBLE : View.GONE);
  }

  private void showError(String message) {
    tvError.setText(message);
    tvError.setVisibility(View.VISIBLE);
  }
}
