package com.kasirku;

import static org.junit.Assert.assertEquals;

import com.kasirku.util.RupiahFormat;

import org.junit.Test;

public class RupiahFormatTest {

  @Test
  public void format_angkaBiasa() {
    assertEquals("Rp 25.000", RupiahFormat.format(25000));
    assertEquals("Rp 1.250.000", RupiahFormat.format(1250000));
  }

  @Test
  public void format_nolDanNegatif() {
    assertEquals("Rp 0", RupiahFormat.format(0));
    assertEquals("-Rp 5.000", RupiahFormat.format(-5000));
  }

  @Test
  public void parse_mengambilAngkaSaja() {
    assertEquals(25000, RupiahFormat.parse("Rp 25.000"));
    assertEquals(0, RupiahFormat.parse(""));
    assertEquals(0, RupiahFormat.parse(null));
  }
}
