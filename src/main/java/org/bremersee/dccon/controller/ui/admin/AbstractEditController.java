package org.bremersee.dccon.controller.ui.admin;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public abstract class AbstractEditController extends AbstractController
    implements RedirectComponent {

  public AbstractEditController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver) {
    super(properties, localeResolver);
  }

  String entityNotFoundRedirect(
      RedirectAttributes redirectAttributes,
      String entityType,
      String i18nCode,
      String entityName,
      String redirect) {

    String msg = String.format("%s '%s' was not found.", entityType, entityName);
    RedirectMessage redirectMessage = getRedirectMessage(RedirectMessageType.WARNING, msg,
        i18nCode, String.valueOf(entityName));
    redirectAttributes.addFlashAttribute(RedirectMessage.ATTRIBUTE_NAME, redirectMessage);
    String redirectUri = getRedirectUri(redirect, PAGE_AND_OU_PARAMS, getParamterMap());
    logRedirectTo(msg, redirectUri);
    return redirectUri;
  }
}
