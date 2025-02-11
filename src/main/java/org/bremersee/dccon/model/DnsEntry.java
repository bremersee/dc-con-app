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

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * The type DnsEntry.
 *
 * @author Christian Bremer
 */
@Schema(description = "DNS Entry")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Slf4j
public class DnsEntry extends CommonAttributes {

  public static final String CONFLICT_IDENTIFIER = "CNF";

  public static final String CONFLICT_NAME_PART =
      "\\0A" + CONFLICT_IDENTIFIER + ':'; // TODO is it json conform?

  private String zoneName;

  private String name;

  private DnsEntryType type;

  private String value;

  private String flags;

  private Integer serial;

  private Integer ttlSeconds;

  public DnsEntry(String zoneName, String name) {
    this.zoneName = zoneName;
    this.name = name;
  }

  public DnsEntry(String zoneName, String name, DnsEntryType type, String value) {
    this.zoneName = zoneName;
    this.name = name;
    this.type = type;
    this.value = value;
  }

  public String getDisplayName() {
    if (isNull(name)) {
      return null;
    }
    int index = name.indexOf(CONFLICT_NAME_PART);
    return index > 0 ? name.substring(0, index) : name;
  }

  public boolean isConflict() {
    if (isNull(name)) {
      return false;
    }
    int index = name.indexOf(CONFLICT_NAME_PART);
    if (index < 0) {
      return false;
    }
    String guid = name.substring(index + CONFLICT_NAME_PART.length());
    try {
      UUID.fromString(guid);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /*
  @Hidden
  @JsonIgnore
  public final String getInternalId() {
    String nameStr = requireNonNullElse(getName(), "");
    String conflictStr = isConflict().toString();
    String guidStr = requireNonNullElse(getObjectGuid(), "");
    String typeStr = Optional.ofNullable(getType()).map(DnsEntryType::name).orElse("");
    String valueStr = requireNonNullElse(getValue(), "");
    String id = nameStr + ':' + conflictStr + ':' + guidStr + ':' + typeStr + ':' + valueStr;
    log.debug("====> id = {}", id);
    return Base64.getEncoder().encodeToString(id.getBytes(StandardCharsets.UTF_8));
  }

  public static DnsEntry fromInternalId(String internalId) {
    if (isNull(internalId) || internalId.isBlank()) {
      return null;
    }
    String id = new String(Base64.getDecoder().decode(internalId), StandardCharsets.UTF_8);
    log.debug("====> id = {}", id);
    DnsEntry entry = new DnsEntry();
    String[] parts = id.split(Pattern.quote(":"));
    if (parts.length > 0) {
      entry.setName(parts[0]);
    }
    if (parts.length > 1) {
      entry.setConflict(Boolean.parseBoolean(parts[1]));
    }
    if (parts.length > 2) {
      entry.setObjectGuid(parts[2].isBlank() ? null : parts[1]);
    }
    if (parts.length > 3) {
      entry.setType(DnsEntryType.fromValue(parts[3], null));
    }
    if (parts.length > 4) {
      entry.setValue(parts[4]);
    }
    if (parts.length > 5) {
      if (parts.length > 6) {
        entry.setValue(Arrays.stream(parts, 5, parts.length).collect(Collectors.joining(":")));
      } else {
        entry.setValue(parts[5]);
      }
    }
    return entry;
  }
  */

}
