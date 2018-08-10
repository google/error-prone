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
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Type;

/**
 * Check for usage of {@code Set<Proto>} or {@code Map<Proto, E>}.
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
@BugPattern(
    name = "ProtosAsKeyOfSetOrMap",
    summary =
        "Protos should not be used as a key to a map, in a set, or in a contains method on a "
            + "descendant of a collection. Protos have non deterministic ordering and proto "
            + "equality is deep, which is a performance issue.",
    severity = WARNING)
public class ProtosAsKeyOfSetOrMap extends AbstractAsKeyOfSetOrMap {

  @Override
  protected boolean isBadType(Type type, VisitorState state) {
    return ASTHelpers.isSubtype(
        type, state.getTypeFromString("com.google.protobuf.GeneratedMessage"), state);
  }
}
