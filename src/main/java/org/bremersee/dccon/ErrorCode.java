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

  String EC_EMPTY_OU_RDN = EC_PREFIX + "e8d52299-664f-4585-a58f-29a69e579729";

  String EC_OU_NOT_FOUND = EC_PREFIX + "000f4ac1-8fea-4c4d-a13d-6f28d4987157";

  String EC_OU_ALREADY_EXISTS = EC_PREFIX + "93368104-44a7-47c8-8fdc-3faffd0ee0a6";

  String EC_ADDING_OU_FAILED = EC_PREFIX + "ec5b79b6-8c01-41b9-9ed9-60261affdaaf";

  String EC_DELETING_OU_FAILED = EC_PREFIX + "1344e1dc-6a00-4463-a95a-ae1d7f360341";

  String EC_PASSWORD_RESTRICTIONS = "check_password_restrictions";

  String EC_SAVING_PASSWORD_FAILED = EC_PREFIX + "3b5b0cae-d223-4077-944b-e978d415e7e2";

  String EC_SAM_ACCOUNT_NAME_REQUIRED = EC_PREFIX + "77ede234-5d01-492f-b6b0-9bfd3b762381";

  String EC_SAM_ACCOUNT_ALREADY_EXISTS = EC_PREFIX + "7bca7443-19f3-4d44-9607-118b10882b92";

  String EC_SAM_ACCOUNT_NOT_FOUND = EC_PREFIX + "f5aa86e6-7430-4f8f-a4cc-a27cdd77a8e7";

  String EC_MAX_MOCK_DATA = EC_PREFIX + "fad49270-8c66-4bff-b261-454e032e46a8";

  String EC_ADDING_USER_FAILED = EC_PREFIX + "216e1246-b464-48f1-ac88-20e8461dea1e";

  String EC_UPDATING_USER_FAILED = EC_PREFIX + "216e1246-b464-48f1-ac88-20e8461dea1e";

  String EC_SAVING_AVATAR_FAILED = EC_PREFIX + "4439f452-ede5-474b-a003-e74a53c2c854";

  String EC_DELETING_USER_FAILED = EC_PREFIX + "076c30f0-d695-46b1-a76d-d382cc7eec81";

  String EC_ADDING_GROUP_FAILED = EC_PREFIX + "7729c3c7-aeff-49f2-9243-dd5aee4b023a";

  String EC_DELETING_GROUP_FAILED = EC_PREFIX + "28f610a5-1679-47d9-8f90-2a4d75882d52";

}
