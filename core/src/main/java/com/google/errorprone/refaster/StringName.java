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

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import javax.lang.model.element.Name;

/**
 * A simple wrapper to view a {@code String} as a {@code Name}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class StringName
    implements Name, Unifiable<Name>, Inlineable<com.sun.tools.javac.util.Name> {
  public static StringName of(CharSequence contents) {
    return new AutoValue_StringName(contents.toString());
  }

  abstract String contents();

  @Override
  public String toString() {
    return contents();
  }

  @Override
  public int length() {
    return contents().length();
  }

  @Override
  public char charAt(int index) {
    return contents().charAt(index);
  }

  @Override
  public CharSequence subSequence(int beginIndex, int endIndex) {
    return contents().subSequence(beginIndex, endIndex);
  }

  @Override
  public boolean contentEquals(@Nullable CharSequence cs) {
    return cs != null && contents().contentEquals(cs);
  }

  @Override
  public Choice<Unifier> unify(@Nullable Name target, Unifier unifier) {
    return Choice.condition(contentEquals(target), unifier);
  }

  @Override
  public com.sun.tools.javac.util.Name inline(Inliner inliner) {
    return inliner.asName(contents());
  }
}
