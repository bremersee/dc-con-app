package org.bremersee.dccon.repository.ldap.transcoder;

import static org.bremersee.dccon.repository.ldap.transcoder.UserAccountControlValueTranscoder.ACCOUNTDISABLE;
import static org.bremersee.dccon.repository.ldap.transcoder.UserAccountControlValueTranscoder.DONT_EXPIRE_PASSWORD;
import static org.bremersee.dccon.repository.ldap.transcoder.UserAccountControlValueTranscoder.NORMAL_ACCOUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The user account control value transcoder test.
 *
 * @author Christian Bremer
 */
public class UserAccountControlValueTranscoderTest {

  private static final UserAccountControlValueTranscoder transcoder
      = new UserAccountControlValueTranscoder();

  /**
   * Gets user account control value.
   */
  @Test
  public void getUserAccountControlValue() {
    int value = UserAccountControlValueTranscoder.getUserAccountControlValue(
        true,
        0);
    assertEquals(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD, value);
    value = UserAccountControlValueTranscoder.getUserAccountControlValue(false, 0);
    assertEquals(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD + ACCOUNTDISABLE, value);
  }

  /**
   * Is user account enabled.
   */
  @Test
  public void isUserAccountEnabled() {
    assertTrue(UserAccountControlValueTranscoder.isUserAccountEnabled(NORMAL_ACCOUNT));
    assertFalse(UserAccountControlValueTranscoder
        .isUserAccountEnabled(NORMAL_ACCOUNT + ACCOUNTDISABLE));
  }

  /**
   * Decode string value.
   */
  @Test
  public void decodeStringValue() {
    assertEquals(Integer.valueOf(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD),
        transcoder.decodeStringValue(null));
    assertEquals(Integer.valueOf(NORMAL_ACCOUNT),
        transcoder.decodeStringValue(String.valueOf(NORMAL_ACCOUNT)));
  }

  /**
   * Encode string value.
   */
  @Test
  public void encodeStringValue() {
    assertEquals(String.valueOf(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD),
        transcoder.encodeStringValue(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD));
    assertEquals(String.valueOf(NORMAL_ACCOUNT),
        transcoder.encodeStringValue(NORMAL_ACCOUNT));
  }

  /**
   * Gets type.
   */
  @Test
  public void getType() {
    assertEquals(Integer.class, transcoder.getType());
  }
}