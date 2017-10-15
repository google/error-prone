/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "JdkObsolete",
  summary = "Suggests alternatives to obsolete JDK classes.",
  severity = WARNING
)
public class JdkObsolete extends BugChecker implements NewClassTreeMatcher {

  static final ImmutableMap<String, String> MATCHER =
      ImmutableMap.<String, String>builder()
          .put(
              "java.util.LinkedList",
              "It is very rare for LinkedList to out-perform ArrayList or ArrayDeque. Avoid it"
                  + " unless you're willing to invest a lot of time into benchmarking.")
          .put(
              "java.util.Vector",
              "Vector performs synchronization that is usually unnecessary; prefer ArrayList.")
          .put(
              "java.util.Hashtable",
              "Hashtable performs synchronization this is usually unnecessary; prefer"
                  + " LinkedHashMap.")
          .put(
              "java.util.Stack",
              "Stack is a nonstandard class that predates the Java Collections Framework; prefer"
                  + " ArrayDeque.")
          .put(
              "java.lang.StringBuffer",
              "StringBuffer performs synchronization that is usually unnecessary;"
                  + " prefer StringBuilder.")
          .build();

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol constructor = ASTHelpers.getSymbol(tree);
    if (constructor == null) {
      return NO_MATCH;
    }
    String message = MATCHER.get(constructor.owner.getQualifiedName().toString());
    if (message == null) {
      return NO_MATCH;
    }
    return buildDescription(tree).setMessage(message).build();
  }
}
