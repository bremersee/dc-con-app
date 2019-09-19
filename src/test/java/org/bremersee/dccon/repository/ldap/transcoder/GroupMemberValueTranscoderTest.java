package org.bremersee.dccon.repository.ldap.transcoder;

import static org.junit.Assert.assertEquals;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The group member value transcoder test.
 *
 * @author Christian Bremer
 */
public class GroupMemberValueTranscoderTest {

  private static GroupMemberValueTranscoder transcoder;

  /**
   * Init.
   */
  @BeforeClass
  public static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setUserRdn("cn");
    properties.setUserBaseDn("cn=Users,dc=example,dc=org");
    transcoder = new GroupMemberValueTranscoder(properties);
  }

  /**
   * Decode string value.
   */
  @Test
  public void decodeStringValue() {
    String expected = "cn=anna,cn=Users,dc=example,dc=org";
    assertEquals(expected, transcoder.encodeStringValue("anna"));
  }

  /**
   * Encode string value.
   */
  @Test
  public void encodeStringValue() {
    assertEquals(
        "anna",
        transcoder.decodeStringValue("cn=anna,cn=Users,dc=example,dc=org"));
  }

  /**
   * Gets type.
   */
  @Test
  public void getType() {
    assertEquals(String.class, transcoder.getType());
  }
}