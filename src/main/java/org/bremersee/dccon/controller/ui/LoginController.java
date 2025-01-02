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

package org.bremersee.dccon.controller.ui;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The type LoginController.
 *
 * @author Christian Bremer
 */
@Controller("loginController")
public class LoginController extends AbstractController {

  public LoginController(
      DomainControllerProperties domainControllerProperties,
      LocaleResolver localeResolver) {
    super(domainControllerProperties, localeResolver);
  }

  @RequestMapping(
      path = "/login",
      method = RequestMethod.GET)
  public String displayLoginView() {
    return "login";
  }

}
