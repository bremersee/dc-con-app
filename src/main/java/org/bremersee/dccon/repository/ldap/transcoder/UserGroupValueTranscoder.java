/*
 * Copyright 2019 the original author or authors.
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

package org.bremersee.dccon.repository.ldap.transcoder;

import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The user group value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class UserGroupValueTranscoder extends AbstractStringValueTranscoder<String> {

  private DomainControllerProperties properties;

  /**
   * Instantiates a new user group value transcoder.
   *
   * @param properties the properties
   */
  public UserGroupValueTranscoder(DomainControllerProperties properties) {
    this.properties = properties;
  }

  @Override
  public String decodeStringValue(String value) {
    return LdaptiveEntryMapper.getRdn(value);
  }

  @Override
  public String encodeStringValue(String value) {
    if (StringUtils.hasText(value)) {
      return LdaptiveEntryMapper
          .createDn(properties.getGroupRdn(), value, properties.getGroupBaseDn());
    }
    return null;
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }
}
