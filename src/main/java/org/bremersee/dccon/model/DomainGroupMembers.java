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
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The type DomainGroupMembers.
 *
 * @author Christian Bremer
 */
@Getter
@EqualsAndHashCode
@ToString
public class DomainGroupMembers {

  @JsonProperty("members")
  private final List<DomainGroupMember> members = new ArrayList<>();

  @JsonProperty("unknownPresent")
  private boolean unknownPresent;

  @JsonProperty("userPresent")
  private boolean userPresent;

  @JsonProperty("groupPresent")
  private boolean groupPresent;

  @JsonProperty("computerPresent")
  private boolean computerPresent;

  private DomainGroupMembers() {
  }

  @JsonCreator
  public DomainGroupMembers(@JsonProperty("members") List<DomainGroupMember> members) {
    if (nonNull(members)) {
      members.forEach(this::add);
    }
  }

  public List<DomainGroupMember> getMembers() {
    return Collections.unmodifiableList(members);
  }

  private DomainGroupMembers add(DomainGroupMember member) {
    members.add(member);
    switch (member.getMemberType()) {
      case UNKNOWN -> unknownPresent = true;
      case USER -> userPresent = true;
      case GROUP -> groupPresent = true;
      case COMPUTER -> computerPresent = true;
    }
    return this;
  }

  private DomainGroupMembers addAll(DomainGroupMembers m) {
    members.addAll(m.members);
    this.unknownPresent = unknownPresent || m.unknownPresent;
    this.userPresent = userPresent || m.userPresent;
    this.groupPresent = groupPresent || m.groupPresent;
    this.computerPresent = computerPresent || m.computerPresent;
    return this;
  }

  public static DomainGroupMembers from(Stream<DomainGroupMember> members) {
    if (isNull(members)) {
      return new DomainGroupMembers();
    }
    return members.sorted()
        .reduce(new DomainGroupMembers(), DomainGroupMembers::add, DomainGroupMembers::addAll);
  }

  public static DomainGroupMembers empty() {
    return new DomainGroupMembers();
  }

}
