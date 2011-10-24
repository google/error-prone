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

import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Matches any tree, but stores whether or not the given Matcher would match the tree into a mutable
 * boolean.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class StoreToBoolean<T extends Tree> implements Matcher<T> {
  private final AtomicBoolean aBoolean;
  private final Matcher<T> matcher;

  public StoreToBoolean(AtomicBoolean aBoolean, Matcher<T> matcher) {
    this.aBoolean = aBoolean;
    this.matcher = matcher;
  }

  @Override
  public boolean matches(T t, VisitorState state) {
    aBoolean.set(matcher.matches(t, state));
    return true;
  }
}
