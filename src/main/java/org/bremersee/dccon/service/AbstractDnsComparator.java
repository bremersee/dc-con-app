/*
 * Copyright 2019 the original author or authors.
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

package org.bremersee.dccon.service;

import java.util.Comparator;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * The abstract dns comparator.
 *
 * @param <T> the type parameter
 * @author Christian Bremer
 */
abstract class AbstractDnsComparator<T> implements Comparator<T> {

  @Getter(AccessLevel.PACKAGE)
  private boolean asc;

  /**
   * Instantiates a new abstract dns comparator.
   *
   * @param asc the asc
   */
  AbstractDnsComparator(Boolean asc) {
    this.asc = !Boolean.FALSE.equals(asc);
  }

  private static boolean isInteger(String value) {
    try {
      Integer.parseInt(value);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  /**
   * Format string.
   *
   * @param value the value
   * @param length the length
   * @return the string
   */
  static String format(String value, @SuppressWarnings("SameParameterValue") int length) {
    final String format = "%0" + length + "d";
    if (value == null) {
      return "";
    } else if (isInteger(value)) {
      return String.format(format, Integer.parseInt(value));
    }
    return value;
  }

}
