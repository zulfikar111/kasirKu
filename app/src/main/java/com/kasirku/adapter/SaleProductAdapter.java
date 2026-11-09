package com.kasirku.adapter;

import android.graphics.Paint;
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
import java.util.List;

/** Daftar barang untuk dijual (layar Transaksi). Ketuk barang untuk menambah ke keranjang. */
public class SaleProductAdapter extends RecyclerView.Adapter<SaleProductAdapter.Holder> {

  public static class Row {
    public final Product product;
    public final long originalPrice;
    public final long unitPrice;     // harga setelah diskon
    public final String promoName;   // null jika tanpa promo
    public final int inCart;

    public Row(Product product, long originalPrice, long unitPrice,
               String promoName, int inCart) {
      this.product = product;
      this.originalPrice = originalPrice;
      this.unitPrice = unitPrice;
      this.promoName = promoName;
      this.inCart = inCart;
    }
  }

  public interface Listener {
    void onAdd(Product product);
  }

  private final List<Row> rows = new ArrayList<>();
  private final Listener listener;

  public SaleProductAdapter(Listener listener) {
    this.listener = listener;
  }

  public void setRows(List<Row> newRows) {
    rows.clear();
    rows.addAll(newRows);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.item_sale_product, parent, false);
    return new Holder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    Row row = rows.get(position);
    Product p = row.product;

    h.tvName.setText(p.name == null ? "" : p.name);
    h.tvMeta.setText((p.code == null ? "" : p.code) + " \u2022 Stok " + p.stock);
    h.tvPrice.setText(RupiahFormat.format(row.unitPrice));

    if (row.unitPrice < row.originalPrice) {
      h.tvOriginal.setVisibility(View.VISIBLE);
      h.tvOriginal.setText(RupiahFormat.format(row.originalPrice));
      h.tvPromo.setVisibility(View.VISIBLE);
      h.tvPromo.setText("Promo " + (row.promoName == null ? "" : row.promoName));
    } else {
      h.tvOriginal.setVisibility(View.GONE);
      h.tvPromo.setVisibility(View.GONE);
    }

    boolean out = p.isOutOfStock();
    h.card.setAlpha(out ? 0.5f : 1f);
    if (out) {
      h.tvStatus.setVisibility(View.VISIBLE);
      h.tvStatus.setText("Stok habis");
      h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.error));
      h.card.setClickable(false);
      h.card.setOnClickListener(null);
    } else {
      if (row.inCart > 0) {
        h.tvStatus.setVisibility(View.VISIBLE);
        h.tvStatus.setText("Di keranjang: " + row.inCart);
        h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.green_dark));
      } else {
        h.tvStatus.setVisibility(View.GONE);
      }
      h.card.setClickable(true);
      h.card.setOnClickListener(v -> listener.onAdd(p));
    }
  }

  @Override
  public int getItemCount() {
    return rows.size();
  }

  static class Holder extends RecyclerView.ViewHolder {
    final MaterialCardView card;
    final TextView tvName;
    final TextView tvMeta;
    final TextView tvPromo;
    final TextView tvStatus;
    final TextView tvOriginal;
    final TextView tvPrice;

    Holder(@NonNull View itemView) {
      super(itemView);
      card = itemView.findViewById(R.id.card);
      tvName = itemView.findViewById(R.id.tvName);
      tvMeta = itemView.findViewById(R.id.tvMeta);
      tvPromo = itemView.findViewById(R.id.tvPromo);
      tvStatus = itemView.findViewById(R.id.tvStatus);
      tvOriginal = itemView.findViewById(R.id.tvOriginal);
      tvPrice = itemView.findViewById(R.id.tvPrice);
      // Harga asli dicoret
      tvOriginal.setPaintFlags(tvOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }
  }
}
