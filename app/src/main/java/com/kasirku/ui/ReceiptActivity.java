package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.kasirku.R;
import com.kasirku.data.DataCallback;
import com.kasirku.data.SaleRepository;
import com.kasirku.model.Sale;
import com.kasirku.util.Constants;
import com.kasirku.util.ReceiptFormatter;
import com.kasirku.util.ViewUtil;

/** Menampilkan struk dan membagikannya sebagai teks. */
public class ReceiptActivity extends BaseActivity {

  public static final String EXTRA_SALE_ID = "sale_id";

  private final SaleRepository saleRepository = new SaleRepository();

  private String saleId;
  private String receiptText = "";

  private ProgressBar progress;
  private TextView tvReceipt, tvError;
  private MaterialButton btnShare, btnRetry;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!accessGranted) {
      return;
    }
    saleId = getIntent().getStringExtra(EXTRA_SALE_ID);
    if (saleId == null) {
      Toast.makeText(this, "Transaksi tidak ditemukan.", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    setContentView(R.layout.activity_receipt);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> finish());

    progress = findViewById(R.id.progress);
    tvReceipt = findViewById(R.id.tvReceipt);
    tvError = findViewById(R.id.tvError);
    btnShare = findViewById(R.id.btnShare);
    btnRetry = findViewById(R.id.btnRetry);
    MaterialButton btnDone = findViewById(R.id.btnDone);

    btnDone.setOnClickListener(v -> finish());
    btnRetry.setOnClickListener(v -> load());
    btnShare.setOnClickListener(v -> {
      Intent send = new Intent(Intent.ACTION_SEND);
      send.setType("text/plain");
      send.putExtra(Intent.EXTRA_TEXT, receiptText);
      startActivity(Intent.createChooser(send, "Bagikan struk"));
    });

    load();
  }

  private void load() {
    progress.setVisibility(View.VISIBLE);
    tvError.setVisibility(View.GONE);
    btnRetry.setVisibility(View.GONE);
    btnShare.setEnabled(false);

    saleRepository.getById(saleId, new DataCallback<Sale>() {
      @Override
      public void onSuccess(Sale sale) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        progress.setVisibility(View.GONE);
        receiptText = ReceiptFormatter.build(sale, Constants.STORE_NAME);
        tvReceipt.setText(receiptText);
        btnShare.setEnabled(true);
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        progress.setVisibility(View.GONE);
        tvError.setText(message + " Transaksi sudah tersimpan; struk bisa dibuka lagi dari Riwayat.");
        tvError.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);
      }
    });
  }
}
