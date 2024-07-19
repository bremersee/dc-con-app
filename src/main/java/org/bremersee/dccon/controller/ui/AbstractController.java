/*
 * Copyright 2024 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type AbstractController.
 *
 * @author Christian Bremer
 */
public class AbstractController implements MessageSourceAware {

  @Getter(AccessLevel.PROTECTED)
  @Setter
  private MessageSource messageSource;

  @Getter(AccessLevel.PROTECTED)
  private LocaleResolver localeResolver;

  public AbstractController(LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  protected Locale resolveLocale(HttpServletRequest request) {
    return localeResolver.resolveLocale(request);
  }

}
