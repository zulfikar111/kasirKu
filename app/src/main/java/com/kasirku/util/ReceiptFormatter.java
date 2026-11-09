package com.kasirku.util;

import com.kasirku.model.Sale;
import com.kasirku.model.SaleItem;

/** Membuat teks struk. Teks yang sama dipakai untuk tampilan dan untuk dibagikan. */
public final class ReceiptFormatter {

  private static final String LINE = "--------------------------------";

  private ReceiptFormatter() {
  }

  public static String build(Sale sale, String storeName) {
    StringBuilder sb = new StringBuilder();
    sb.append(storeName).append('\n');
    if (sale.isCancelled()) {
      sb.append("*** TRANSAKSI DIBATALKAN ***\n");
      if (sale.cancelReason != null && !sale.cancelReason.isEmpty()) {
        sb.append("Alasan: ").append(sale.cancelReason).append('\n');
      }
    }
    sb.append("No: ").append(nz(sale.invoiceNo)).append('\n');
    if (sale.createdAt != null) {
      sb.append(DateUtil.formatDateTime(sale.createdAt)).append('\n');
    }
    sb.append("Kasir: ").append(nz(sale.cashierName)).append('\n');
    sb.append(LINE).append('\n');

    if (sale.items != null) {
      for (SaleItem item : sale.items) {
        sb.append(nz(item.productName)).append('\n');
        sb.append(item.quantity).append(" x ")
          .append(RupiahFormat.format(item.sellPrice))
          .append(" = ").append(RupiahFormat.format(item.subtotal)).append('\n');
        if (item.discountPerUnit > 0) {
          String promo = (item.promotionName == null || item.promotionName.isEmpty())
            ? "Diskon" : "Promo " + item.promotionName;
          sb.append("  ").append(promo).append(": -")
            .append(RupiahFormat.format(item.discountPerUnit))
            .append("/pcs\n");
        }
      }
    }

    sb.append(LINE).append('\n');
    sb.append("Subtotal: ").append(RupiahFormat.format(sale.grossTotal)).append('\n');
    if (sale.discountTotal > 0) {
      sb.append("Diskon: -").append(RupiahFormat.format(sale.discountTotal)).append('\n');
    }
    sb.append("TOTAL: ").append(RupiahFormat.format(sale.total)).append('\n');
    sb.append("Tunai: ").append(RupiahFormat.format(sale.paidAmount)).append('\n');
    sb.append("Kembalian: ").append(RupiahFormat.format(sale.changeAmount)).append('\n');
    sb.append(LINE).append('\n');
    sb.append("Terima kasih atas kunjungan Anda");
    return sb.toString();
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }
}
