package org.bremersee.dccon.repository.ldap.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The user group value transcoder test.
 *
 * @author Christian Bremer
 */
class UserGroupValueTranscoderTest {

  private static UserGroupValueTranscoder transcoder;

  /**
   * Init.
   */
  @BeforeAll
  static void init() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setGroupRdn("cn");
    properties.setGroupBaseDn("cn=Users,dc=example,dc=org");
    transcoder = new UserGroupValueTranscoder(properties);
  }

  /**
   * Decode string value.
   */
  @Test
  void decodeStringValue() {
    String expected = "cn=anna,cn=Users,dc=example,dc=org";
    assertThat(transcoder.encodeStringValue("anna"))
        .isEqualTo(expected);
  }

  /**
   * Encode string value.
   */
  @Test
  void encodeStringValue() {
    assertThat(transcoder.decodeStringValue("cn=anna,cn=Users,dc=example,dc=org"))
        .isEqualTo("anna");
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertThat(transcoder.getType())
        .isEqualTo(String.class);
  }
}