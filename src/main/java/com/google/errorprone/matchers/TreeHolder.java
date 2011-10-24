/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.sun.source.tree.Tree;

/**
 * A substitute for pass-by-reference, allowing a Tree method parameter to act as a return value.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class TreeHolder<T extends Tree> {
  private T value;

  public TreeHolder(T value) {
    this.value = value;
  }

  public TreeHolder() {
  }

  public void set(T value) {
    this.value = value;
  }

  public T get() {
    return value;
  }

  public static <T extends Tree> TreeHolder<T> create() {
    return new TreeHolder<T>();
  }
}
