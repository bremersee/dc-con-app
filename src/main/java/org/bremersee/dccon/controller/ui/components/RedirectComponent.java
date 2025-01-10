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
import jakarta.validation.constraints.NotNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bremersee.dccon.controller.ui.ControllerConstants;
import org.bremersee.dccon.controller.ui.LoggerProvider;
import org.ldaptive.SearchScope;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

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

  default Map<String, Object> getParamterMap(Integer page, Integer size, String sort,
      String query) {
    return Map.of(
        PAGE, Optional.ofNullable(page).orElse(PAGE_DEFAULT_INT),
        SIZE, Optional.ofNullable(size)
            .filter(s -> s > 0)
            .orElse(SIZE_DEFAULT_INT),
        SORT, Optional.ofNullable(sort).orElse(""),
        QUERY, Optional.ofNullable(query).orElse("")
    );
  }

  default Map<String, Object> getParamterMap(Integer page, Integer size, String sort,
      String query, Dn ou, SearchScope scope) {
    return Map.of(
        PAGE, Optional.ofNullable(page).orElse(PAGE_DEFAULT_INT),
        SIZE, Optional.ofNullable(size)
            .filter(s -> s > 0)
            .orElse(SIZE_DEFAULT_INT),
        SORT, Optional.ofNullable(sort).orElse(""),
        QUERY, Optional.ofNullable(query).orElse(""),
        OU, Optional.ofNullable(ou)
            .map(Dn::format)
            .orElse(""),
        SCOPE, Optional.ofNullable(scope).orElse(SearchScope.ONELEVEL)
    );
  }

  default Map<String, Object> addToMap(Map<String, Object> map, String key, Object value) {
    Map<String, Object> result = new HashMap<>(map);
    result.put(key, value);
    return result;
  }

  default String getRedirectUri(
      @NotNull String path,
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
