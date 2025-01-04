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

package org.bremersee.dccon.controller.ui.components;

import java.util.Optional;
import org.bremersee.dccon.controller.ControllerConstants;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The interface PageableComponent.
 *
 * @author Christian Bremer
 */
public interface PageableComponent extends ControllerConstants {

  @ModelAttribute(PAGE)
  default int addPage(@RequestParam(name = PAGE, defaultValue = PAGE_DEFAULT) int page) {
    return page;
  }

  @ModelAttribute(SIZE)
  default int addSize(@RequestParam(name = SIZE, defaultValue = SIZE_DEFAULT) int size) {
    return size;
  }

  String getDefaultSort();

  @ModelAttribute(SORT)
  default String addSort(@RequestParam(name = SORT, required = false) String sort) {
    return Optional.ofNullable(sort).orElse(getDefaultSort());
  }

  @ModelAttribute(QUERY)
  default String addQuery(@RequestParam(name = QUERY, defaultValue = QUERY_DEFAULT) String q) {
    return q;
  }

}
