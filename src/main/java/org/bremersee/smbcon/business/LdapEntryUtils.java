/*
 * Copyright 2017 the original author or authors.
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

package org.bremersee.smbcon.business;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.AttributeModification;
import org.ldaptive.AttributeModificationType;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.springframework.util.StringUtils;

/**
 * The type Ldap entry utils.
 *
 * @author Christian Bremer
 */
@Slf4j
abstract class LdapEntryUtils {

  /**
   * The Uf account disabled.
   */
  static final int UF_ACCOUNT_DISABLED = 1 << 1;

  /**
   * The Uf normal account.
   */
  static final int UF_NORMAL_ACCOUNT = 1 << 9;

  /**
   * The Uf dont expire passwd.
   */
  static final int UF_DONT_EXPIRE_PASSWD = 1 << 16;

  private static final String WHEN_DATE_PATTERN = "yyyyMMddHHmmss";

  private static final long ACTIVE_DIRECTORY_START_TIME;

  static {
    final GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    cal.set(1601, Calendar.JANUARY, 1, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    ACTIVE_DIRECTORY_START_TIME = cal.getTimeInMillis();
  }

  private LdapEntryUtils() {
  }

  /**
   * Gets attribute value.
   *
   * @param ldapEntry     the ldap entry
   * @param attributeName the attribute name
   * @param defaultValue  the default value
   * @return the attribute value
   */
  static String getAttributeValue(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final String attributeName,
      final String defaultValue) {

    final LdapAttribute attr = ldapEntry.getAttribute(attributeName);
    if (attr == null || !StringUtils.hasText(attr.getStringValue())) {
      return defaultValue;
    }
    return attr.getStringValue();
  }

  private static void removeAttribute(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final String attributeName,
      @NotNull final List<AttributeModification> modifications) {

    if (ldapEntry.getAttribute(attributeName) != null) {
      ldapEntry.removeAttribute(attributeName);
      modifications.add(
          new AttributeModification(
              AttributeModificationType.REMOVE,
              new LdapAttribute(attributeName)));
    }
  }

  private static void setAttribute(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final String attributeName,
      @NotNull final String attributeValue,
      @NotNull final List<AttributeModification> modifications) {

    LdapAttribute attr = ldapEntry.getAttribute(attributeName);
    if (attr == null) {
      attr = new LdapAttribute(attributeName, attributeValue);
      modifications.add(
          new AttributeModification(
              AttributeModificationType.ADD,
              attr));
      ldapEntry.addAttribute(attr);
    } else if (!attributeValue.equals(attr.getStringValue())) {
      modifications.add(
          new AttributeModification(
              AttributeModificationType.REPLACE,
              new LdapAttribute(attributeName, attributeValue)));
      ldapEntry.removeAttribute(attributeName);
      ldapEntry.addAttribute(new LdapAttribute(attributeName, attributeValue));
    }
  }

  /**
   * Update attribute.
   *
   * @param ldapEntry      the ldap entry
   * @param attributeName  the attribute name
   * @param attributeValue the attribute value
   * @param modifications  the modifications
   */
  static void updateAttribute(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final String attributeName,
      final String attributeValue,
      @NotNull final List<AttributeModification> modifications) {

    if (StringUtils.hasText(attributeValue)) {
      setAttribute(ldapEntry, attributeName, attributeValue, modifications);
    } else {
      removeAttribute(ldapEntry, attributeName, modifications);
    }
  }

  /**
   * Create dn string.
   *
   * @param rdn      the rdn
   * @param rdnValue the rdn value
   * @param baseDn   the base dn
   * @return the string
   */
  static String createDn(
      @NotNull final String rdn,
      @NotNull final String rdnValue,
      @NotNull final String baseDn) {
    return rdn + "=" + rdnValue + "," + baseDn;
  }

  private static Long activeDirectoryTimeToMillis(final String value) {
    if (!StringUtils.hasText(value) || "0".equals(value)) {
      return null;
    }
    try {
      return ACTIVE_DIRECTORY_START_TIME + (Long.parseLong(value) / 10000L);

    } catch (final Exception e) {
      log.error("Active directory time value [{}] could not be parsed.", value, e);
      return null;
    }
  }

  private static Date activeDirectoryTimeToDate(final String value) {
    final Long millis = activeDirectoryTimeToMillis(value);
    return millis == null ? null : new Date(millis);
  }

  /**
   * Active directory time to offset date time.
   *
   * @param value the value
   * @return the offset date time
   */
  static OffsetDateTime activeDirectoryTimeToOffsetDateTime(final String value) {
    final Date date = activeDirectoryTimeToDate(value);
    return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
  }

  private static Date whenTimeToDate(final String value) {
    if (!StringUtils.hasText(value) || value.length() < WHEN_DATE_PATTERN.length()) {
      return null;
    }
    final SimpleDateFormat sdf = new SimpleDateFormat(WHEN_DATE_PATTERN);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    try {
      return sdf.parse(value.substring(0, WHEN_DATE_PATTERN.length()));

    } catch (final Exception e) {
      log.error("Parsing when time [{}] failed. Returning null.", value, e);
      return null;
    }
  }

  /**
   * When time to offset date time.
   *
   * @param value the value
   * @return the offset date time
   */
  static OffsetDateTime whenTimeToOffsetDateTime(final String value) {
    final Date date = whenTimeToDate(value);
    return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
  }

  /**
   * Gets user account control.
   *
   * @param ldapEntry the ldap entry
   * @return the user account control
   */
  static int getUserAccountControl(@NotNull final LdapEntry ldapEntry) {
    try {
      return Integer.parseInt(getAttributeValue(
          ldapEntry,
          "userAccountControl",
          String.valueOf(UF_NORMAL_ACCOUNT + UF_DONT_EXPIRE_PASSWD)));

    } catch (final Exception ignored) {
      return UF_NORMAL_ACCOUNT + UF_DONT_EXPIRE_PASSWD;
    }
  }

}
