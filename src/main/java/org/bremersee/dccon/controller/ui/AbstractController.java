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

import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bremersee.comparator.model.SortOrders;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.ui.ModelMap;
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

  protected void addPageRequest(ModelMap model, int page, int size, SortOrders sort, String query) {
    if (!model.containsAttribute("page")) {
      model.addAttribute("page", page);
    }
    if (!model.containsAttribute("size")) {
      model.addAttribute("size", size);
    }
    if (!model.containsAttribute("sort")) {
      model.addAttribute("sort", sort.getSortOrdersText());
    }
    if (!model.containsAttribute("q")) {
      model.addAttribute("q", requireNonNullElse(query, ""));
    }
  }

  protected void addSearchScope(ModelMap model, SearchScope scope) {
    if (!model.containsAttribute("scope")) {
      model.addAttribute("ou", Optional.ofNullable(scope)
          .map(Enum::name)
          .map(String::toLowerCase)
          .orElse(""));
    }
  }

  protected String redirect(String path, int page, int size, SortOrders sort, String query) {
    String pathAppender = path.contains("?") ? "&" : "?";
    if (isEmpty(query)) {
      return String.format("redirect:%s%spage=%s&size=%s&sort=%s",
          path, pathAppender, page, size, sort.getSortOrdersText());
    }
    return String.format("redirect:%s%spage=%s&size=%s&sort=%s&q=%s",
        path, pathAppender, page, size, sort.getSortOrdersText(), encodeUrlParameter(query));
  }

  protected String encodeUrlParameter(String parameter) {
    return URLEncoder.encode(parameter, StandardCharsets.UTF_8);
  }

}
