/*
 * Copyright 2025 the original author or authors.
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

package org.bremersee.dccon.model;

import static java.util.Objects.isNull;

import lombok.Getter;

/**
 * The enum TreeSearchScope.
 *
 * @author Christian Bremer
 */
@Getter
public enum TreeSearchScope {

  ONELEVEL("search-scope.one-level.label", "One Level"), // Eine Ebene

  SUBTREE("search-scope.subtree.label", "Subtree"); // Teilbaum

  private final String i18nCode;

  private final String defaultDisplayName;

  TreeSearchScope(String i18nCode, String defaultDisplayName) {
    this.i18nCode = i18nCode;
    this.defaultDisplayName = defaultDisplayName;
  }

  public String getParameterValue() {
    return name().toLowerCase();
  }

  public static TreeSearchScope fromValue(String value) {
    if (isNull(value) || value.isEmpty()) {
      return null;
    }
    try {
      return valueOf(value.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

}
