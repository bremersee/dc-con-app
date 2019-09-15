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

package org.bremersee.dccon.repository;

import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.dccon.model.DhcpLease;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The dhcp repository mock.
 *
 * @author Christian Bremer
 */
@Profile("!cli")
@Component
@Slf4j
public class DhcpRepositoryMock implements DhcpRepository {

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    log.warn("\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
        + "!! MOCK is running:  DhcpRepository                                                 !!\n"
        + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    );
  }

  @Override
  public List<DhcpLease> findAll() {
    return Collections.emptyList();
  }

  @Override
  public List<DhcpLease> findActive() {
    return Collections.emptyList();
  }

}
