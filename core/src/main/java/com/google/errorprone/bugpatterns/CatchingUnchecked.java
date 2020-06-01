/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isCheckedExceptionType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers.ScanThrownTypes;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.TryTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UnionClassType;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Flags code which catches {@link RuntimeException}s under the guise of catching {@link Exception}.
 */
@BugPattern(
    name = "CatchingUnchecked",
    summary =
        "This catch block catches `Exception`, but can only catch unchecked exceptions. Consider"
            + " catching RuntimeException (or something more specific) instead so it is more"
            + " apparent that no checked exceptions are being handled.",
    severity = SeverityLevel.WARNING)
public final class CatchingUnchecked extends BugChecker implements TryTreeMatcher {

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    ScanThrownTypes scanner = new ScanThrownTypes(state);
    scanner.scanResources(tree);
    scanner.scan(tree.getBlock(), null);
    Set<Type> thrownExceptions = new HashSet<>(scanner.getThrownTypes());
    for (CatchTree catchTree : tree.getCatches()) {
      if (isSameType(getType(catchTree.getParameter()), state.getSymtab().exceptionType, state)) {
        if (thrownExceptions.stream().noneMatch(t -> isCheckedExceptionType(t, state))) {
          state.reportMatch(
              describeMatch(
                  catchTree,
                  SuggestedFix.replace(catchTree.getParameter().getType(), "RuntimeException")));
        }
      }

      ImmutableList<Type> caughtTypes = extractTypes(getType(catchTree.getParameter()));
      thrownExceptions.removeIf(t -> caughtTypes.stream().anyMatch(ct -> isSubtype(t, ct, state)));
    }
    return NO_MATCH;
  }

  private static ImmutableList<Type> extractTypes(@Nullable Type type) {
    if (type == null) {
      return ImmutableList.of();
    }
    if (type.isUnion()) {
      return ImmutableList.copyOf(((UnionClassType) type).getAlternativeTypes());
    }
    return ImmutableList.of(type);
  }
}
