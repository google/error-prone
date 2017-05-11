/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.Maps.newHashMap;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.hasIdentifier;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static java.util.Collections.unmodifiableList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Map;

/**
 * Checks, if two constructors in a class both accept {@code Foo foo} and one calls the other, that
 * the caller passes {@code foo} as a parameter. The goal is to catch copy-paste errors:
 *
 * <pre>
 *   MissileLauncher(Location target, boolean askForConfirmation) {
 *     ...
 *   }
 *   MissileLauncher(Location target) {
 *     this(target, false);
 *   }
 *   MissileLauncher(boolean askForConfirmation) {
 *     this(TEST_TARGET, <b>false</b>); // should be askForConfirmation
 *   }</pre>
 *
 * @author cpovirk@google.com (Chris Povirk)
 */
@BugPattern(
  name = "ChainingConstructorIgnoresParameter",
  category = JDK,
  severity = ERROR,
  explanation = "A constructor parameter might not be being used as expected",
  summary =
      "The called constructor accepts a parameter with the same name and type as one of "
          + "its caller's parameters, but its caller doesn't pass that parameter to it.  It's "
          + "likely that it was intended to."
)
public final class ChainingConstructorIgnoresParameter extends BugChecker
    implements CompilationUnitTreeMatcher, MethodInvocationTreeMatcher, MethodTreeMatcher {
  private final Map<MethodSymbol, List<VariableTree>> paramTypesForMethod = newHashMap();
  private final Multimap<MethodSymbol, Caller> callersToEvaluate = ArrayListMultimap.create();

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    /*
     * Clear the collections to save memory. (I wonder if it also helps to handle weird cases when a
     * class has multiple definitions. But I would expect for multiple definitions within the same
     * compiler invocation to cause deeper problems.)
     */
    paramTypesForMethod.clear();
    callersToEvaluate.clear(); // should have already been cleared
    return NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    // TODO(cpovirk): determine whether anyone might be calling Foo.this()
    if (!isIdentifierWithName(tree.getMethodSelect(), "this")) {
      return NO_MATCH;
    }
    callersToEvaluate.put(symbol, new Caller(tree, state));
    return evaluateCallers(symbol);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (!symbol.isConstructor()) {
      return NO_MATCH;
    }
    paramTypesForMethod.put(symbol, unmodifiableList(tree.getParameters()));
    return evaluateCallers(symbol);
  }

  private Description evaluateCallers(MethodSymbol symbol) {
    List<VariableTree> paramTypes = paramTypesForMethod.get(symbol);
    if (paramTypes == null) {
      // We haven't seen the declaration yet. We'll evaluate the call when we do.
      return NO_MATCH;
    }

    for (Caller caller : callersToEvaluate.removeAll(symbol)) {
      VisitorState state = caller.state;
      MethodInvocationTree invocation = caller.tree;

      MethodTree callerConstructor = state.findEnclosing(MethodTree.class);
      if (callerConstructor == null) {
        continue; // impossible, at least in compilable code?
      }
      Map<String, Type> availableParams = indexTypeByName(callerConstructor.getParameters());

      /*
       * TODO(cpovirk): Better handling of varargs: If the last parameter type is varargs and it is
       * called as varargs (rather than by passing an array), then rewrite the parameter types to
       * (p0, p1, ..., p[n-2], p[n-1] = element type of varargs parameter if an argument is
       * supplied, p[n] = ditto, etc.). For now, we settle for not crashing in the face of a
       * mismatch between the number of parameters declared and the number supplied.
       *
       * (Use MethodSymbol.isVarArgs.)
       */
      for (int i = 0; i < paramTypes.size() && i < invocation.getArguments().size(); i++) {
        VariableTree formalParam = paramTypes.get(i);
        String formalParamName = formalParam.getName().toString();
        Type formalParamType = getType(formalParam.getType());

        Type availableParamType = availableParams.get(formalParamName);

        ExpressionTree actualParam = invocation.getArguments().get(i);

        if (
        /*
         * The caller has no param of this type. (Or if it did, we couldn't determine the type.
         * Does that ever happen?) If the param doesn't exist, the caller can't be failing to
         * pass it.
         */
        availableParamType == null

            /*
             * We couldn't determine the type of the formal parameter. (Does this ever happen?)
             */
            || formalParamType == null

            /*
             * The caller is passing the expected parameter (or "ImmutableList.copyOf(parameter),"
             * "new File(parameter)," etc.).
             */
            || referencesIdentifierWithName(formalParamName, actualParam, state)) {
          continue;
        }

        if (state.getTypes().isAssignable(availableParamType, formalParamType)) {
          reportMatch(invocation, state, actualParam, formalParamName);
        }
        /*
         * If formal parameter is of an incompatible type, the caller might in theory still intend
         * to pass a dervied expression. For example, "Foo(String file)" might intend to call
         * "Foo(File file)" by passing "new File(file)." If this comes up in practice, we could
         * provide the dummy suggested fix "someExpression(formalParamName)." However, my research
         * suggests that this will rarely if ever be what the user wants.
         */
      }
    }

    // All matches are reported through reportMatch calls instead of return values.
    return NO_MATCH;
  }

  private static Map<String, Type> indexTypeByName(List<? extends VariableTree> parameters) {
    Map<String, Type> result = newHashMap();
    for (VariableTree parameter : parameters) {
      result.put(parameter.getName().toString(), getType(parameter.getType()));
    }
    return result;
  }

  private void reportMatch(
      Tree diagnosticPosition, VisitorState state, Tree toReplace, String replaceWith) {
    state.reportMatch(describeMatch(diagnosticPosition, replace(toReplace, replaceWith)));
  }

  private static boolean referencesIdentifierWithName(
      final String name, ExpressionTree tree, VisitorState state) {
    Matcher<IdentifierTree> identifierMatcher =
        new Matcher<IdentifierTree>() {
          @Override
          public boolean matches(IdentifierTree tree, VisitorState state) {
            return isIdentifierWithName(tree, name);
          }
        };
    return hasIdentifier(identifierMatcher).matches(tree, state);
  }

  private static boolean isIdentifierWithName(ExpressionTree tree, String name) {
    return tree.getKind() == IDENTIFIER && ((IdentifierTree) tree).getName().contentEquals(name);
  }

  private static final class Caller {
    final MethodInvocationTree tree;
    final VisitorState state;

    Caller(MethodInvocationTree tree, VisitorState state) {
      this.tree = tree;
      this.state = state;
    }
  }
}
