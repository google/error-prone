/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "RedundantThrows",
  summary = "Thrown exception is a subtype of another",
  category = JDK,
  severity = WARNING
)
public class RedundantThrows extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    List<? extends ExpressionTree> thrown = tree.getThrows();
    if (thrown.isEmpty()) {
      return NO_MATCH;
    }
    SetMultimap<Symbol, ExpressionTree> exceptionsBySuper = LinkedHashMultimap.create();
    for (ExpressionTree exception : thrown) {
      Type type = getType(exception);
      do {
        type = state.getTypes().supertype(type);
        exceptionsBySuper.put(type.tsym, exception);
      } while (!state.getTypes().isSameType(type, state.getSymtab().objectType));
    }
    Set<ExpressionTree> toRemove = new HashSet<>();
    List<String> messages = new ArrayList<>();
    for (ExpressionTree exception : thrown) {
      Symbol sym = getSymbol(exception);
      if (exceptionsBySuper.containsKey(sym)) {
        Set<ExpressionTree> sub = exceptionsBySuper.get(sym);
        messages.add(
            String.format(
                "%s %s of %s",
                oxfordJoin(", ", sub),
                sub.size() == 1 ? "is a subtype" : "are subtypes",
                sym.getSimpleName()));
        toRemove.addAll(sub);
      }
    }
    if (toRemove.isEmpty()) {
      return NO_MATCH;
    }
    // sort by order in input
    List<ExpressionTree> delete =
        ImmutableList.<ExpressionTree>copyOf(
            Iterables.filter(tree.getThrows(), Predicates.in(toRemove)));
    return buildDescription(delete.get(0))
        .setMessage("Redundant throws clause: " + oxfordJoin("; ", messages))
        .addFix(SuggestedFixes.deleteExceptions(tree, state, delete))
        .build();
  }

  static String oxfordJoin(String on, Iterable<?> pieces) {
    StringBuilder result = new StringBuilder();
    int size = Iterables.size(pieces);
    if (size == 2) {
      return Joiner.on(" and ").join(pieces);
    }
    int idx = 0;
    for (Object piece : pieces) {
      if (idx > 0) {
        result.append(on);
        if (idx == size - 1) {
          result.append("and ");
        }
      }
      result.append(piece);
      idx++;
    }
    return result.toString();
  }
}
