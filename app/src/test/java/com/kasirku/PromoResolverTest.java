package com.kasirku;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.kasirku.model.Product;
import com.kasirku.model.Promotion;
import com.kasirku.util.Constants;
import com.kasirku.util.PromoResolver;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PromoResolverTest {

  private final Date start = new Date(1_000_000L);
  private final Date end = new Date(2_000_000L);
  private final Date duringPromo = new Date(1_500_000L);
  private final Date afterPromo = new Date(3_000_000L);

  private Product product(String id, String categoryId, long price) {
    Product p = new Product();
    p.id = id;
    p.categoryId = categoryId;
    p.sellPrice = price;
    return p;
  }

  private Promotion promo(String name, String type, long value, String scope) {
    Promotion p = new Promotion();
    p.id = name;
    p.name = name;
    p.discountType = type;
    p.discountValue = value;
    p.scope = scope;
    p.startDate = start;
    p.endDate = end;
    return p;
  }

  @Test
  public void persen_dibulatkanKeBawah() {
    Promotion p = promo("P", Constants.DiscountType.PERSEN, 10, Constants.PromoScope.SEMUA);
    assertEquals(1000, p.discountPerUnit(10000));

    Promotion p15 = promo("P15", Constants.DiscountType.PERSEN, 15, Constants.PromoScope.SEMUA);
    assertEquals(299, p15.discountPerUnit(1999)); // 299,85 dibulatkan ke bawah
  }

  @Test
  public void nominal_tidakMelebihiHarga() {
    Promotion p = promo("P", Constants.DiscountType.NOMINAL, 2000, Constants.PromoScope.SEMUA);
    assertEquals(1500, p.discountPerUnit(1500));
    assertEquals(2000, p.discountPerUnit(10000));
  }

  @Test
  public void duaPromo_dipakaiYangPalingBesar() {
    Product product = product("b1", "c1", 10000);
    List<Promotion> promos = new ArrayList<>();
    promos.add(promo("A-persen10", Constants.DiscountType.PERSEN, 10, Constants.PromoScope.SEMUA));
    promos.add(promo("B-nominal2500", Constants.DiscountType.NOMINAL, 2500, Constants.PromoScope.SEMUA));

    Promotion best = PromoResolver.findBest(product, promos, duringPromo);
    assertNotNull(best);
    assertEquals("B-nominal2500", best.name);
  }

  @Test
  public void promoDiLuarTanggal_atauNonaktif_diabaikan() {
    Product product = product("b1", "c1", 10000);
    Promotion expired = promo("E", Constants.DiscountType.PERSEN, 50, Constants.PromoScope.SEMUA);
    assertNull(PromoResolver.findBest(product, Arrays.asList(expired), afterPromo));

    Promotion off = promo("O", Constants.DiscountType.PERSEN, 50, Constants.PromoScope.SEMUA);
    off.isActive = false;
    assertNull(PromoResolver.findBest(product, Arrays.asList(off), duringPromo));
  }

  @Test
  public void cakupanKategoriDanBarang() {
    Product inCategory = product("b1", "c1", 10000);
    Product otherCategory = product("b2", "c2", 10000);

    Promotion byCategory = promo("K", Constants.DiscountType.PERSEN, 10, Constants.PromoScope.KATEGORI);
    byCategory.categoryIds = Arrays.asList("c1");
    assertNotNull(PromoResolver.findBest(inCategory, Arrays.asList(byCategory), duringPromo));
    assertNull(PromoResolver.findBest(otherCategory, Arrays.asList(byCategory), duringPromo));

    Promotion byProduct = promo("B", Constants.DiscountType.PERSEN, 10, Constants.PromoScope.BARANG);
    byProduct.productIds = Arrays.asList("b2");
    assertNull(PromoResolver.findBest(inCategory, Arrays.asList(byProduct), duringPromo));
    assertNotNull(PromoResolver.findBest(otherCategory, Arrays.asList(byProduct), duringPromo));
  }
}
