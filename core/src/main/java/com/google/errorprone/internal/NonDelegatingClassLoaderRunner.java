/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.internal;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * Executes a function inside a special classloader that searches its own resource path
 * <i>before</i> delegating to the runtime classpath. This allows error-prone's javac8 to be run on
 * JDK7, and is morally equivalent to invoking java with
 * {@code -Xbootclasspath/p:<path to javac.jar>}.
 *
 * @author cushon@google.com
 */
public class NonDelegatingClassLoaderRunner {
  public static <T, R> R run(T input, Class<R> outClass, String runnerClassName) {
    ClassLoader loader =
        NonDelegatingClassLoader.create(ImmutableSet.<String>of(Function.class.getName()));
    try {
      Class<?> runnerClass = Class.forName(runnerClassName, true, loader);
      @SuppressWarnings("unchecked")
      Function<T, R> runner = (Function<T, R>) Function.class.cast(runnerClass.newInstance());
      return runner.apply(input);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError("Unable to create runner.", e);
    }
  }
}
