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

package org.bremersee.dccon.controller;

import org.bremersee.exception.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The throw error controller to test the error handling of the angular web application.
 *
 * @author Christian Bremer
 */
@RestController
public class ThrowErrorController {

  /**
   * Throw error response entity.
   *
   * @return the response entity
   */
  @GetMapping(path = "/api/error", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<Void> throwError() {
    final RuntimeException cause = new RuntimeException("Example error message");
    throw new ServiceException(HttpStatus.I_AM_A_TEAPOT, "teapot", cause);
  }

}
