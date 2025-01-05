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

import static java.util.Objects.requireNonNullElse;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import org.bremersee.dccon.service.TemplateEngine;
import org.bremersee.dccon.service.TemplateEngineException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

/**
 * The interface FieldTemplateComponent.
 *
 * @author Christian Bremer
 */
@Validated
public interface FieldTemplateComponent {

  TemplateEngine getTemplateEngine();

  default String processTemplatedField(
      @NotNull BindingResult bindingResult,
      @NotNull String fieldName,
      @Nullable String value,
      @Nullable Map<String, Object> context) {

    try {
      return Optional.ofNullable(getTemplateEngine().compileAndExecute(value, context))
          .map(v -> {
            String renderedValue = v;
            while (renderedValue.contains("  ")) {
              renderedValue = renderedValue.replace("  ", " ");
            }
            return renderedValue;
          })
          .map(String::trim)
          .orElse(null);

    } catch (TemplateEngineException te) {
      bindingResult.rejectValue(
          fieldName,
          "unknown-template-key",
          new Object[]{te.getKey()},
          String.format("Unknown template key '%s'.", te.getKey()));
    } catch (RuntimeException re) {
      bindingResult.rejectValue(
          fieldName,
          "processing-template-failed",
          null,
          requireNonNullElse(re.getMessage(), "Processing template failed."));
    }
    return value;
  }

}
