package org.bremersee.dccon.repository.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.bremersee.dccon.repository.transcoder.UserAccountControl;
import org.bremersee.dccon.repository.transcoder.UserAccountControlValueTranscoder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The user account control value transcoder test.
 *
 * @author Christian Bremer
 */
@Disabled
@ExtendWith(SoftAssertionsExtension.class)
class UserAccountControlValueTranscoderTest {

  private static final UserAccountControlValueTranscoder transcoder
      = new UserAccountControlValueTranscoder();

  /**
   * Gets user account control value.
   *
   * @param softly the soft assertions
   */
  @Test
  void getUserAccountControlValue(SoftAssertions softly) {
    /*
    int value = UserAccountControlValueTranscoder.getUserAccountControlValue(
        true,
        0);
    softly.assertThat(value)
        .isEqualTo(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD);

    value = UserAccountControlValueTranscoder.getUserAccountControlValue(false, 0);
    softly.assertThat(value)
        .isEqualTo(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD + DISABLED_ACCOUNT);
    */
  }

  /**
   * Is user account enabled.
   *
   * @param softly the soft assertions
   */
  @Test
  void isUserAccountEnabled(SoftAssertions softly) {
    /*
    softly.assertThat(UserAccountControlValueTranscoder
            .isUserAccountEnabled(NORMAL_ACCOUNT))
        .isTrue();
    softly.assertThat(UserAccountControlValueTranscoder
            .isUserAccountEnabled(NORMAL_ACCOUNT + DISABLED_ACCOUNT))
        .isFalse();

     */
  }

  /**
   * Decode string value.
   *
   * @param softly the soft assertions
   */
  @Test
  void decodeStringValue(SoftAssertions softly) {
    /*
    softly.assertThat(transcoder.decodeStringValue(null))
        .isEqualTo(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD);
    softly.assertThat(transcoder.decodeStringValue(String.valueOf(NORMAL_ACCOUNT)))
        .isEqualTo(NORMAL_ACCOUNT);
    */
  }

  /**
   * Encode string value.
   *
   * @param softly the soft assertions
   */
  @Test
  void encodeStringValue(SoftAssertions softly) {
    /*
    softly.assertThat(transcoder.encodeStringValue(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD))
        .isEqualTo(String.valueOf(NORMAL_ACCOUNT + DONT_EXPIRE_PASSWORD));
    softly.assertThat(transcoder.encodeStringValue(NORMAL_ACCOUNT))
        .isEqualTo(String.valueOf(NORMAL_ACCOUNT));
    */
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertThat(transcoder.getType())
        .isEqualTo(UserAccountControl.class);
  }
}