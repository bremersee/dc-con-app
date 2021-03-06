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

import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The async and scheduling configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration { // implements AsyncConfigurer {

  MessageSourceAutoConfiguration h;

  /*
  @Override
  public Executor getAsyncExecutor() {
    return new ThreadPoolTaskExecutor(); // This is to simple, throws Thread pool not initialized.
  }
  */

}
