/*
 * Copyright 2019 the original author or authors.
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

package org.bremersee.dccon.config;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;

/**
 * The mail template resolver.
 *
 * @author Christian Bremer
 */
@Slf4j
public class MailTemplateResolver extends AbstractConfigurableTemplateResolver {

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  /**
   * Instantiates a new mail template resolver.
   */
  @SuppressWarnings("unused")
  public MailTemplateResolver() {
  }

  /**
   * Instantiates a new mail template resolver.
   *
   * @param resourceLoader the resource loader
   */
  @SuppressWarnings("WeakerAccess")
  public MailTemplateResolver(ResourceLoader resourceLoader) {
    if (resourceLoader != null) {
      this.resourceLoader = resourceLoader;
    }
    log.info("MailTemplateResolver (prefix={})", getPrefix());
  }

  @Override
  protected ITemplateResource computeTemplateResource(
      IEngineConfiguration configuration,
      String ownerTemplate,
      String template,
      String resourceName,
      String characterEncoding,
      Map<String, Object> templateResolutionAttributes) {

    return new MailTemplateResource(resourceName, characterEncoding, resourceLoader);
  }
}
