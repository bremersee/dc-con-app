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

import java.util.Locale;
import java.util.Optional;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

/**
 * The interface MessageProvider.
 *
 * @author Christian Bremer
 */
public interface MessageProvider extends MessageSourceAware {

  LocaleResolver getLocaleResolver();

  MessageSource getMessageSource();

  default Locale getResolvedLocale() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(attrs -> attrs instanceof ServletRequestAttributes)
        .map(attrs -> (ServletRequestAttributes) attrs)
        .map(ServletRequestAttributes::getRequest)
        .map(req -> getLocaleResolver().resolveLocale(req))
        .orElse(Locale.ENGLISH);
  }

  default String getMessage(String defaultMessage, String code, Object... args) {
    return getMessageSource().getMessage(code, args, defaultMessage, getResolvedLocale());
  }

  default RedirectMessage getRedirectMessage(RedirectMessageType type,
      String defaultMessage, String code, Object... args) {
    return new RedirectMessage(getMessage(defaultMessage, code, args), type);
  }

}
