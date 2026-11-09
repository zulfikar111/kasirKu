package com.kasirku.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.kasirku.R;
import com.kasirku.model.Product;
import com.kasirku.util.RupiahFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Daftar barang. Jika listener null, daftar hanya untuk dilihat (kasir). */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.Holder> {

  public interface OnProductClick {
    void onClick(Product product);
  }

  private final List<Product> items = new ArrayList<>();
  private final Map<String, String> categoryNames = new HashMap<>();
  private final OnProductClick listener;

  public ProductAdapter(OnProductClick listener) {
    this.listener = listener;
  }

  public void setItems(List<Product> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
  }

  public void setCategoryNames(Map<String, String> names) {
    categoryNames.clear();
    categoryNames.putAll(names);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.item_product, parent, false);
    return new Holder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    Product p = items.get(position);
    String category = categoryNames.get(p.categoryId);
    if (category == null) {
      category = "Tanpa kategori";
    }

    h.tvName.setText(p.name == null ? "" : p.name);
    h.tvMeta.setText((p.code == null ? "" : p.code) + " \u2022 " + category);
    h.tvPrice.setText(RupiahFormat.format(p.sellPrice));
    h.tvInactive.setVisibility(p.isActive ? View.GONE : View.VISIBLE);
    h.card.setAlpha(p.isActive ? 1f : 0.6f);

    int colorRes;
    String stockText;
    if (p.isOutOfStock()) {
      colorRes = R.color.error;
      stockText = "Stok habis";
    } else if (p.isLowStock()) {
      colorRes = R.color.warning;
      stockText = "Stok: " + p.stock + " (menipis)";
    } else {
      colorRes = R.color.success;
      stockText = "Stok: " + p.stock;
    }
    h.tvStock.setText(stockText);
    h.tvStock.setTextColor(ContextCompat.getColor(h.itemView.getContext(), colorRes));

    if (listener != null) {
      h.card.setClickable(true);
      h.card.setOnClickListener(v -> listener.onClick(p));
    } else {
      h.card.setClickable(false);
      h.card.setOnClickListener(null);
    }
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class Holder extends RecyclerView.ViewHolder {
    final MaterialCardView card;
    final TextView tvName;
    final TextView tvMeta;
    final TextView tvPrice;
    final TextView tvStock;
    final TextView tvInactive;

    Holder(@NonNull View itemView) {
      super(itemView);
      card = itemView.findViewById(R.id.card);
      tvName = itemView.findViewById(R.id.tvName);
      tvMeta = itemView.findViewById(R.id.tvMeta);
      tvPrice = itemView.findViewById(R.id.tvPrice);
      tvStock = itemView.findViewById(R.id.tvStock);
      tvInactive = itemView.findViewById(R.id.tvInactive);
    }
  }
}
