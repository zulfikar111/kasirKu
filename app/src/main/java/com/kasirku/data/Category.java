package com.kasirku.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Category {
  @DocumentId
  public String id;
  public String name;
  @ServerTimestamp
  public Date createdAt;

  public Category() {
  }
}
