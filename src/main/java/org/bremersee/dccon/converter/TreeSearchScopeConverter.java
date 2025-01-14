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

package org.bremersee.dccon.converter;

import static java.util.Objects.isNull;

import org.bremersee.dccon.model.TreeSearchScope;
import org.ldaptive.SearchScope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * The tree search scope converter.
 *
 * @author Christian Bremer
 */
@Component
public class TreeSearchScopeConverter implements Converter<String, TreeSearchScope> {

  @Override
  public TreeSearchScope convert(@NonNull String source) {
    return TreeSearchScope.fromValue(source);
  }

  public static SearchScope toSearchScope(TreeSearchScope scope) {
    if (isNull(scope)) {
      return null;
    }
    try {
      return SearchScope.valueOf(scope.name().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static TreeSearchScope fromSearchScope(SearchScope scope) {
    if (isNull(scope)) {
      return null;
    }
    return TreeSearchScope.fromValue(scope.name());
  }
}
