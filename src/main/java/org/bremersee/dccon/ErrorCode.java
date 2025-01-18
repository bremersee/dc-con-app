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

package org.bremersee.dccon;

/**
 * The type ErrorCode.
 *
 * @author Christian Bremer
 */
public interface ErrorCode {

  String EC_PREFIX = "org.bremersee:dc-con-app:";

  String EC_TEMPLATE_WITH_MISSING_KEY = EC_PREFIX + "20257abd-f480-4187-8156-3890b985b6e9";

  String EC_TEMPLATE_FAILED = EC_PREFIX + "bd6bb230-56fe-4c2d-9b0c-d8b2af91f073";

  String EC_ILLEGAL_DN = EC_PREFIX + "33cd9d6a-51ed-4124-921c-ea81962b8c74";

  String EC_ILLEGAL_SYSTEM_ENTITY_OPERATION = EC_PREFIX + "30c87232-ac6e-4530-b36e-5c58ece4d9e8";

  String EC_EMPTY_OU_RDN = EC_PREFIX + "e8d52299-664f-4585-a58f-29a69e579729";

  String EC_OU_NOT_FOUND = EC_PREFIX + "000f4ac1-8fea-4c4d-a13d-6f28d4987157";

  String EC_OU_NAME_REQUIRED = EC_PREFIX + "3ea46148-d4df-46fd-b4f6-099241868428";

  String EC_ILLEGAL_OU_NAME = EC_PREFIX + "ba5e538a-aaeb-480f-a066-8bc8ee362829";

  String EC_OU_ALREADY_EXISTS = EC_PREFIX + "93368104-44a7-47c8-8fdc-3faffd0ee0a6";

  String EC_ADDING_OU_FAILED = EC_PREFIX + "ec5b79b6-8c01-41b9-9ed9-60261affdaaf";

  String EC_UPDATING_OU_FAILED = EC_PREFIX + "91b7b4bb-59ca-4fa8-a4b2-e46e001bd542";

  String EC_DELETING_OU_FAILED = EC_PREFIX + "1344e1dc-6a00-4463-a95a-ae1d7f360341";

  String EC_DN_ALREADY_EXISTS = EC_PREFIX + "4e04709a-db4f-40bc-a193-11c75a4077ac";

  String EC_PASSWORD_RESTRICTIONS = "check_password_restrictions";

  String EC_SAVING_PASSWORD_FAILED = EC_PREFIX + "3b5b0cae-d223-4077-944b-e978d415e7e2";

  String EC_SAM_ACCOUNT_NAME_REQUIRED = EC_PREFIX + "77ede234-5d01-492f-b6b0-9bfd3b762381";

  String EC_ILLEGAL_SAM_ACCOUNT_NAME = EC_PREFIX + "8c9037dc-132e-4960-b2ad-fca9aae8b1c8";

  String EC_SAM_ACCOUNT_ALREADY_EXISTS = EC_PREFIX + "7bca7443-19f3-4d44-9607-118b10882b92";

  String EC_SAM_ACCOUNT_NOT_FOUND = EC_PREFIX + "f5aa86e6-7430-4f8f-a4cc-a27cdd77a8e7";

  String EC_MAX_MOCK_DATA = EC_PREFIX + "fad49270-8c66-4bff-b261-454e032e46a8";

  String EC_UID_ALREADY_EXISTS = EC_PREFIX + "a75519c6-9f25-4a9f-8be6-6c144f169e57";

  String EC_UID_NUMBER_ALREADY_EXISTS = EC_PREFIX + "76b3ad40-95ac-46e6-9f44-11911e026f5e";

  String EC_PRINCIPAL_ALREADY_EXISTS = EC_PREFIX + "8dfe1a27-770b-409d-a554-874d0a278dd7";

  String EC_ILLEGAL_FIRST_NAME = EC_PREFIX + "a51e2c02-0a52-4ca3-b507-b3a4ba739cf2";

  String EC_ILLEGAL_LAST_NAME = EC_PREFIX + "23f8c2af-dea7-494a-a295-269a18ff7dbe";

  String EC_ADDING_USER_FAILED = EC_PREFIX + "216e1246-b464-48f1-ac88-20e8461dea1e";

  String EC_UPDATING_USER_FAILED = EC_PREFIX + "4f4ce2f6-5db2-43f6-956a-7efe24743b49";

  String EC_SAVING_AVATAR_FAILED = EC_PREFIX + "4439f452-ede5-474b-a003-e74a53c2c854";

  String EC_DELETING_USER_FAILED = EC_PREFIX + "076c30f0-d695-46b1-a76d-d382cc7eec81";

  String EC_GID_NUMBER_ALREADY_EXISTS = EC_PREFIX + "192ae1b7-c831-44ed-b50b-bb6979e28c4b";

  String EC_ADDING_GROUP_FAILED = EC_PREFIX + "7729c3c7-aeff-49f2-9243-dd5aee4b023a";

  String EC_UPDATING_GROUP_FAILED = EC_PREFIX + "0624062c-0fe9-45ab-a1c7-b48ef2666f8d";

  String EC_DELETING_GROUP_FAILED = EC_PREFIX + "28f610a5-1679-47d9-8f90-2a4d75882d52";

  String EC_CREATING_DNS_ZONE_FAILED = EC_PREFIX + "78e3bbaf-9b51-4442-a1e6-791d7fbc1032";

  String EC_DELETING_DNS_ZONE_FAILED = EC_PREFIX + "496a3bb3-92a0-406f-8bb1-8a3d43619ea9";

  String EC_ADDING_DNS_ENTRY_FAILED = EC_PREFIX + "72ca0002-caf1-4bc8-aac6-eb04d0b558f4";

  String EC_DNS_ENTRY_NOT_FOUND = EC_PREFIX + "89e502bb-2303-4524-8378-327d601b5727";

  String EC_UPDATING_DNS_ENTRY_FAILED = EC_PREFIX + "bdfc6d4c-40a1-4ac0-ac04-1792748e696b";

  String EC_DELETING_DNS_ENTRY_FAILED = EC_PREFIX + "69fcdc93-c029-496f-840f-9bf443a18a3b";

  String EC_ILLEGAL_DNS_ENTRY_TYPE = EC_PREFIX + "69fcdc93-c029-496f-840f-9bf443a18a3b";

}
