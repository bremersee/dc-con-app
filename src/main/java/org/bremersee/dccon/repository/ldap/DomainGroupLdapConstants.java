/*
 * Copyright 2019-2020 the original author or authors.
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

package org.bremersee.dccon.repository.ldap;

/**
 * The domain group ldap constants.
 *
 * @author Christian Bremer
 */
public class DomainGroupLdapConstants {

  /**
   * The constant DESCRIPTION.
   */
  public static final String DESCRIPTION = "description";

  /**
   * The constant MEMBER.
   */
  public static final String MEMBER = "member";

  /**
   * The constant NAME.
   */
  public static final String NAME = "name";

  /**
   * The constant OBJECT_SID.
   */
  public static final String OBJECT_SID = "objectSid";

  /**
   * The constant SAM_ACCOUNT_NAME.
   */
  public static final String SAM_ACCOUNT_NAME = "sAMAccountName";

  /**
   * The constant BINARY_ATTRIBUTES.
   */
  public static final String[] BINARY_ATTRIBUTES = {
      OBJECT_SID
  };

  private DomainGroupLdapConstants() {
  }
}
