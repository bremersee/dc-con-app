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

package org.bremersee.dccon.converter;

import org.bremersee.dccon.model.DnsZoneType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * The type DnsZoneTypeConverter.
 *
 * @author Christian Bremer
 */
@Component
public class DnsZoneTypeConverter implements Converter<String, DnsZoneType> {

  @Override
  public DnsZoneType convert(@NonNull String source) {
    return DnsZoneType.fromValue(source);
  }
}
