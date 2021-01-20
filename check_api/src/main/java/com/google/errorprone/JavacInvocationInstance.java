/*
 * Copyright 2019 The Error Prone Authors.
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
package com.google.errorprone;

import com.sun.tools.javac.util.Context;

/**
 * A token uniquely identifying a single invocation of javac. Any caches which might otherwise
 * persist indefinitely should be reset if they detect that the JavacInvocationInstance inside their
 * {@link Context} has changed. The only meaningful way to compare JavacInvocationInstance objects
 * is by their object identity, as they have no properties.
 */
public final class JavacInvocationInstance {
  public static JavacInvocationInstance instance(Context context) {
    JavacInvocationInstance instance = context.get(JavacInvocationInstance.class);
    if (instance == null) {
      instance = new JavacInvocationInstance();
      context.put(JavacInvocationInstance.class, instance);
    }
    return instance;
  }

  private JavacInvocationInstance() {}
}
