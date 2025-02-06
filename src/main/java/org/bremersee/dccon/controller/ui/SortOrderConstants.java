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

/**
 * The interface SortOrderConstants.
 *
 * @author Christian Bremer
 */
public interface SortOrderConstants extends ControllerConstants {

  String USER_SORT = "lastName;firstName;samAccountName";

  String GROUP_SORT = "samAccountName";

  String COMPUTER_SORT = "name";

  String OU_SORT = "nameTree";

  String DNS_ENTRY_SORT = "name;type;value";

  String DHCP_LEASE_SORT = "ip";

}
