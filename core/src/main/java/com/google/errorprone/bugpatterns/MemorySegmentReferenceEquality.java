/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Do not compare MemorySegments using reference equality.", severity = ERROR)
public final class MemorySegmentReferenceEquality extends AbstractReferenceEquality {

  private static final Supplier<Type> MEMORY_SEGMENT_TYPE =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.foreign.MemorySegment"));

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    Type type = getType(tree);
    Type memorySegmentType = MEMORY_SEGMENT_TYPE.get(state);
    return (type == null || memorySegmentType == null)
        ? false
        : isSubtype(type, memorySegmentType, state);
  }
}
