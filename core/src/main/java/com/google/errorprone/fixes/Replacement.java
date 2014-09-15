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

package com.google.errorprone.fixes;

import com.google.auto.value.AutoValue;

/**
 * A single replaced section of a source file. When multiple replacements are to be made in a file,
 * these should be applied in reverse order of startPosition.
 * @author alexeagle@google.com (Alex Eagle)
 */
@AutoValue
public abstract class Replacement {
  public static Replacement create(int startPosition, int endPosition, String replaceWith) {
    return new AutoValue_Replacement(startPosition, endPosition, replaceWith);
  }

  // positions are character offset from beginning of the source file
  public abstract int startPosition();
  public abstract int endPosition();
  public abstract String replaceWith();
}
