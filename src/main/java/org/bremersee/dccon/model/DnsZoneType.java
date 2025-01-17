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

import lombok.Getter;

/**
 * The enum DnsZoneType.
 *
 * @author Christian Bremer
 */
@Getter
public enum DnsZoneType {

  /**
   * List primary zones (default).
   */
  PRIMARY("primary"),

  /**
   * List secondary zones.
   */
  SECONDARY("secondary"),

  /**
   * List cached zones.
   */
  CACHE("cache"),

  /**
   * List automatically created zones.
   */
  AUTO("auto"),

  /**
   * List forward zones.
   */
  FORWARD("forward"),

  /**
   * List reverse zones.
   */
  REVERSE("reverse"),

  /**
   * List directory integrated zones.
   */
  DS("ds"),

  /**
   * List non-directory zones.
   */
  NON_DS("non-ds");

  private final String parameterName;

  DnsZoneType(String parameterName) {
    this.parameterName = parameterName;
  }
}
