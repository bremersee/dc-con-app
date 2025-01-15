package org.bremersee.dccon.controller.ui.admin;

import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.controller.ui.AbstractController;
import org.bremersee.dccon.controller.ui.components.RedirectComponent;
import org.bremersee.dccon.controller.ui.model.RedirectMessage;
import org.bremersee.dccon.controller.ui.model.RedirectMessageType;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.LocaleResolver;

public abstract class AbstractEditController extends AbstractController
    implements RedirectComponent {

  public AbstractEditController(
      DomainControllerProperties properties,
      LocaleResolver localeResolver) {
    super(properties, localeResolver);
  }

  String entityNotFoundRedirect(
      ModelMap model,
      String entityType,
      String i18nCode,
      String entityName,
      String redirect) {

    String msg = String.format("%s '%s' was not found.", entityType, entityName);
    RedirectMessage redirectMessage = getRedirectMessage(RedirectMessageType.WARNING, msg,
        i18nCode, String.valueOf(entityName));
    // TODO hier fehlen die redirect attributes
    model.addAttribute(RedirectMessage.ATTRIBUTE_NAME, redirectMessage);
    String redirectUri = getRedirectUri(redirect, PAGE_AND_OU_PARAMS, getParamterMap());
    logRedirectTo(msg, redirectUri);
    return redirectUri;
  }
}
