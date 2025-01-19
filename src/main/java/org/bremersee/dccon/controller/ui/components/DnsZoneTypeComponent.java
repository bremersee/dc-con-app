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

import org.bremersee.dccon.controller.ui.ControllerConstants;
import org.bremersee.dccon.model.DnsZoneType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The interface DnsZoneTypeComponent.
 *
 * @author Christian Bremer
 */
public interface DnsZoneTypeComponent extends ControllerConstants {

  @ModelAttribute(ZONE_TYPE_VARIABLE)
  default String addDnsZoneType(
      @RequestParam(name = ZONE_TYPE, defaultValue = ZONE_TYPE_DEFAULT) DnsZoneType zoneType) {
    return zoneType.getParameterValue();
  }

}
