package org.bremersee.dccon.repository.ldap.transcoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The group member value transcoder test.
 *
 * @author Christian Bremer
 */
class GroupMemberValueTranscoderTest {

  private static GroupMemberValueTranscoder transcoder;

  /**
   * Init.
   */
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setUserRdn("cn");
    properties.setUserBaseDn("cn=Users,dc=example,dc=org");
    transcoder = new GroupMemberValueTranscoder(properties);
  }

  /**
   * Decode string value.
   */
  @Test
  void decodeStringValue() {
    String expected = "cn=anna,cn=Users,dc=example,dc=org";
    assertEquals(expected, transcoder.encodeStringValue("anna"));
  }

  /**
   * Encode string value.
   */
  @Test
  void encodeStringValue() {
    assertEquals(
        "anna",
        transcoder.decodeStringValue("cn=anna,cn=Users,dc=example,dc=org"));
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertEquals(String.class, transcoder.getType());
  }
}