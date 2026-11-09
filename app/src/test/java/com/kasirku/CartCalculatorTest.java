package com.kasirku;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.kasirku.model.CartLine;
import com.kasirku.model.Product;
import com.kasirku.model.Promotion;
import com.kasirku.util.CartCalculator;
import com.kasirku.util.Constants;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CartCalculatorTest {

  private final Date now = new Date(1_500_000L);

  private Product product(String id, String name, long sell, long buy, int stock) {
    Product p = new Product();
    p.id = id;
    p.code = id.toUpperCase();
    p.name = name;
    p.sellPrice = sell;
    p.buyPrice = buy;
    p.stock = stock;
    p.categoryId = "k1";
    return p;
  }

  private Promotion promoPersenUntuk(String productId, long persen) {
    Promotion p = new Promotion();
    p.id = "promo1";
    p.name = "Ramadan";
    p.discountType = Constants.DiscountType.PERSEN;
    p.discountValue = persen;
    p.startDate = new Date(1_000_000L);
    p.endDate = new Date(2_000_000L);
    p.scope = Constants.PromoScope.BARANG;
    p.productIds = Arrays.asList(productId);
    return p;
  }

  @Test
  public void barisTanpaPromo() {
    Product mie = product("a", "Mie Instan", 3000, 2000, 10);
    CartLine line = CartCalculator.buildLine(mie, 2, null);
    assertEquals(3000, line.unitPrice);
    assertEquals(0, line.discountPerUnit);
    assertEquals(6000, line.subtotal());
    assertNull(line.promotionId);
  }

  @Test
  public void barisDenganPromoPersen() {
    Product mie = product("a", "Mie Instan", 3000, 2000, 10);
    CartLine line = CartCalculator.buildLine(mie, 2, promoPersenUntuk("a", 10));
    assertEquals(300, line.discountPerUnit);
    assertEquals(2700, line.unitPrice);
    assertEquals(5400, line.subtotal());
    assertEquals(6000, line.grossSubtotal());
    assertEquals(600, line.discountTotal());
    assertEquals(4000, line.costTotal());
    assertEquals("promo1", line.promotionId);
    assertEquals("Ramadan", line.promotionName);
  }

  @Test
  public void totalDanRingkasan() {
    Product mie = product("a", "Mie Instan", 3000, 2000, 10);
    Product air = product("b", "Air Mineral", 5000, 4000, 3);
    List<CartLine> lines = Arrays.asList(
      CartCalculator.buildLine(mie, 2, promoPersenUntuk("a", 10)),
      CartCalculator.buildLine(air, 1, null));

    assertEquals(11000, CartCalculator.grossTotal(lines));
    assertEquals(600, CartCalculator.discountTotal(lines));
    assertEquals(10400, CartCalculator.total(lines));
    assertEquals(8000, CartCalculator.costTotal(lines));
    assertEquals(3, CartCalculator.itemCount(lines));
  }

  @Test
  public void buildLines_melewatiBarangHilangDanNonaktif() {
    Product mie = product("a", "Mie Instan", 3000, 2000, 10);
    Product air = product("b", "Air Mineral", 5000, 4000, 3);
    Product mati = product("d", "Barang Mati", 1000, 500, 5);
    mati.isActive = false;

    Map<String, Product> products = new HashMap<>();
    products.put("a", mie);
    products.put("b", air);
    products.put("d", mati);

    Map<String, Integer> cart = new LinkedHashMap<>();
    cart.put("a", 2);
    cart.put("b", 1);
    cart.put("x", 1); // tidak ada di database
    cart.put("d", 1); // nonaktif

    List<CartLine> lines = CartCalculator.buildLines(cart, products,
      Arrays.asList(promoPersenUntuk("a", 10)), now);
    assertEquals(2, lines.size());
    assertEquals(10400, CartCalculator.total(lines));
  }

  @Test
  public void masalahStok() {
    Product air = product("b", "Air Mineral", 5000, 4000, 3);
    assertNotNull(CartCalculator.findStockProblem(
      Arrays.asList(CartCalculator.buildLine(air, 5, null))));
    assertNull(CartCalculator.findStockProblem(
      Arrays.asList(CartCalculator.buildLine(air, 3, null))));
  }

  @Test
  public void pembayaranDanKembalian() {
    assertNotNull(CartCalculator.validatePayment(10400, 10399)); // kurang ditolak
    assertNull(CartCalculator.validatePayment(10400, 10400));    // pas
    assertNull(CartCalculator.validatePayment(10400, 20000));    // lebih
    assertEquals(9600, CartCalculator.change(10400, 20000));
    assertEquals(0, CartCalculator.change(10400, 10400));
  }
}
