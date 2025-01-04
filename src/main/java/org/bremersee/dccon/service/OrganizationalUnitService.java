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

package org.bremersee.dccon.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.bremersee.dccon.model.SelectOption;
import org.ldaptive.dn.Dn;
import org.springframework.lang.Nullable;

/**
 * The interface OrganizationalUnitService.
 *
 * @author Christian Bremer
 */
public interface OrganizationalUnitService {

  List<SelectOption<OrganizationalUnit>> getOrganizationalUnitSelectors(@Nullable Dn ou);

  List<SelectOption<OrganizationalUnit>> getOrganizationalUnitSelectorsWithBase(@Nullable Dn ou);

  Stream<OrganizationalUnit> getOrganizationalUnits();

  Stream<OrganizationalUnit> getOrganizationalUnitsWithBase();

  Stream<OrganizationalUnit> getOrganizationalUnitsWithBaseButWithoutSelected(@Nullable Dn ou);

  Optional<OrganizationalUnit> getOrganizationalUnit(@Nullable Dn ou);

  OrganizationalUnit getBase();

}
