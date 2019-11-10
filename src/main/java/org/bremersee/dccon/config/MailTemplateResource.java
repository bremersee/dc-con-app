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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.thymeleaf.templateresource.ITemplateResource;

/**
 * @author Christian Bremer
 */
@Slf4j
public class MailTemplateResource implements ITemplateResource {

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  private String path;

  private String characterEncoding = StandardCharsets.UTF_8.name();

  public MailTemplateResource(String path) {
    this(path, null, null);
  }

  public MailTemplateResource(
      String path,
      String characterEncoding) {
    this(path, characterEncoding, null);
  }

  public MailTemplateResource(
      String path,
      ResourceLoader resourceLoader) {
    this(path, null, resourceLoader);
  }

  public MailTemplateResource(
      String path,
      String characterEncoding,
      ResourceLoader resourceLoader) {
    this.path = path;
    setCharacterEncoding(characterEncoding);
    setResourceLoader(resourceLoader);
    log.info("MailTemplateResource (path={}, characterEncoding={})",
        this.path, this.characterEncoding);
  }

  public void setResourceLoader(ResourceLoader resourceLoader) {
    if (resourceLoader != null) {
      this.resourceLoader = resourceLoader;
    }
  }

  public void setCharacterEncoding(String characterEncoding) {
    if (StringUtils.hasText(characterEncoding)) {
      this.characterEncoding = characterEncoding;
    }
  }

  @Override
  public String getDescription() {
    return path;
  }

  @Override
  public String getBaseName() {
    if (path == null || path.length() == 0) {
      return null;
    }
    final String basePath = (path.charAt(path.length() - 1) == '/'
        ? path.substring(0, path.length() - 1)
        : path);
    final int slashPos = basePath.lastIndexOf('/');
    if (slashPos != -1) {
      final int dotPos = basePath.lastIndexOf('.');
      if (dotPos != -1 && dotPos > slashPos + 1) {
        return basePath.substring(slashPos + 1, dotPos);
      }
      return basePath.substring(slashPos + 1);
    } else {
      final int dotPos = basePath.lastIndexOf('.');
      if (dotPos != -1) {
        return basePath.substring(0, dotPos);
      }
    }
    return (basePath.length() > 0 ? basePath : null);
  }

  @Override
  public boolean exists() {
    return resourceLoader.getResource(path).exists();
  }

  @Override
  public Reader reader() throws IOException {
    return new BufferedReader(
        new InputStreamReader(
            new BufferedInputStream(
                resourceLoader.getResource(path).getInputStream()), characterEncoding));
  }

  @Override
  public ITemplateResource relative(String relativeLocation) {
    Assert.hasText(relativeLocation, "Relative Path cannot be null or empty.");
    final String newPath = computeRelativeLocation(path, relativeLocation);
    return new MailTemplateResource(newPath, characterEncoding, resourceLoader);
  }

  private static String computeRelativeLocation(final String location,
      final String relativeLocation) {
    final int separatorPos = location.lastIndexOf('/');
    if (separatorPos != -1) {
      final StringBuilder relativeBuilder = new StringBuilder(
          location.length() + relativeLocation.length());
      relativeBuilder.append(location, 0, separatorPos);
      if (relativeLocation.charAt(0) != '/') {
        relativeBuilder.append('/');
      }
      relativeBuilder.append(relativeLocation);
      return relativeBuilder.toString();
    }
    return relativeLocation;
  }

}
