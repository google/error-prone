/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.predicates.TypePredicates.isExactTypeAny;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "Incomparable",
    summary = "Types contained in sorted collections must implement Comparable.",
    severity = ERROR)
public class Incomparable extends BugChecker implements NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      anyOf(
          constructor()
              .forClass(
                  isExactTypeAny(
                      ImmutableList.of(
                          "java.util.TreeMap", "java.util.concurrent.ConcurrentSkipListMap")))
              .withParameters("java.util.Map"),
          constructor()
              .forClass(
                  isExactTypeAny(
                      ImmutableList.of(
                          "java.util.TreeSet", "java.util.concurrent.ConcurrentSkipListSet")))
              .withParameters("java.util.Set"),
          constructor()
              .forClass(
                  isExactTypeAny(
                      ImmutableList.of(
                          "java.util.TreeMap",
                          "java.util.TreeSet",
                          "java.util.concurrent.ConcurrentSkipListMap",
                          "java.util.concurrent.ConcurrentSkipListSet")))
              .withParameters());

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    Type type;
    ASTHelpers.TargetType targetType = ASTHelpers.targetType(state);
    if (targetType != null) {
      type = targetType.type();
    } else {
      type = getType(tree.getIdentifier());
    }
    List<Type> typeArguments = type.getTypeArguments();
    if (typeArguments.isEmpty()) {
      return NO_MATCH;
    }
    Type keyType = typeArguments.get(0);
    if (ASTHelpers.isCastable(keyType, state.getSymtab().comparableType, state)) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(String.format("%s does not implement Comparable", keyType))
        .build();
  }
}
