/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ReturnTree;
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
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Suggests alternatives to obsolete JDK classes.", severity = WARNING)
public class JdkObsolete extends BugChecker
    implements NewClassTreeMatcher, ClassTreeMatcher, MemberReferenceTreeMatcher {

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

    Optional<Fix> fix(Tree tree, VisitorState state) {
      return Optional.empty();
    }
  }

  static final ImmutableMap<String, Obsolete> OBSOLETE =
      ImmutableList.of(
              new Obsolete(
                  "java.util.LinkedList",
                  "It is very rare for LinkedList to out-perform ArrayList or ArrayDeque. Avoid it"
                      + " unless you're willing to invest a lot of time into benchmarking. Caveat:"
                      + " LinkedList supports null elements, but ArrayDeque does not.") {
                @Override
                Optional<Fix> fix(Tree tree, VisitorState state) {
                  return linkedListFix(tree, state);
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
                      + " prefer ArrayDeque. Note that the Stack methods push/pop/peek correspond"
                      + " to the Deque methods addFirst/removeFirst/peekFirst."),
              new Obsolete(
                  "java.lang.StringBuffer",
                  "StringBuffer performs synchronization that is usually unnecessary;"
                      + " prefer StringBuilder.") {
                @Override
                Optional<Fix> fix(Tree tree, VisitorState state) {
                  return stringBufferFix(state);
                }
              },
              new Obsolete(
                  "java.util.SortedSet", "SortedSet was replaced by NavigableSet in Java 6."),
              new Obsolete(
                  "java.util.SortedMap", "SortedMap was replaced by NavigableMap in Java 6."),
              new Obsolete(
                  "java.util.Dictionary",
                  "Dictionary is a nonstandard class that predates the Java Collections Framework;"
                      + " use LinkedHashMap."),
              new Obsolete(
                  "java.util.Enumeration", "Enumeration is an ancient precursor to Iterator."))
          .stream()
          .collect(toImmutableMap(Obsolete::qualifiedName, x -> x));

  static final Matcher<ExpressionTree> MATCHER_STRINGBUFFER =
      anyOf(
          // a pre-JDK-8039124 concession
          instanceMethod()
              .onExactClass("java.util.regex.Matcher")
              .named("appendTail")
              .withParameters("java.lang.StringBuffer"),
          instanceMethod()
              .onExactClass("java.util.regex.Matcher")
              .named("appendReplacement")
              .withParameters("java.lang.StringBuffer", "java.lang.String"),
          // TODO(cushon): back this out if https://github.com/google/re2j/pull/44 happens
          instanceMethod()
              .onExactClass("com.google.re2j.Matcher")
              .named("appendTail")
              .withParameters("java.lang.StringBuffer"),
          instanceMethod()
              .onExactClass("com.google.re2j.Matcher")
              .named("appendReplacement")
              .withParameters("java.lang.StringBuffer", "java.lang.String"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol constructor = ASTHelpers.getSymbol(tree);
    Symbol owner = constructor.owner;
    Description description =
        describeIfObsolete(
            // don't refactor anonymous implementations of LinkedList
            tree.getClassBody() == null ? tree.getIdentifier() : null,
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
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof NewClassTree newClassTree && tree.equals(newClassTree.getClassBody())) {
      // don't double-report anonymous implementations of obsolete interfaces
      return NO_MATCH;
    }
    ClassSymbol symbol = ASTHelpers.getSymbol(tree);
    return describeIfObsolete(null, state.getTypes().directSupertypes(symbol.asType()), state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree.getQualifierExpression());
    if (type == null) {
      return NO_MATCH;
    }
    return describeIfObsolete(tree.getQualifierExpression(), ImmutableList.of(type), state);
  }

  private Description describeIfObsolete(
      @Nullable Tree tree, Iterable<Type> types, VisitorState state) {
    for (Type type : types) {
      Obsolete obsolete = OBSOLETE.get(type.asElement().getQualifiedName().toString());
      if (obsolete == null) {
        continue;
      }
      if (shouldSkip(state, type)) {
        continue;
      }
      Description.Builder description =
          buildDescription(state.getPath().getLeaf()).setMessage(obsolete.message());
      if (tree != null) {
        obsolete.fix(tree, state).ifPresent(description::addFix);
      }
      return description.build();
    }
    return NO_MATCH;
  }

  private static @Nullable Type getMethodOrLambdaReturnType(VisitorState state) {
    for (Tree tree : state.getPath()) {
      switch (tree.getKind()) {
        case LAMBDA_EXPRESSION -> {
          return state.getTypes().findDescriptorType(ASTHelpers.getType(tree)).getReturnType();
        }
        case METHOD -> {
          return ASTHelpers.getType(tree).getReturnType();
        }
        case CLASS -> {
          return null;
        }
        default -> {}
      }
    }
    return null;
  }

  static @Nullable Type getTargetType(VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    Type type;
    if (parent instanceof VariableTree || parent instanceof AssignmentTree) {
      type = ASTHelpers.getType(parent);
    } else if (parent instanceof ReturnTree || parent instanceof LambdaExpressionTree) {
      type = getMethodOrLambdaReturnType(state);
    } else if (parent instanceof MethodInvocationTree tree) {
      int idx = tree.getArguments().indexOf(state.getPath().getLeaf());
      if (idx == -1) {
        return null;
      }
      Type methodType = ASTHelpers.getType(tree.getMethodSelect());
      if (idx >= methodType.getParameterTypes().size()) {
        return null;
      }
      return methodType.getParameterTypes().get(idx);
    } else {
      return null;
    }
    Tree tree = state.getPath().getLeaf();
    if (tree instanceof MemberReferenceTree) {
      type = state.getTypes().findDescriptorType(ASTHelpers.getType(tree)).getReturnType();
    }
    return type;
  }

  // rewrite e.g. `List<Object> xs = new LinkedList<>()` -> `... = new ArrayList<>()`
  private static Optional<Fix> linkedListFix(Tree tree, VisitorState state) {
    Type type = getTargetType(state);
    if (type == null) {
      return Optional.empty();
    }
    Types types = state.getTypes();
    for (String replacement : ImmutableList.of("java.util.ArrayList", "java.util.ArrayDeque")) {
      Symbol sym = state.getSymbolFromString(replacement);
      if (sym == null) {
        continue;
      }
      if (types.isAssignable(types.erasure(sym.asType()), types.erasure(type))) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        while (tree instanceof ParameterizedTypeTree) {
          tree = ((ParameterizedTypeTree) tree).getType();
        }
        fix.replace(tree, SuggestedFixes.qualifyType(state, fix, sym));
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
    if (!(tree instanceof NewClassTree newClassTree)) {
      return Optional.empty();
    }
    // expect e.g. `StringBuffer sb = new StringBuffer();`
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof VariableTree varTree)) {
      return Optional.empty();
    }
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
          if (!(parent instanceof MemberSelectTree memberSelectTree)
              || memberSelectTree.getExpression() != tree) {
            escape[0] = true;
          }
        }
        return null;
      }
    }.scan(methodPath, null);
    if (escape[0]) {
      return Optional.empty();
    }
    SuggestedFix.Builder fix =
        SuggestedFix.builder().replace(newClassTree.getIdentifier(), "StringBuilder");
    if (!ASTHelpers.hasImplicitType(varTree, state)) {
      // If the variable is declared with `var`, there's no declaration type to change
      fix = fix.replace(varTree.getType(), "StringBuilder");
    }
    return Optional.of(fix.build());
  }

  private static @Nullable TreePath findEnclosingMethod(VisitorState state) {
    TreePath path = state.getPath();
    while (path != null) {
      switch (path.getLeaf().getKind()) {
        case METHOD -> {
          return path;
        }
        case CLASS, LAMBDA_EXPRESSION -> {
          return null;
        }
        default -> {}
      }
      path = path.getParentPath();
    }
    return null;
  }

  private boolean shouldSkip(VisitorState state, Type type) {
    TreePath path = findEnclosingMethod(state);
    if (path == null) {
      return false;
    }
    MethodTree enclosingMethod = (MethodTree) path.getLeaf();
    if (enclosingMethod == null) {
      return false;
    }
    return implementingObsoleteMethod(enclosingMethod, state, type)
        || mockingObsoleteMethod(enclosingMethod, state, type);
  }

  private static final Matcher<ExpressionTree> MOCKITO_MATCHER =
      staticMethod().onClass("org.mockito.Mockito").named("when");

  /** Allow mocking APIs that return obsolete types. */
  private boolean mockingObsoleteMethod(MethodTree enclosingMethod, VisitorState state, Type type) {
    // mutable boolean to return result from visitor
    boolean[] found = {false};
    enclosingMethod.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            if (found[0]) {
              return null;
            }
            if (MOCKITO_MATCHER.matches(node, state)) {
              Type stubber = ASTHelpers.getReturnType(node);
              if (!stubber.getTypeArguments().isEmpty()
                  && ASTHelpers.isSameType(
                      getOnlyElement(stubber.getTypeArguments()), type, state)) {
                found[0] = true;
              }
            }
            return super.visitMethodInvocation(node, null);
          }
        },
        null);
    return found[0];
  }

  /** Allow creating obsolete types when overriding a method with an obsolete return type. */
  private static boolean implementingObsoleteMethod(
      MethodTree enclosingMethod, VisitorState state, Type type) {
    MethodSymbol method = ASTHelpers.getSymbol(enclosingMethod);
    if (ASTHelpers.findSuperMethods(method, state.getTypes()).isEmpty()) {
      // not an override
      return false;
    }
    if (!ASTHelpers.isSameType(method.getReturnType(), type, state)) {
      return false;
    }
    return true;
  }

  @Override
  public boolean supportsUnneededSuppressionWarnings() {
    return true;
  }
}
