/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testdata;

/**
 * Negative cases for {@link UnsafeReflectiveConstructionCast}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class UnsafeReflectiveConstructionCastNegativeCases {

  public String newInstanceDirectCall() throws Exception {
    return (String) Class.forName("java.lang.String").newInstance();
  }

  public String newInstanceDirectlyOnClassAndGetDeclaredConstructor() throws Exception {
    return (String) String.class.getDeclaredConstructor().newInstance();
  }

  public String newInstanceDirectlyOnClassAndNewInstance() throws Exception {
    return (String) String.class.newInstance();
  }

  public String invocationWithAsSubclass() throws Exception {
    return Class.forName("java.lang.String").asSubclass(String.class).newInstance();
  }

  public class Supplier<T> {
    public T get(String className) {
      try {
        return (T) Class.forName(className).getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
