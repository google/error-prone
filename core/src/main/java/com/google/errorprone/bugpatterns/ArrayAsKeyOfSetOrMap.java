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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.sun.tools.javac.code.Type;

/**
 * Warns that users should not have an array as a key to a Set or Map
 *
 * @author siyuanl@google.com (Siyuan Liu)
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(
    summary =
        "Arrays do not override equals() or hashCode, so comparisons will be done on"
            + " reference equality only. If neither deduplication nor lookup are needed, "
            + "consider using a List instead. Otherwise, use IdentityHashMap/Set, "
            + "a Map from a library that handles object arrays, or an Iterable/List of pairs.",
    severity = WARNING)
public class ArrayAsKeyOfSetOrMap extends AbstractAsKeyOfSetOrMap {

  @Override
  protected boolean isBadType(Type type, VisitorState state) {
    return type instanceof Type.ArrayType;
  }
}
