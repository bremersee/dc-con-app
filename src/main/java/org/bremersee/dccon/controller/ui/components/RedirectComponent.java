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
import java.util.Map;
import java.util.Objects;
import org.bremersee.dccon.controller.ControllerConstants;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

/**
 * The interface RedirectComponent.
 *
 * @author Christian Bremer
 */
@Validated
public interface RedirectComponent extends ControllerConstants {

  String PAGE_PARAMS = PAGE + "={{" + PAGE + "}}"
      + "&" + SIZE + "={{" + SIZE + "}}"
      + "&" + SORT + "={{" + SORT + "}}"
      + "&" + QUERY + "={{" + QUERY + "}}"
      + "&" + OU + "={{" + OU + "}}"
      + "&" + SCOPE + "={{" + SCOPE + "}}";

  String PAGE_AND_OU_PARAMS = PAGE_PARAMS
      + "&" + OU + "={{" + OU + "}}"
      + "&" + SCOPE + "={{" + SCOPE + "}}";

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
    return Mustache
        .compiler()
        .escapeHTML(true)
        .defaultValue("")
        .compile(template)
        .execute(Objects.requireNonNullElseGet(parameters, Map::of));
  }

}
