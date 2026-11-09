package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.kasirku.util.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Sale {
  @DocumentId
  public String id;
  public String invoiceNo;       // contoh: INV-20261015-0001
  public String cashierId;
  public String cashierName;
  public long grossTotal;        // harga normal x jumlah, sebelum diskon
  public long discountTotal;     // total diskon
  public long total;             // grossTotal - discountTotal (penjualan bersih)
  public long costTotal;         // total modal barang terjual
  public long paidAmount;
  public long changeAmount;
  public String status = Constants.SaleStatus.SELESAI;
  public List<SaleItem> items = new ArrayList<>();
  @ServerTimestamp
  public Date createdAt;

  // Terisi hanya jika transaksi dibatalkan
  public Date cancelledAt;
  public String cancelledBy;
  public String cancelledByName;
  public String cancelReason;

  public Sale() {
  }

  @Exclude
  public boolean isCancelled() {
    return Constants.SaleStatus.BATAL.equals(status);
  }

  /** Keuntungan kotor transaksi = penjualan bersih - modal. */
  @Exclude
  public long getGrossProfit() {
    return total - costTotal;
  }
}
