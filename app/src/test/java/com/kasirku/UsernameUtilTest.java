package com.kasirku;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kasirku.util.UsernameUtil;

import org.junit.Test;

public class UsernameUtilTest {

  @Test
  public void usernameValid() {
    assertTrue(UsernameUtil.isValid("admin"));
    assertTrue(UsernameUtil.isValid("kasir.1"));
    assertTrue(UsernameUtil.isValid("  Budi_01 ")); // huruf besar dan spasi dirapikan
  }

  @Test
  public void usernameTidakValid() {
    assertFalse(UsernameUtil.isValid("ab"));          // terlalu pendek
    assertFalse(UsernameUtil.isValid("a b c"));       // ada spasi di tengah
    assertFalse(UsernameUtil.isValid("budi!"));       // karakter terlarang
    assertFalse(UsernameUtil.isValid(""));
    assertFalse(UsernameUtil.isValid(null));
  }

  @Test
  public void ubahKeEmailInternal() {
    assertEquals("admin@kasirku.app", UsernameUtil.toEmail("  Admin "));
    assertEquals("", UsernameUtil.normalize(null));
  }
}
