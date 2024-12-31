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

package org.bremersee.dccon.repository.automock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * The mock aspect.
 *
 * @author Christian Bremer
 */
@Profile("mock")
@Component
@Aspect
@Setter
@Slf4j
public class MockAspect implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  public MockAspect() {
    log.info("Mock aspect is available.");
  }

  @Around("execution(* org.bremersee.dccon.repository.*Impl.*(..))")
  public Object invokeMockIfRequired(ProceedingJoinPoint joinPoint) throws Throwable {
    log.debug("Invoke mock if required with {}", joinPoint);
    if (areAllProfilesEnabled(getRequiredProfiles(joinPoint))) {
      log.debug("Invoking mock is not required.");
      return joinPoint.proceed();
    }
    try {
      return findMock(joinPoint)
          .flatMap(mock -> findMethod(joinPoint)
              .map(method -> {
                Class<?> mockCls = ClassUtils.getUserClass(mock);
                Method mockMethod = ReflectionUtils
                    .findMethod(mockCls, method.getName(), method.getParameterTypes());
                if (!ObjectUtils.isEmpty(mockMethod)) {
                  log.warn("Using method '{}' of mock '{}'.",
                      method.getName(), mockCls.getSimpleName());
                }
                return mockMethod;
              })
              .map(mockMethod -> ReflectionUtils.invokeMethod(mockMethod, mock,
                  joinPoint.getArgs())))
          .orElseThrow(MockNotFoundException::new);

    } catch (MockNotFoundException e) {
      log.debug("No mock found for method '{}'.", joinPoint.getSignature().getName());
      return joinPoint.proceed();
    }
  }

  private boolean areAllProfilesEnabled(String[] requiredProfiles) {
    if (ObjectUtils.isEmpty(requiredProfiles)) {
      return true;
    }
    Set<String> activeProfiles = getActiveProfiles();
    log.debug("Required profiles: {}", Arrays.toString(requiredProfiles));
    log.debug("Active profiles: {}", activeProfiles);
    return Arrays.stream(requiredProfiles)
        .allMatch(activeProfiles::contains);
  }

  private Set<String> getActiveProfiles() {
    return Arrays.stream(applicationContext.getEnvironment().getActiveProfiles())
        .collect(Collectors.toSet());
  }

  private Optional<Method> findMethod(ProceedingJoinPoint joinPoint) {
    return Optional.ofNullable(joinPoint.getSignature())
        .filter(signature -> signature instanceof MethodSignature)
        .map(signature -> (MethodSignature) signature)
        .map(MethodSignature::getMethod)
        .filter(method -> isMethodOf(method, joinPoint));
  }

  private String[] getRequiredProfiles(ProceedingJoinPoint joinPoint) {
    return findMethod(joinPoint)
        .map(method -> AnnotationUtils.findAnnotation(method, NoProfileRequired.class))
        .map(noProfileAnnotation -> new String[0])
        .or(() -> findMethod(joinPoint)
            .map(method -> AnnotationUtils.findAnnotation(method, ProfileRequired.class))
            .map(ProfileRequired::value)
            .filter(profiles -> profiles.length > 0))
        .or(() -> Optional.ofNullable(joinPoint.getSignature())
            .filter(signature -> signature instanceof MethodSignature)
            .map(Signature::getDeclaringType)
            .map(cls -> AnnotationUtils.findAnnotation(cls, ProfileRequired.class))
            .map(ProfileRequired::value)
            .filter(profiles -> profiles.length > 0))
        .orElse(new String[0]);
  }

  private Optional<MockComponent> findMockComponent(ProceedingJoinPoint joinPoint) {
    return Optional.ofNullable(joinPoint.getSignature())
        .map(Signature::getDeclaringType)
        .map(cls -> AnnotationUtils.findAnnotation(cls, MockComponent.class));
  }

  private Optional<Object> findMock(ProceedingJoinPoint joinPoint) {
    return findMockComponent(joinPoint)
        .map(MockComponent::value)
        .map(mockCls -> {
          try {
            return applicationContext.getBean(mockCls);
          } catch (BeansException e) {
            return null;
          }
        });
  }

  private boolean isMethodOf(Method method, ProceedingJoinPoint joinPoint) {
    Class<?>[] classes = findMockComponent(joinPoint)
        .map(MockComponent::methodsOf)
        .orElse(new Class<?>[0]);
    if (classes.length == 0) {
      return true;
    }
    return Arrays.stream(classes)
        .anyMatch(cls -> isMethodOf(method, cls));
  }

  private boolean isMethodOf(Method method, Class<?> cls) {
    return Objects.nonNull(ReflectionUtils
        .findMethod(cls, method.getName(), method.getParameterTypes()));
  }

}
