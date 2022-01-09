package org.bremersee.dccon.repository.ldap.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

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