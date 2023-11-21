/*
 * Copyright 2023 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "StringCharset",
    severity = WARNING,
    explanation = "Prefer StandardCharsets over using string names for charsets")
public class StringCharset extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> CONSTRUCTOR_MATCHER =
      constructor()
          .forClass("java.lang.String")
          .withParametersOfType(
              ImmutableList.of(Suppliers.arrayOf(Suppliers.BYTE_TYPE), Suppliers.STRING_TYPE));

  private static final Matcher<ExpressionTree> METHOD_MATCHER =
      instanceMethod()
          .onExactClass("java.lang.String")
          .named("getBytes")
          .withParameters("java.lang.String");

  private static final ImmutableSet<Charset> CHARSETS =
      ImmutableSet.of(
          StandardCharsets.UTF_8,
          StandardCharsets.ISO_8859_1,
          StandardCharsets.US_ASCII,
          StandardCharsets.UTF_16,
          StandardCharsets.UTF_16BE,
          StandardCharsets.UTF_16LE);

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!METHOD_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    return match(tree.getArguments().get(0), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!CONSTRUCTOR_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    return match(tree.getArguments().get(1), state);
  }

  private Description match(ExpressionTree tree, VisitorState state) {
    String value = ASTHelpers.constValue(tree, String.class);
    if (value == null) {
      return NO_MATCH;
    }
    Charset charset;
    try {
      charset = Charset.forName(value);
    } catch (IllegalArgumentException e) {
      return buildDescription(tree)
          .setMessage(String.format("%s is not a valid charset", value))
          .build();
    }
    if (!CHARSETS.contains(charset)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        tree,
        SuggestedFixes.qualifyStaticImport(
            "java.nio.charset.StandardCharsets." + charset.name().replace('-', '_'), fix, state));
    return describeMatch(tree, fix.build());
  }
}
