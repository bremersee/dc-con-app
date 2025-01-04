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

package org.bremersee.dccon.service;

import static org.springframework.util.ObjectUtils.isEmpty;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import java.util.HashMap;
import java.util.Map;
import org.bremersee.dccon.ErrorCode;
import org.bremersee.dccon.config.DomainControllerProperties;
import org.bremersee.dccon.repository.DomainRepository;
import org.bremersee.exception.ServiceException;
import org.springframework.stereotype.Component;

/**
 * The type MustacheTemplateEngine.
 *
 * @author Christian Bremer
 */
@Component("mustacheTemplateEngine")
public class MustacheTemplateEngine implements TemplateEngine {

  private final DomainControllerProperties properties;

  private final DomainRepository domainRepository;

  public MustacheTemplateEngine(
      DomainControllerProperties properties,
      DomainRepository domainRepository) {
    this.properties = properties;
    this.domainRepository = domainRepository;
  }

  @Override
  public String compileAndExecute(String template, Map<String, Object> model) {
    if (isEmpty(template) || (!template.contains("{{") && !template.contains("}}"))) {
      return template;
    }
    Map<String, Object> map = isEmpty(model) ? new HashMap<>() : new HashMap<>(model);
    map.put("properties", properties);
    map.put("domain", domainRepository);
    try {
      return Mustache
          .compiler()
          .escapeHTML(false)
          .nullValue("")
          .compile(template)
          .execute(map);

    } catch (MustacheException.Context mc) {
      throw new TemplateEngineException(mc.getMessage(), mc.key, mc.lineNo, mc);
    } catch (RuntimeException re) {
      throw ServiceException.internalServerError(
          String.format("Processing template failed (template = '%s').", template),
          ErrorCode.EC_TEMPLATE_FAILED,
          re);
    }
  }

}
