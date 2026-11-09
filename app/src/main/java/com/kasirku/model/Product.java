package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Product {
  @DocumentId
  public String id;
  public String code;
  public String name;
  public String categoryId;
  public long buyPrice;     // harga beli (modal), rupiah
  public long sellPrice;    // harga jual, rupiah
  public int stock;
  public int minStock;      // batas peringatan stok menipis
  public boolean isActive = true;
  @ServerTimestamp
  public Date createdAt;
  @ServerTimestamp
  public Date updatedAt;

  public Product() {
  }

  @Exclude
  public boolean isLowStock() {
    return stock <= minStock;
  }

  @Exclude
  public boolean isOutOfStock() {
    return stock <= 0;
  }
}
