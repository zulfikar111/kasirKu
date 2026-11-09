package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/** Catatan perubahan stok. Hanya ditambah, tidak pernah diubah atau dihapus. */
public class StockMovement {
  @DocumentId
  public String id;
  public String productId;
  public String productName;
  public int changeQty;          // positif = masuk, negatif = keluar
  public int stockBefore;
  public int stockAfter;
  public String type;            // Constants.MovementType.*
  public String reason;
  public String referenceSaleId; // terisi untuk PENJUALAN dan PEMBATALAN
  public String userId;
  public String userName;
  @ServerTimestamp
  public Date createdAt;

  public StockMovement() {
  }
}
