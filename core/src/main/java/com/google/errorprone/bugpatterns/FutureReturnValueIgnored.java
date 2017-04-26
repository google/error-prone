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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ForkJoinTask;

/** See BugPattern annotation. */
@BugPattern(
  name = "FutureReturnValueIgnored",
  summary =
      "Return value of methods returning Future must be checked. Ignoring returned Futures "
          + "suppresses exceptions thrown from the code that completes the Future.",
  explanation =
      "Methods that return `java.util.concurrent.Future` and its subclasses "
          + "generally indicate errors by returning a future that eventually fails.\n\n"
          + "If you don’t check the return value of these methods, you will never find out if they "
          + "threw an exception.",
  category = JDK,
  severity = WARNING
)
public final class FutureReturnValueIgnored extends AbstractReturnValueIgnored {

  private static final Matcher<ExpressionTree> BLACKLIST =
      anyOf(
          // ForkJoinTask#fork has side-effects and returns 'this', so it's reasonable to ignore
          // the return value.
          instanceMethod()
              .onDescendantOf(ForkJoinTask.class.getName())
              .named("fork")
              .withParameters(),
          // CompletionService is intended to be used in a way where the Future returned
          // from submit is discarded, because the Futures are available later via e.g. take()
          instanceMethod().onDescendantOf(CompletionService.class.getName()).named("submit"));

  private static final Matcher<MethodInvocationTree> MATCHER =
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree tree, VisitorState state) {
          Type futureType =
              Objects.requireNonNull(state.getTypeFromString("java.util.concurrent.Future"));
          MethodSymbol sym = ASTHelpers.getSymbol(tree);
          if (hasAnnotation(sym, CanIgnoreReturnValue.class, state)) {
            return false;
          }
          for (MethodSymbol superSym : ASTHelpers.findSuperMethods(sym, state.getTypes())) {
            // There are interfaces annotated with @CanIgnoreReturnValue (like Guava's Function)
            // whose return value really shouldn't be ignored - as a heuristic, check if the super's
            // method is returning a future subtype.
            if (hasAnnotation(superSym, CanIgnoreReturnValue.class, state)
                && ASTHelpers.isSubtype(
                    ASTHelpers.getUpperBound(superSym.getReturnType(), state.getTypes()),
                    futureType,
                    state)) {
              return false;
            }
          }
          if (BLACKLIST.matches(tree, state)) {
            return false;
          }
          Type returnType = sym.getReturnType();
          return ASTHelpers.isSubtype(
              ASTHelpers.getUpperBound(returnType, state.getTypes()), futureType, state);
        }
      };

  @Override
  public Matcher<? super MethodInvocationTree> specializedMatcher() {
    return MATCHER;
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return describeUnused(methodInvocationTree, state);
  }

  // TODO(b/18771748) the following code is hacky and scary, but will only live for as long as the
  // migration to enable this check takes. Delete once this check is turned into an error.
  private final Stack<Set<String>> stackNames = new Stack<>();
  private List<Tree> previousTreePath = new ArrayList<>();

  List<Tree> pathToList(TreePath input) {
    ArrayList<Tree> list = new ArrayList<>();
    do {
      list.add(0, input.getLeaf());
      input = input.getParentPath();
    } while (input != null);
    return list;
  }

  private String findVariableName(String name, VisitorState state) {
    if (previousTreePath.size() != stackNames.size()) {
      throw new IllegalStateException();
    }
    TreePath currentPath = state.getPath();
    List<Tree> currentPathList = pathToList(currentPath);
    for (int i = 0; i < currentPathList.size(); i++) {
      if (previousTreePath.size() > i) {
        if (!previousTreePath.get(i).equals(currentPathList.get(i))) {
          while (stackNames.size() > i) {
            stackNames.pop();
          }
          stackNames.push(new HashSet<String>());
        }
      } else {
        stackNames.push(new HashSet<String>());
      }
    }
    previousTreePath = currentPathList;
    if (previousTreePath.size() != stackNames.size()) {
      throw new IllegalStateException();
    }

    final String chosenName;
    search:
    for (int i = 0; ; i++) {
      final String identifierName;
      if (i == 0) {
        identifierName = name;
      } else {
        identifierName = name + i;
      }

      for (Set<String> set : stackNames) {
        if (set.contains(identifierName)) {
          continue search;
        }
      }
      if (FindIdentifiers.findIdent(identifierName, state) == null) {
        chosenName = identifierName;
        break;
      }
    }
    for (int i = stackNames.size() - 1; i >= 0; i--) {
      if (declaresVariableScope(currentPathList.get(i).getKind())) {
        stackNames.get(i).add(chosenName);
        return chosenName;
      }
    }
    throw new IllegalStateException("Didn't find enclosing block");
  }

  boolean declaresVariableScope(Kind kind) {
    switch (kind) {
      case BLOCK:
      case METHOD:
        return true;
      default:
        return false;
    }
  }

  /** Fixes the error by assigning the result of the call to a new local variable. */
  public Description describeUnused(MethodInvocationTree methodInvocationTree, VisitorState state) {
    // Fix by assigning the assigning the result of the call to an unused variable.
    SuggestedFix fix =
        SuggestedFix.builder()
            .addImport("java.util.concurrent.Future")
            .prefixWith(
                methodInvocationTree,
                String.format(
                    "@SuppressWarnings(\"unused\") // go/futurereturn-lsc\n" + "Future<?> %s = ",
                    findVariableName("possiblyIgnoredError", state)))
            .build();
    return describeMatch(methodInvocationTree, fix);
  }
}
