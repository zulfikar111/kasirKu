package com.kasirku;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.kasirku.model.ProductInput;
import com.kasirku.util.Validator;

import org.junit.Test;

public class ValidatorTest {

  private ProductInput valid() {
    ProductInput in = new ProductInput();
    in.code = "MIE-01";
    in.name = "Mie Instan";
    in.categoryId = "kat1";
    in.buyPrice = 2500;
    in.sellPrice = 3000;
    in.stock = 20;
    in.minStock = 5;
    return in;
  }

  @Test
  public void kodeDirapikanJadiHurufBesar() {
    assertEquals("MIE01", Validator.normalizeCode("  mie01 "));
    assertEquals("", Validator.normalizeCode(null));
  }

  @Test
  public void kodeValid() {
    assertTrue(Validator.isValidCode("mie01"));
    assertTrue(Validator.isValidCode("A"));
    assertTrue(Validator.isValidCode("AB.12_x-3"));
  }

  @Test
  public void kodeTidakValid() {
    assertFalse(Validator.isValidCode(""));
    assertFalse(Validator.isValidCode(null));
    assertFalse(Validator.isValidCode("-ABC"));       // tidak boleh diawali tanda
    assertFalse(Validator.isValidCode("A/B"));        // garis miring dilarang
    assertFalse(Validator.isValidCode("A B"));        // spasi dilarang
    assertFalse(Validator.isValidCode("A1234567890123456789012345678901")); // 32 karakter
  }

  @Test
  public void dataBarangValid() {
    assertNull(Validator.validateProduct(valid(), true));
    assertNull(Validator.validateProduct(valid(), false));
  }

  @Test
  public void dataBarangTidakValid() {
    ProductInput in = valid();
    in.name = "  ";
    assertNotNull(Validator.validateProduct(in, true));

    in = valid();
    in.sellPrice = 0;
    assertNotNull(Validator.validateProduct(in, true));

    in = valid();
    in.buyPrice = -1;
    assertNotNull(Validator.validateProduct(in, true));

    in = valid();
    in.categoryId = null;
    assertNotNull(Validator.validateProduct(in, true));

    in = valid();
    in.stock = -5;
    assertNotNull(Validator.validateProduct(in, true));
    assertNull(Validator.validateProduct(in, false)); // stok tidak diperiksa saat edit
  }
}
