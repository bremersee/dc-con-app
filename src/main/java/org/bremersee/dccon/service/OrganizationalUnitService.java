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

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.stream.Stream;
import org.bremersee.dccon.model.OrganizationalUnit;
import org.ldaptive.dn.Dn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * The interface OrganizationalUnitService.
 *
 * @author Christian Bremer
 */
public interface OrganizationalUnitService {

  Page<OrganizationalUnit> getOrganizationalUnits(Pageable pageable, String query);

  Stream<OrganizationalUnit> getOrganizationalUnits();

  Stream<OrganizationalUnit> getOrganizationalUnitsWithBase();

  Optional<OrganizationalUnit> getOrganizationalUnit(@Nullable Dn ou);

  OrganizationalUnit getBase();

  boolean organisationUnitExists(@Nullable Dn ou);

  OrganizationalUnit add(@NotNull OrganizationalUnit organizationalUnit, @Nullable Dn parentOu);

  OrganizationalUnit update(@NotNull OrganizationalUnit organizationalUnit, @Nullable Dn newParentOu);

  boolean delete(@NotNull Dn ou);

}
