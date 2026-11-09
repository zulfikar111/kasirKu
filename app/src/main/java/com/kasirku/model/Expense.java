package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Expense {
  @DocumentId
  public String id;
  public Date expenseDate;       // tanggal pengeluaran (dipilih admin)
  public String category;        // misalnya Listrik, Transportasi
  public long amount;
  public String description;
  public String userId;
  public String userName;
  @ServerTimestamp
  public Date createdAt;         // waktu pencatatan

  public Expense() {
  }
}
