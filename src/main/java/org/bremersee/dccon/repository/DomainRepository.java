/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.dccon.repository;

import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.model.PasswordInformation;
import org.bremersee.dccon.model.SamAccount;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.PasswordGenerator;

/**
 * The domain repository interface.
 *
 * @author Christian Bremer
 */
public interface DomainRepository extends RepositoryConstants, ErrorCode {

  boolean dnExistsWithAnyObjectClass(String dn, String... objectClasses);

  default boolean samAccountExists(SamAccount samAccount) {
    return findDnOfSamAccount(samAccount).isPresent();
  }

  default boolean samAccountNameExists(String samAccountName) {
    return findDnOfSamAccountName(samAccountName).isPresent();
  }

  default Optional<String> findDnOfSamAccount(SamAccount samAccount) {
    if (isEmpty(samAccount)) {
      return Optional.empty();
    }
    return findDnOfSamAccountName(samAccount.getSamAccountName());
  }

  Optional<String> findDnOfSamAccountName(String samAccountName);

  /**
   * Specifies whether NIS extensions (rfc2307) are installed on the AD Domain Controller. See <a
   * href="https://wiki.samba.org/index.php/Setting_up_RFC2307_in_AD">Setting up RFC2307 in AD</a>
   */
  boolean isRfc2307Enabled();

  /**
   * Gets password information.
   *
   * @return the password information
   */
  PasswordInformation getPasswordInformation();

  /**
   * Create random password.
   *
   * @return the random password
   */
  default String createRandomPassword() {
    PasswordInformation passwordInformation = getPasswordInformation();
    int minLength = requireNonNullElse(passwordInformation.getMinimumPasswordLength(), 12);
    int maxLength = requireNonNullElse(passwordInformation.getMaximumPasswordLength(), 75);
    int maxPlus = Math.min(maxLength - minLength, 9);
    int length = Optional.of(minLength + (maxPlus > 0 ? new Random().nextInt(maxPlus) : 0))
        .filter(len -> len >= 4)
        .orElse(4);
    int lower = (int) Math.floor(length * 0.3);
    int upper = (int) Math.floor(length * 0.3);
    int digit = (int) Math.floor(length * 0.2);
    int special = 1;
    List<CharacterRule> rules = getCharacterRules(lower, upper, digit, special);
    return new PasswordGenerator().generatePassword(length, rules);
  }

  default List<CharacterRule> getCharacterRules(
      int lowerNum, int upperNum, int digitNum, int specialNum) {
    return List.of(
        new CharacterRule(new SpecialCharacterData(), specialNum > 0 ? specialNum : 1),
        new CharacterRule(new DigitCharacterData(), digitNum > 0 ? digitNum : 1),
        new CharacterRule(new UpperCharacterData(), upperNum > 0 ? upperNum : 1),
        new CharacterRule(new LowerCharacterData(), lowerNum > 0 ? lowerNum : 1)
    );
  }

  class SpecialCharacterData implements CharacterData {

    @Override
    public String getErrorCode() {
      return "INSUFFICIENT_SPECIAL";
    }

    @Override
    public String getCharacters() {
      return "!#$%&*+-.:<=>?@_";
    }
  }

  class DigitCharacterData implements CharacterData {

    @Override
    public String getErrorCode() {
      return "INSUFFICIENT_DIGIT";
    }

    @Override
    public String getCharacters() {
      return "123456789";
    }
  }

  class UpperCharacterData implements CharacterData {

    @Override
    public String getErrorCode() {
      return "INSUFFICIENT_UPPER";
    }

    @Override
    public String getCharacters() {
      return "ABCDEFGHJKLMNPQRSTUVWXYZ";
    }
  }

  class LowerCharacterData implements CharacterData {

    @Override
    public String getErrorCode() {
      return "INSUFFICIENT_LOWER";
    }

    @Override
    public String getCharacters() {
      return "abcdefghijkmnpqrstuvwxyz";
    }
  }

}
