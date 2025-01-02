/*
 * Copyright 2017 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.NonNull;

/**
 * @author Christian Bremer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE
)
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class SelectOption implements Serializable, Comparable<SelectOption> {

  @Serial
  private static final long serialVersionUID = -1811407290501112995L;

  @JsonProperty(value = "value", required = true)
  private String value;

  @JsonProperty(value = "displayValue")
  private String displayValue;

  @JsonProperty(value = "selected", defaultValue = "false")
  private boolean selected;

  @Override
  public int compareTo(@NonNull SelectOption selectOption) {
    String s0 = requireNonNullElse(displayValue, "");
    String s1 = requireNonNullElse(selectOption.displayValue, "");
    int c = s0.compareToIgnoreCase(s1);
    if (c != 0) {
      return c;
    } else {
      s0 = requireNonNullElse(value, "");
      s1 = requireNonNullElse(selectOption.value, "");
      return s0.compareTo(s1);
    }
  }

}
