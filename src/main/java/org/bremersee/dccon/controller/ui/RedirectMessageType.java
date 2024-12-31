/*
 * Copyright 2016 the original author or authors.
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

package org.bremersee.dccon.controller.ui;

import lombok.Getter;

/**
 * @author Christian Bremer
 */
public enum RedirectMessageType {
  PRIMARY("alert alert-primary"),
  SECONDARY("alert alert-secondary"),
  SUCCESS("alert alert-success"),
  DANGER("alert alert-danger"),
  WARNING("alert alert-warning"),
  INFO("alert alert-info"),
  LIGHT("alert alert-light"),
  DARK("alert alert-dark");

  @Getter
  private final String cssClass;

  RedirectMessageType(final String cssClass) {
    this.cssClass = cssClass;
  }
}
