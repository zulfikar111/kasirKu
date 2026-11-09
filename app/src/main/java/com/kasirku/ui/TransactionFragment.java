package com.kasirku.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.kasirku.R;
import com.kasirku.adapter.SaleProductAdapter;
import com.kasirku.data.DataCallback;
import com.kasirku.data.ProductRepository;
import com.kasirku.data.PromotionRepository;
import com.kasirku.model.CartLine;
import com.kasirku.model.Product;
import com.kasirku.model.Promotion;
import com.kasirku.session.CartManager;
import com.kasirku.session.SessionManager;
import com.kasirku.util.CartCalculator;
import com.kasirku.util.PromoResolver;
import com.kasirku.util.RupiahFormat;
import com.kasirku.util.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Layar Transaksi: pilih barang, isi keranjang, lanjut ke pembayaran. */
public class TransactionFragment extends Fragment {

  private final ProductRepository productRepository = new ProductRepository();
  private final PromotionRepository promotionRepository = new PromotionRepository();

  private final List<Product> allProducts = new ArrayList<>();
  private final Map<String, Product> productMap = new HashMap<>();
  private List<Promotion> promotions = new ArrayList<>();
  private boolean productsLoaded = false;
  private ListenerRegistration registration;

  private TextInputEditText etSearch;
  private ProgressBar progress;
  private TextView tvEmpty;
  private TextView tvCartSummary;
  private TextView tvCartTotal;
  private View cardCart;
  private SaleProductAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_transaction, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    etSearch = view.findViewById(R.id.etSearch);
    progress = view.findViewById(R.id.progress);
    tvEmpty = view.findViewById(R.id.tvEmpty);
    tvCartSummary = view.findViewById(R.id.tvCartSummary);
    tvCartTotal = view.findViewById(R.id.tvCartTotal);
    cardCart = view.findViewById(R.id.cardCart);
    MaterialButton btnCheckout = view.findViewById(R.id.btnCheckout);
    RecyclerView rv = view.findViewById(R.id.rvProducts);

    adapter = new SaleProductAdapter(this::addToCart);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    rv.setAdapter(adapter);

    etSearch.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        refresh();
      }
    });

    btnCheckout.setOnClickListener(v ->
      startActivity(new Intent(requireContext(), CheckoutActivity.class)));

    registration = productRepository.listen(new DataCallback<List<Product>>() {
      @Override
      public void onSuccess(List<Product> result) {
        if (getView() == null) {
          return;
        }
        allProducts.clear();
        allProducts.addAll(result);
        productMap.clear();
        for (Product p : result) {
          productMap.put(p.id, p);
        }
        productsLoaded = true;
        refresh();
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
    // Muat promo terbaru, dan segarkan keranjang (mungkin baru kembali dari layar bayar)
    promotionRepository.loadAll(new DataCallback<List<Promotion>>() {
      @Override
      public void onSuccess(List<Promotion> result) {
        if (!isAdded() || getView() == null) {
          return;
        }
        promotions = result;
        refresh();
      }

      @Override
      public void onError(String message) {
        if (isAdded() && getView() != null) {
          Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
      }
    });
    refresh();
  }

  private void addToCart(Product tapped) {
    Product latest = productMap.get(tapped.id);
    String error = CartManager.add(latest != null ? latest : tapped);
    if (error != null) {
      Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }
    refresh();
  }

  private void refresh() {
    if (adapter == null || getView() == null) {
      return;
    }
    Date now = new Date();
    String query = etSearch.getText() == null
      ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);

    List<SaleProductAdapter.Row> rows = new ArrayList<>();
    int activeCount = 0;
    for (Product p : allProducts) {
      if (!p.isActive) {
        continue;
      }
      activeCount++;
      if (!query.isEmpty()) {
        String name = p.name == null ? "" : p.name.toLowerCase(Locale.ROOT);
        String code = p.code == null ? "" : p.code.toLowerCase(Locale.ROOT);
        if (!name.contains(query) && !code.contains(query)) {
          continue;
        }
      }
      Promotion best = PromoResolver.findBest(p, promotions, now);
      CartLine one = CartCalculator.buildLine(p, 1, best);
      rows.add(new SaleProductAdapter.Row(p, one.originalPrice, one.unitPrice,
        one.promotionName, CartManager.quantityOf(p.id)));
    }
    adapter.setRows(rows);

    progress.setVisibility(productsLoaded ? View.GONE : View.VISIBLE);
    if (productsLoaded && rows.isEmpty()) {
      tvEmpty.setText(activeCount == 0
        ? "Belum ada barang yang bisa dijual.\nHubungi admin untuk menambah barang."
        : "Tidak ada barang yang cocok.");
      tvEmpty.setVisibility(View.VISIBLE);
    } else {
      tvEmpty.setVisibility(View.GONE);
    }

    List<CartLine> lines = CartCalculator.buildLines(
      CartManager.snapshot(), productMap, promotions, now);
    if (lines.isEmpty()) {
      cardCart.setVisibility(View.GONE);
    } else {
      cardCart.setVisibility(View.VISIBLE);
      tvCartSummary.setText(CartCalculator.itemCount(lines) + " barang di keranjang");
      tvCartTotal.setText(RupiahFormat.format(CartCalculator.total(lines)));
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
