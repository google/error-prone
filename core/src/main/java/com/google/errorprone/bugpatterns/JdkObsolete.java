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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
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
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Optional;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "JdkObsolete",
  summary = "Suggests alternatives to obsolete JDK classes.",
  severity = WARNING
)
public class JdkObsolete extends BugChecker implements NewClassTreeMatcher, ClassTreeMatcher {

  static class Obsolete {
    final String qualifiedName;
    final String message;

    Obsolete(String qualifiedName, String message) {
      this.qualifiedName = qualifiedName;
      this.message = message;
    }

    String qualifiedName() {
      return qualifiedName;
    }

    String message() {
      return message;
    }

    Optional<Fix> fix(VisitorState state) {
      return Optional.empty();
    }
  }

  static final ImmutableMap<String, Obsolete> OBSOLETE =
      ImmutableList.of(
              new Obsolete(
                  "java.util.LinkedList",
                  "It is very rare for LinkedList to out-perform ArrayList or ArrayDeque. Avoid it"
                      + " unless you're willing to invest a lot of time into benchmarking.") {
                @Override
                Optional<Fix> fix(VisitorState state) {
                  return linkedListFix(state);
                }
              },
              new Obsolete(
                  "java.util.Vector",
                  "Vector performs synchronization that is usually unnecessary; prefer ArrayList."),
              new Obsolete(
                  "java.util.Hashtable",
                  "Hashtable performs synchronization this is usually unnecessary; prefer"
                      + " LinkedHashMap."),
              new Obsolete(
                  "java.util.Stack",
                  "Stack is a nonstandard class that predates the Java Collections Framework;"
                      + " prefer ArrayDeque."),
              new Obsolete(
                  "java.lang.StringBuffer",
                  "StringBuffer performs synchronization that is usually unnecessary;"
                      + " prefer StringBuilder.") {
                @Override
                Optional<Fix> fix(VisitorState state) {
                  return stringBufferFix(state);
                }
              },
              new Obsolete(
                  "java.util.SortedSet", "SortedSet was replaced by NavigableSet in Java 6."),
              new Obsolete(
                  "java.util.SortedMap", "SortedMap was replaced by NavigableMap in Java 6."),
              new Obsolete(
                  "java.util.Dictionary",
                  "Dictionary is a nonstandard class that predate the Java Collections Framework;"
                      + " use LinkedHashMap."),
              new Obsolete(
                  "java.util.Enumeration", "Enumeration is an ancient precursor to Iterator."))
          .stream()
          .collect(toImmutableMap(Obsolete::qualifiedName, x -> x));

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
                : ImmutableList.of(owner.asType()),
            state);
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
    return describeIfObsolete(tree, state.getTypes().directSupertypes(symbol.asType()), state);
  }

  private Description describeIfObsolete(Tree tree, Iterable<Type> types, VisitorState state) {
    for (Type type : types) {
      Obsolete obsolete = OBSOLETE.get(type.asElement().getQualifiedName().toString());
      if (obsolete == null) {
        continue;
      }
      Description.Builder description = buildDescription(tree).setMessage(obsolete.message());
      obsolete.fix(state).ifPresent(description::addFix);
      return description.build();
    }
    return NO_MATCH;
  }

  // rewrite e.g. `List<Object> xs = new LinkedList<>()` -> `... = new ArrayList<>()`
  private static Optional<Fix> linkedListFix(VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof VariableTree)) {
      return Optional.empty();
    }
    VariableTree varTree = (VariableTree) parent;
    if (!(varTree.getInitializer() instanceof NewClassTree)) {
      return Optional.empty();
    }
    NewClassTree init = (NewClassTree) varTree.getInitializer();
    Type varType = ASTHelpers.getType(parent);
    Types types = state.getTypes();
    for (String replacement : ImmutableList.of("java.util.ArrayList", "java.util.ArrayDeque")) {
      Symbol sym = state.getSymbolFromString(replacement);
      if (types.isAssignable(types.erasure(sym.asType()), types.erasure(varType))) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        Tree typeTree = init.getIdentifier();
        if (typeTree instanceof ParameterizedTypeTree) {
          typeTree = ((ParameterizedTypeTree) typeTree).getType();
        }
        fix.replace(typeTree, SuggestedFixes.qualifyType(state, fix, sym));
        return Optional.of(fix.build());
      }
    }
    return Optional.empty();
  }

  // Rewrite StringBuffers that are immediately assigned to a variable which does not escape the
  // current method.
  private static Optional<Fix> stringBufferFix(VisitorState state) {
    Tree tree = state.getPath().getLeaf();
    // expect `new StringBuffer()`
    if (!(tree instanceof NewClassTree)) {
      return Optional.empty();
    }
    // expect e.g. `StringBuffer sb = new StringBuffer();`
    NewClassTree newClassTree = (NewClassTree) tree;
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof VariableTree)) {
      return Optional.empty();
    }
    VariableTree varTree = (VariableTree) parent;
    VarSymbol varSym = ASTHelpers.getSymbol(varTree);
    TreePath methodPath = findEnclosingMethod(state);
    if (methodPath == null) {
      return Optional.empty();
    }
    // Expect all uses to be of the form `sb.<method>` (append, toString, etc.)
    // We don't want to refactor StringBuffers that escape the current method.
    // Use an array to get a boxed boolean that we can update in the anonymous class.
    boolean[] escape = {false};
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (varSym.equals(ASTHelpers.getSymbol(tree))) {
          Tree parent = getCurrentPath().getParentPath().getLeaf();
          if (parent == varTree) {
            // the use of the variable in its declaration gets a pass
            return null;
          }
          // the LHS of a select (e.g. in `sb.append(...)`) does not escape
          if (!(parent instanceof MemberSelectTree)
              || ((MemberSelectTree) parent).getExpression() != tree) {
            escape[0] = true;
          }
        }
        return null;
      }
    }.scan(methodPath, null);
    if (escape[0]) {
      return Optional.empty();
    }
    return Optional.of(
        SuggestedFix.builder()
            .replace(newClassTree.getIdentifier(), "StringBuilder")
            .replace(varTree.getType(), "StringBuilder")
            .build());
  }

  private static TreePath findEnclosingMethod(VisitorState state) {
    TreePath path = state.getPath();
    while (path != null) {
      switch (path.getLeaf().getKind()) {
        case METHOD:
          return path;
        case CLASS:
        case LAMBDA_EXPRESSION:
          return null;
        default: // fall out
      }
      path = path.getParentPath();
    }
    return null;
  }
}
