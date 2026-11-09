package com.kasirku;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.kasirku.util.Validator;

import org.junit.Test;

public class StockValidationTest {

  @Test
  public void isianValid() {
    assertNull(Validator.validateAdjustmentInput(5, "Barang rusak"));
    assertNull(Validator.validateAdjustmentInput(-5, "  Barang hilang  "));
  }

  @Test
  public void jumlahNolDitolak() {
    assertNotNull(Validator.validateAdjustmentInput(0, "Barang rusak"));
  }

  @Test
  public void jumlahTerlaluBesarDitolak() {
    assertNotNull(Validator.validateAdjustmentInput(1_000_001, "Barang masuk"));
    assertNotNull(Validator.validateAdjustmentInput(-1_000_001, "Barang hilang"));
  }

  @Test
  public void alasanWajibDanTidakBolehTerlaluPanjang() {
    assertNotNull(Validator.validateAdjustmentInput(5, ""));
    assertNotNull(Validator.validateAdjustmentInput(5, "  ab  "));
    assertNotNull(Validator.validateAdjustmentInput(5, null));

    String panjang = new String(new char[201]).replace('\0', 'a');
    assertNotNull(Validator.validateAdjustmentInput(5, panjang));
  }

  @Test
  public void stokTidakBolehNegatif() {
    assertNull(Validator.validateAdjustmentResult(10, -10));     // jadi 0, boleh
    assertNotNull(Validator.validateAdjustmentResult(10, -11));  // jadi -1, ditolak
    assertNotNull(Validator.validateAdjustmentResult(0, -1));
  }

  @Test
  public void stokTidakBolehMelebihiBatas() {
    assertNull(Validator.validateAdjustmentResult(999_999, 1));      // pas 1.000.000
    assertNotNull(Validator.validateAdjustmentResult(999_999, 2));   // 1.000.001
  }
}
