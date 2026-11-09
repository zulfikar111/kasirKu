package com.kasirku.ui;

import android.content.Intent;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.kasirku.R;
import com.kasirku.data.DataCallback;
import com.kasirku.data.ProductRepository;
import com.kasirku.data.PromotionRepository;
import com.kasirku.data.SaleRepository;
import com.kasirku.model.CartLine;
import com.kasirku.model.Product;
import com.kasirku.model.Promotion;
import com.kasirku.session.CartManager;
import com.kasirku.util.CartCalculator;
import com.kasirku.util.NetworkUtil;
import com.kasirku.util.RupiahFormat;
import com.kasirku.util.SimpleTextWatcher;
import com.kasirku.util.ViewUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Keranjang dan pembayaran. Harga dan stok mengikuti database secara langsung. */
public class CheckoutActivity extends BaseActivity {

  private final ProductRepository productRepository = new ProductRepository();
  private final PromotionRepository promotionRepository = new PromotionRepository();
  private final SaleRepository saleRepository = new SaleRepository();

  private final Map<String, Product> productMap = new HashMap<>();
  private List<Promotion> promotions = new ArrayList<>();
  private List<CartLine> lines = new ArrayList<>();

  private boolean productsLoaded = false;
  private boolean promosLoaded = false;
  private boolean isSubmitting = false;
  private String pendingSaleId = null; // tetap sama selama isi keranjang tidak berubah
  private ListenerRegistration registration;

