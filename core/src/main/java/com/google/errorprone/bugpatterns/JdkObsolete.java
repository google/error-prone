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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "JdkObsolete",
  summary = "Suggests alternatives to obsolete JDK classes.",
  severity = WARNING
)
public class JdkObsolete extends BugChecker implements NewClassTreeMatcher, ClassTreeMatcher {

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
          .put("java.util.SortedSet", "SortedSet was replaced by NavigableSet in Java 6.")
          .put("java.util.SortedMap", "SortedMap was replaced by NavigableMap in Java 6.")
          .put(
              "java.util.Dictionary",
              "Dictionary is a nonstandard class that predate the Java Collections Framework;"
                  + " use LinkedHashMap.")
          .put("java.util.Enumeration", "Enumeration is an ancient precursor to Iterator.")
          .build();

  // a pre-JDK-8039124 concession
  static final Matcher<ExpressionTree> MATCHER_STRINGBUFFER =
      anyOf(
          instanceMethod()
              .onExactClass("java.util.regex.Matcher")
              .withSignature("appendTail(java.lang.StringBuffer)"),
          instanceMethod()
              .onExactClass("java.util.regex.Matcher")
              .withSignature("appendReplacement(java.lang.StringBuffer,java.lang.String)"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol constructor = ASTHelpers.getSymbol(tree);
    if (constructor == null) {
      return NO_MATCH;
    }
    Symbol owner = constructor.owner;
    Description description =
        describeIfObsolete(
            tree,
            owner.name.isEmpty()
                ? state.getTypes().directSupertypes(owner.asType())
                : ImmutableList.of(owner.asType()));
    if (description == NO_MATCH) {
      return NO_MATCH;
    }
    if (owner.getQualifiedName().contentEquals("java.lang.StringBuffer")) {
      boolean[] found = {false};
      new TreeScanner<Void, Void>() {
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
          if (MATCHER_STRINGBUFFER.matches(tree, state)) {
            found[0] = true;
          }
          return null;
        }
      }.scan(state.getPath().getCompilationUnit(), null);
      if (found[0]) {
        return NO_MATCH;
      }
    }
    return description;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return NO_MATCH;
    }
    return describeIfObsolete(tree, state.getTypes().directSupertypes(symbol.asType()));
  }

  private Description describeIfObsolete(Tree tree, Iterable<Type> types) {
    for (Type type : types) {
      String message = MATCHER.get(type.asElement().getQualifiedName().toString());
      if (message != null) {
        return buildDescription(tree).setMessage(message).build();
      }
    }
    return NO_MATCH;
  }
}
