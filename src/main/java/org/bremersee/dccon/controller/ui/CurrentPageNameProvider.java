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

import org.bremersee.dccon.controller.ControllerConstants;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * The interface CurrentPageProvider.
 *
 * @author Christian Bremer
 */
public interface CurrentPageNameProvider extends ControllerConstants {

  String getCurrentPageName();

  @ModelAttribute(CURRENT_PAGE_NAME)
  default String addCurrentPageName() {
    return getCurrentPageName();
  }

}