  private ProgressBar progress, progressSubmit;
  private TextView tvEmpty, tvGross, tvDiscount, tvTotal, tvChange, tvWarning, tvError;
  private View rowDiscount, cardTotals, cardPayment;
  private LinearLayout cartContainer;
  private TextInputEditText etPaid;
  private MaterialButton btnConfirm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!accessGranted) {
      return;
    }
    setContentView(R.layout.activity_checkout);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> finish());

    progress = findViewById(R.id.progress);
    progressSubmit = findViewById(R.id.progressSubmit);
    tvEmpty = findViewById(R.id.tvEmpty);
    tvGross = findViewById(R.id.tvGross);
    tvDiscount = findViewById(R.id.tvDiscount);
    tvTotal = findViewById(R.id.tvTotal);
    tvChange = findViewById(R.id.tvChange);
    tvWarning = findViewById(R.id.tvWarning);
    tvError = findViewById(R.id.tvError);
    rowDiscount = findViewById(R.id.rowDiscount);
    cardTotals = findViewById(R.id.cardTotals);
    cardPayment = findViewById(R.id.cardPayment);
    cartContainer = findViewById(R.id.cartContainer);
    etPaid = findViewById(R.id.etPaid);
    btnConfirm = findViewById(R.id.btnConfirm);

    etPaid.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        clearError();
        updatePayment();
      }
    });
    findViewById(R.id.btnPaidExact).setOnClickListener(
      v -> setPaid(CartCalculator.total(lines)));
    findViewById(R.id.btnPaid20).setOnClickListener(v -> setPaid(20000));
    findViewById(R.id.btnPaid50).setOnClickListener(v -> setPaid(50000));
    findViewById(R.id.btnPaid100).setOnClickListener(v -> setPaid(100000));
    btnConfirm.setOnClickListener(v -> confirm());

    progress.setVisibility(View.VISIBLE);
    cardTotals.setVisibility(View.GONE);
    cardPayment.setVisibility(View.GONE);

    registration = productRepository.listen(new DataCallback<List<Product>>() {
      @Override
      public void onSuccess(List<Product> result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        productMap.clear();
        for (Product p : result) {
          productMap.put(p.id, p);
        }
        productsLoaded = true;
        render();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        showLoadError(message);
      }
    });

    promotionRepository.loadAll(new DataCallback<List<Promotion>>() {
      @Override
      public void onSuccess(List<Promotion> result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        promotions = result;
        promosLoaded = true;
        render();
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        showLoadError(message + " Tutup lalu buka keranjang lagi.");
      }
    });
  }

  private void showLoadError(String message) {
    progress.setVisibility(View.GONE);
    tvEmpty.setText("Gagal memuat data.\n" + message);
    tvEmpty.setVisibility(View.VISIBLE);
  }

  /** Menggambar ulang keranjang dari data terbaru. */
  private void render() {
    if (!productsLoaded || !promosLoaded) {
      return;
    }
    progress.setVisibility(View.GONE);

    // Buang barang yang sudah tidak ada atau nonaktif
    boolean removed = false;
    for (String id : CartManager.snapshot().keySet()) {
      Product p = productMap.get(id);
      if (p == null || !p.isActive) {
        CartManager.remove(id);
        removed = true;
      }
    }
    if (removed) {
      Toast.makeText(this, "Barang yang tidak tersedia lagi dikeluarkan dari keranjang.",
        Toast.LENGTH_LONG).show();
    }

    lines = CartCalculator.buildLines(CartManager.snapshot(), productMap,
      promotions, new Date());

    cartContainer.removeAllViews();
    boolean empty = lines.isEmpty();
    tvEmpty.setText("Keranjang masih kosong.\nPilih barang di tab Transaksi.");
    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    cardTotals.setVisibility(empty ? View.GONE : View.VISIBLE);
    cardPayment.setVisibility(empty ? View.GONE : View.VISIBLE);

    for (CartLine line : lines) {
      cartContainer.addView(buildRow(line));
    }

    long discount = CartCalculator.discountTotal(lines);
    tvGross.setText(RupiahFormat.format(CartCalculator.grossTotal(lines)));
    rowDiscount.setVisibility(discount > 0 ? View.VISIBLE : View.GONE);
    tvDiscount.setText("-" + RupiahFormat.format(discount));
    tvTotal.setText(RupiahFormat.format(CartCalculator.total(lines)));

    updatePayment();
  }

  private View buildRow(CartLine line) {
    View row = getLayoutInflater().inflate(R.layout.item_cart_line, cartContainer, false);
    TextView tvName = row.findViewById(R.id.tvName);
    TextView tvPriceInfo = row.findViewById(R.id.tvPriceInfo);
    TextView tvPromo = row.findViewById(R.id.tvPromo);
    TextView tvStockWarn = row.findViewById(R.id.tvStockWarn);
    TextView tvQty = row.findViewById(R.id.tvQty);
    TextView tvSubtotal = row.findViewById(R.id.tvSubtotal);
    MaterialButton btnMinus = row.findViewById(R.id.btnMinus);
    MaterialButton btnPlus = row.findViewById(R.id.btnPlus);
    MaterialButton btnRemove = row.findViewById(R.id.btnRemove);

    tvName.setText(line.productName == null ? "" : line.productName);
    String priceInfo = RupiahFormat.format(line.unitPrice);
    if (line.discountPerUnit > 0) {
      priceInfo += " (normal " + RupiahFormat.format(line.originalPrice) + ")";
    }
    tvPriceInfo.setText(priceInfo + " x " + line.quantity);
    tvQty.setText(String.valueOf(line.quantity));
    tvSubtotal.setText(RupiahFormat.format(line.subtotal()));

    if (line.promotionName != null) {
      tvPromo.setVisibility(View.VISIBLE);
      tvPromo.setText("Promo " + line.promotionName + ": -"
        + RupiahFormat.format(line.discountPerUnit) + "/pcs");
    } else {
      tvPromo.setVisibility(View.GONE);
    }

    if (line.quantity > line.availableStock) {
      tvStockWarn.setVisibility(View.VISIBLE);
      tvStockWarn.setText("Stok tersisa " + line.availableStock + " pcs. Kurangi jumlahnya.");
    } else {
      tvStockWarn.setVisibility(View.GONE);
    }

    btnPlus.setEnabled(!isSubmitting && line.quantity < line.availableStock);
    btnMinus.setEnabled(!isSubmitting);
    btnRemove.setEnabled(!isSubmitting);

    btnPlus.setOnClickListener(v -> {
      CartManager.setQuantity(line.productId, line.quantity + 1);
      onCartChanged();
    });
    btnMinus.setOnClickListener(v -> {
      CartManager.setQuantity(line.productId, line.quantity - 1); // 0 = hapus
      onCartChanged();
    });
    btnRemove.setOnClickListener(v -> {
      CartManager.remove(line.productId);
      onCartChanged();
    });
    return row;
  }

  private void onCartChanged() {
    pendingSaleId = null; // isi keranjang berubah, jadi ini transaksi yang berbeda
    clearError();
    render();
  }

  private void setPaid(long amount) {
    etPaid.setText(String.valueOf(amount));
    etPaid.setSelection(etPaid.getText() == null ? 0 : etPaid.getText().length());
  }

  private void updatePayment() {
    long total = CartCalculator.total(lines);
    String paidText = etPaid.getText() == null ? "" : etPaid.getText().toString().trim();
    long paid = RupiahFormat.parse(paidText);
    String stockProblem = CartCalculator.findStockProblem(lines);

    if (stockProblem != null) {
      tvWarning.setText(stockProblem);
      tvWarning.setVisibility(View.VISIBLE);
    } else {
      tvWarning.setVisibility(View.GONE);
    }

    if (lines.isEmpty()) {
      tvChange.setText("");
    } else if (paidText.isEmpty()) {
      tvChange.setText("Masukkan uang yang diterima");
      tvChange.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    } else if (paid < total) {
      tvChange.setText("Kurang " + RupiahFormat.format(total - paid));
      tvChange.setTextColor(ContextCompat.getColor(this, R.color.error));
    } else {
      tvChange.setText("Kembalian: " + RupiahFormat.format(CartCalculator.change(total, paid)));
      tvChange.setTextColor(ContextCompat.getColor(this, R.color.green_dark));
    }

    boolean canPay = !lines.isEmpty() && stockProblem == null
      && !paidText.isEmpty() && paid >= total && !isSubmitting;
    btnConfirm.setEnabled(canPay);
  }

  private void confirm() {
    if (isSubmitting) {
      return; // cegah tombol ditekan berulang
    }
    clearError();

    long total = CartCalculator.total(lines);
    long paid = RupiahFormat.parse(etPaid.getText() == null ? "" : etPaid.getText().toString());

    if (lines.isEmpty()) {
      showError("Keranjang kosong.");
      return;
    }
    String stockProblem = CartCalculator.findStockProblem(lines);
    if (stockProblem != null) {
      showError(stockProblem);
      return;
    }
    String payProblem = CartCalculator.validatePayment(total, paid);
    if (payProblem != null) {
      showError(payProblem);
      return;
    }
    if (!NetworkUtil.isOnline(this)) {
      showError(NetworkUtil.PESAN_TIDAK_ADA_INTERNET);
      return;
    }

    new MaterialAlertDialogBuilder(this)
      .setTitle("Selesaikan transaksi?")
      .setMessage("Total: " + RupiahFormat.format(total)
        + "\nDiterima: " + RupiahFormat.format(paid)
        + "\nKembalian: " + RupiahFormat.format(CartCalculator.change(total, paid)))
      .setNegativeButton("Periksa lagi", null)
      .setPositiveButton("Selesaikan", (dialog, which) -> submit(total, paid))
      .show();
  }

  private void submit(long expectedTotal, long paid) {
    if (isSubmitting) {
      return;
    }
    setLoading(true);
    if (pendingSaleId == null) {
      pendingSaleId = saleRepository.newSaleId();
    }
    saleRepository.createSale(pendingSaleId, CartManager.snapshot(), paid, expectedTotal,
      new DataCallback<String>() {
        @Override
        public void onSuccess(String saleId) {
          // Transaksi sudah tersimpan: keranjang SELALU dikosongkan
          CartManager.clear();
          pendingSaleId = null;
          if (isFinishing() || isDestroyed()) {
            return;
          }
          Intent intent = new Intent(CheckoutActivity.this, ReceiptActivity.class);
          intent.putExtra(ReceiptActivity.EXTRA_SALE_ID, saleId);
          startActivity(intent);
          finish();
        }

        @Override
        public void onError(String message) {
          if (isFinishing() || isDestroyed()) {
            return;
          }
          setLoading(false);
          showError(message + " Transaksi belum disimpan.");
        }
      });
  }

  private void setLoading(boolean loading) {
    isSubmitting = loading;
    progressSubmit.setVisibility(loading ? View.VISIBLE : View.GONE);
    etPaid.setEnabled(!loading);
    render(); // menyegarkan status tombol di setiap baris
  }

  private void showError(String message) {
    tvError.setText(message);
    tvError.setVisibility(View.VISIBLE);
  }

  private void clearError() {
    tvError.setVisibility(View.GONE);
  }

  @Override
  protected void onDestroy() {
    if (registration != null) {
      registration.remove();
      registration = null;
    }
    super.onDestroy();
  }
}
