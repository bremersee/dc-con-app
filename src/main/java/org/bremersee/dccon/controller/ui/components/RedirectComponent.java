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

package org.bremersee.dccon.controller.ui.components;

import static org.springframework.util.ObjectUtils.isEmpty;

import com.samskivert.mustache.Mustache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bremersee.dccon.controller.ui.ControllerConstants;
import org.bremersee.dccon.controller.ui.LoggerProvider;
import org.bremersee.dccon.converter.DnConverter;
import org.bremersee.dccon.model.DnsZoneType;
import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The interface RedirectComponent.
 *
 * @author Christian Bremer
 */
@Validated
public interface RedirectComponent extends ControllerConstants, LoggerProvider {

  String PAGE_PARAMS = PAGE + "={{" + PAGE + "}}"
      + "&" + SIZE + "={{" + SIZE + "}}"
      + "&" + SORT + "={{" + SORT + "}}"
      + "&" + QUERY + "={{" + QUERY + "}}";

  String PAGE_AND_OU_PARAMS = PAGE_PARAMS
      + "&" + OU + "={{" + OU + "}}"
      + "&" + SCOPE + "={{" + SCOPE + "}}";

  String PAGE_AND_ZONE_TYPE_PARAMS = PAGE_PARAMS
      + "&" + ZONE_TYPE + "={{" + ZONE_TYPE + "}}";

  String PAGE_AND_ZONE_NAME_PARAMS = PAGE_AND_ZONE_TYPE_PARAMS
      + "&" + ZONE_NAME + "={{" + ZONE_NAME + "}}";

  String PAGE_AND_DNS_ENTRY_PARAMS = PAGE_AND_ZONE_NAME_PARAMS
      + "&name={{name}}"
      + "&type={{type}}"
      + "&value={{value}}";

  default Map<String, Object> getParamterMap(Dn ou) {
    return Map.of(
        PAGE, findPageParameterValue(),
        SIZE, findSizeParameterValue(),
        SORT, findParameterValue(SORT).orElse(""),
        QUERY, findParameterValue(QUERY).orElse(""),
        OU, Optional.ofNullable(ou)
            .or(this::findOuParameterValue)
            .filter(dn -> !dn.isEmpty())
            .map(Dn::format)
            .orElse(""),
        SCOPE, findScopeParameterValue()
            .map(TreeSearchScope::getParameterValue)
            .orElse(""),
        ZONE_TYPE, findZoneTypeParameterValue()
            .map(DnsZoneType::getParameterValue)
            .orElse(""),
        ZONE_NAME, findParameterValue(ZONE_NAME).orElse("")
    );
  }

  default Map<String, Object> getParamterMap() {
    return getParamterMap(null);
  }

  default Map<String, Object> putToParameterMap(Map<String, Object> map, String key, Object value) {
    Map<String, Object> result = new HashMap<>(map);
    result.put(key, value);
    return result;
  }

  default Optional<HttpServletRequest> findHttpServletRequest() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(attrs -> attrs instanceof ServletRequestAttributes)
        .map(attrs -> (ServletRequestAttributes) attrs)
        .map(ServletRequestAttributes::getRequest);
  }

  default Optional<String> findParameterValue(String parameterName) {
    return findHttpServletRequest()
        .map(req -> req.getParameter(parameterName));
  }

  default Integer findPageParameterValue() {
    return findParameterValue(PAGE)
        .map(value -> {
          try {
            return Integer.parseInt(value);
          } catch (NumberFormatException e) {
            return null;
          }
        })
        .filter(value -> value >= 0)
        .orElse(PAGE_DEFAULT_INT);
  }

  default Integer findSizeParameterValue() {
    return findParameterValue(SIZE)
        .map(value -> {
          try {
            return Integer.parseInt(value);
          } catch (NumberFormatException e) {
            return null;
          }
        })
        .filter(value -> value >= 1)
        .orElse(SIZE_DEFAULT_INT);
  }

  default Optional<Dn> findOuParameterValue() {
    return findParameterValue(OU)
        .map(dn -> new DnConverter().convert(dn));
  }

  default Optional<TreeSearchScope> findScopeParameterValue() {
    return findParameterValue(SCOPE)
        .map(TreeSearchScope::fromValue);
  }

  default Optional<DnsZoneType> findZoneTypeParameterValue() {
    return findParameterValue(ZONE_TYPE)
        .map(DnsZoneType::fromValue);
  }

  default String getRedirectUri(
      @NotEmpty String path,
      @Nullable String mustacheTemplate,
      @Nullable Map<String, Object> parameters) {

    String template = mustacheTemplate;
    StringBuilder sb = new StringBuilder();
    if (!path.toLowerCase().startsWith("redirect:")) {
      sb.append("redirect:");
    }
    sb.append(path);
    if (!isEmpty(template)) {
      if (template.startsWith("?") || template.startsWith("&")) {
        template = template.substring(1);
      }
      if (path.contains("?")) {
        sb.append("&");
      } else {
        sb.append("?");
      }
      sb.append(template);
    }
    template = sb.toString();
    String redirect = Mustache
        .compiler()
        .withEscaper(raw -> URLEncoder.encode(raw, StandardCharsets.UTF_8))
        .defaultValue("")
        .compile(template)
        .execute(Objects.requireNonNullElseGet(parameters, Map::of));
    getLogger().debug("Redirect URI: {}", redirect);
    return redirect;
  }

}
