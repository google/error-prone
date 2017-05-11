/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.tools.javac.util.Context;

/**
 * A view on top of a {@code Context} allowing additional modifications to be added without
 * affecting the underlying {@code Context}.
 *
 * @author Louis Wasserman
 */
public final class SubContext extends Context {
  private final Context base;

  public SubContext(Context base) {
    this.base = checkNotNull(base);
  }

  @Override
  public <T> T get(Key<T> key) {
    T result = super.get(key);
    return (result == null) ? base.get(key) : result;
  }

  @Override
  public <T> T get(Class<T> clazz) {
    T result = super.get(clazz);
    return (result == null) ? base.get(clazz) : result;
  }
}
