/*
 * Copyright 2023 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.scanner;

import static com.google.common.collect.Lists.reverse;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An injector for ErrorProne.
 *
 * <p>This implements a very simplified subset of the functionality that Guice does. Specifically,
 * it allows injecting only non-generic classes, and treats everything as a singleton within a given
 * compilation.
 */
public final class ErrorProneInjector {
  private final ClassToInstanceMap<Object> instances = MutableClassToInstanceMap.create();

  /** Indicates that there was a runtime failure while providing an instance. */
  public static final class ProvisionException extends RuntimeException {
    public ProvisionException(String message) {
      super(message);
    }

    public ProvisionException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static ErrorProneInjector create() {
    return new ErrorProneInjector();
  }

  @CanIgnoreReturnValue
  public <T> ErrorProneInjector addBinding(Class<T> clazz, T instance) {
    instances.putInstance(clazz, instance);
    return this;
  }

  public synchronized <T> T getInstance(Class<T> clazz) {
    return getInstance(clazz, new ArrayList<>());
  }

  private synchronized <T> T getInstance(Class<T> clazz, List<Class<?>> path) {
    var instance = instances.getInstance(clazz);
    if (instance != null) {
      return instance;
    }
    path.add(clazz);
    Constructor<T> constructor =
        findConstructor(clazz)
            .orElseThrow(
                () ->
                    new ProvisionException(
                        "Failed to find an injectable constructor for "
                            + clazz.getCanonicalName()
                            + " requested by "
                            + printPath(path)));

    constructor.setAccessible(true);

    Object[] args =
        stream(constructor.getParameterTypes()).map(c -> getInstance(c, path)).toArray();
    T newInstance;
    try {
      newInstance = constructor.newInstance(args);
    } catch (ReflectiveOperationException e) {
      throw new ProvisionException("Failed to initialize " + clazz.getCanonicalName(), e);
    }
    instances.putInstance(clazz, newInstance);
    return newInstance;
  }

  public static <T> Optional<Constructor<T>> findConstructor(Class<T> clazz) {
    return findConstructorMatching(
            clazz,
            c ->
                stream(c.getAnnotations())
                    .anyMatch(a -> a.annotationType().getSimpleName().equals("Inject")))
        .or(
            () ->
                findConstructorMatching(
                    clazz,
                    c ->
                        c.getParameters().length != 0
                            && stream(c.getParameters())
                                .allMatch(p -> p.getType().equals(ErrorProneFlags.class))))
        .or(() -> findConstructorMatching(clazz, c -> c.getParameters().length == 0));
  }

  @SuppressWarnings("unchecked")
  private static <T> Optional<Constructor<T>> findConstructorMatching(
      Class<T> clazz, Predicate<Constructor<?>> predicate) {
    return stream(clazz.getDeclaredConstructors())
        .filter(predicate)
        .map(c -> (Constructor<T>) c)
        .findFirst();
  }

  private static String printPath(List<Class<?>> path) {
    return reverse(path).stream().map(Class::getSimpleName).collect(joining(" <- "));
  }
}
