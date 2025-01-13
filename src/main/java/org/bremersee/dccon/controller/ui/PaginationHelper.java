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

package org.bremersee.dccon.controller.ui;

import org.bremersee.pagebuilder.model.AbstractPageDto;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The type PaginationHelper.
 *
 * @author Christian Bremer
 */
@Component("paginationHelper")
public class PaginationHelper {

  private int ceil(Number a, Number b) {
    return (int) Math.ceil(a.doubleValue() / b.doubleValue());
  }

  public int getTotalPages(AbstractPageDto<?> page) {
    Assert.notNull(page, "Page must not be null.");
    return ceil(page.getTotalElements(), page.getSize());
  }

  public boolean isValidPageNumber(AbstractPageDto<?> page, int number) {
    Assert.notNull(page, "Page must not be null.");
    int totalPages = getTotalPages(page);
    return 0 <= number && number < totalPages;
  }

}
