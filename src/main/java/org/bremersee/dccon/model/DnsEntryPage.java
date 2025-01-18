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

package org.bremersee.dccon.model;

import java.util.List;
import org.bremersee.comparator.model.SortOrders;
import org.bremersee.pagebuilder.model.JsonPageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

/**
 * The type DnsEntryPage.
 *
 * @author Christian Bremer
 */
public class DnsEntryPage extends JsonPageDto<DnsEntry> {

  public DnsEntryPage() {
  }

  public DnsEntryPage(List<? extends DnsEntry> content, int number, int size,
      long totalElements) {
    super(content, number, size, totalElements);
  }

  public DnsEntryPage(List<? extends DnsEntry> content, int number, int size,
      long totalElements, SortOrders sort) {
    super(content, number, size, totalElements, sort);
  }

  public DnsEntryPage(List<? extends DnsEntry> content, int number, int size,
      long totalElements, Sort sort) {
    super(content, number, size, totalElements, sort);
  }

  public DnsEntryPage(Page<? extends DnsEntry> page) {
    super(page);
  }
}
