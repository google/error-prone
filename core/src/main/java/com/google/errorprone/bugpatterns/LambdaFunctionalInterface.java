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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** @author amesbah@google.com (Ali Mesbah) */
@BugPattern(
    name = "LambdaFunctionalInterface",
    summary =
        "Use Java's utility functional interfaces instead of Function<A, B> for primitive types.",
    generateExamplesFromTestCases = false,
    severity = SUGGESTION)
public class LambdaFunctionalInterface extends BugChecker implements MethodTreeMatcher {
  private static final String JAVA_UTIL_FUNCTION_FUNCTION = "java.util.function.Function";
  private static final String JAVA_LANG_NUMBER = "java.lang.Number";

  private static final ImmutableMap<String, String> methodMappings =
      ImmutableMap.<String, String>builder()
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Double,java.lang.Double>",
              "java.util.function.DoubleFunction<Double>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Double,java.lang.Integer>",
              "java.util.function.DoubleToIntFunction")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Double,java.lang.Long>",
              "java.util.function.DoubleToLongFunction")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Double,T>",
              "java.util.function.DoubleFunction<T>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Integer,java.lang.Integer>",
              "java.util.function.IntFunction<Integer>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Integer,java.lang.Double>",
              "java.util.function.IntToDoubleFunction")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Integer,java.lang.Long>",
              "java.util.function.IntToLongFunction")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Integer,T>",
              "java.util.function.IntFunction<T>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Long,java.lang.Long>",
              "java.util.function.LongFunction<Long>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Long,java.lang.Integer>",
              "java.util.function.LongToIntFunction")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Long,java.lang.Double>",
              "java.util.function.LongToDoubleFunction")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<java.lang.Long,T>",
              "java.util.function.LongFunction<T>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<T,java.lang.Long>",
              "java.util.function.ToLongFunction<T>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<T,java.lang.Integer>",
              "java.util.function.ToIntFunction<T>")
          .put(
              JAVA_UTIL_FUNCTION_FUNCTION + "<T,java.lang.Double>",
              "java.util.function.ToDoubleFunction<T>")
          .build();

  private static final ImmutableMap<String, String> applyMappings =
      ImmutableMap.<String, String>builder()
          .put("java.util.function.DoubleToIntFunction", "applyAsInt")
          .put("java.util.function.DoubleToLongFunction", "applyAsLong")
          .put("java.util.function.IntToDoubleFunction", "applyAsDouble")
          .put("java.util.function.IntToLongFunction", "applyAsLong")
          .put("java.util.function.LongToIntFunction", "applyAsInt")
          .put("java.util.function.LongToDoubleFunction", "applyAsDouble")
          .put("java.util.function.ToIntFunction<T>", "applyAsInt")
          .put("java.util.function.ToDoubleFunction<T>", "applyAsDouble")
          .put("java.util.function.ToLongFunction<T>", "applyAsLong")
          .build();

  /**
   * Identifies methods with parameters that have a generic argument with Int, Long, or Double. If
   * pre-conditions are met, it refactors them to the primitive specializations.
   *
   * <pre>PreConditions:
   * (1): The method declaration has to be private (to do a safe refactoring)
   * (2): Its parameters have to meet the following conditions:
   *    2.1 Contain type java.util.function.Function
   *    2.2 At least one argument type of the Function must be subtype of Number
   * (3): All its invocations in the top-level enclosing class have to meet the following
   * conditions as well:
   *    3.1: lambda argument of Kind.LAMBDA_EXPRESSION
   *    3.2: same as 2.1
   *    3.3: same as 2.2
   * </pre>
   *
   * <pre>
   * Refactoring Changes for matched methods:
   * (1) Add the imports
   * (2) Change the method signature to use utility function instead of Function
   * (3) Find and change the 'apply' calls to the corresponding applyAsT
   * </pre>
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {

    MethodSymbol methodSym = ASTHelpers.getSymbol(tree);

    // precondition (1)
    if (!methodSym.getModifiers().contains(Modifier.PRIVATE)) {
      return Description.NO_MATCH;
    }

    ImmutableList<Tree> params =
        tree.getParameters().stream()
            .filter(param -> hasFunctionAsArg(param, state))
            .filter(
                param ->
                    isFunctionArgSubtypeOf(
                            param, 0, state.getTypeFromString(JAVA_LANG_NUMBER), state)
                        || isFunctionArgSubtypeOf(
                            param, 1, state.getTypeFromString(JAVA_LANG_NUMBER), state))
            .collect(toImmutableList());

    // preconditions (2) and (3)
    if (params.isEmpty() || !methodCallsMeetConditions(methodSym, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    for (Tree param : params) {
      getMappingForFunctionFromTree(param)
          .ifPresent(
              mappedFunction -> {
                fixBuilder.addImport(getImportName(mappedFunction));
                fixBuilder.replace(
                    param,
                    getFunctionName(mappedFunction) + " " + ASTHelpers.getSymbol(param).name);
                refactorInternalApplyMethods(tree, fixBuilder, param, mappedFunction);
              });
    }

    return describeMatch(tree, fixBuilder.build());
  }

  private void refactorInternalApplyMethods(
      MethodTree tree, SuggestedFix.Builder fixBuilder, Tree param, String mappedFunction) {
    getMappingForApply(mappedFunction)
        .ifPresent(
            apply -> {
              tree.accept(
                  new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree callTree, Void unused) {
                      if (getSymbol(callTree).name.contentEquals("apply")) {
                        Symbol receiverSym = getSymbol(getReceiver(callTree));
                        if (receiverSym != null
                            && receiverSym.equals(ASTHelpers.getSymbol(param))) {
                          fixBuilder.replace(
                              callTree.getMethodSelect(), receiverSym.name + "." + apply);
                        }
                      }
                      return super.visitMethodInvocation(callTree, unused);
                    }
                  },
                  null);
            });
  }

  private boolean methodCallsMeetConditions(Symbol sym, VisitorState state) {
    ImmutableMultimap<String, MethodInvocationTree> methodCallMap =
        methodCallsForSymbol(sym, getTopLevelClassTree(state));

    if (methodCallMap.isEmpty()) {
      // no method invocations for this method, safe to refactor
      return true;
    }

    for (MethodInvocationTree methodInvocationTree : methodCallMap.values()) {
      if (methodInvocationTree.getArguments().stream()
          .filter(a -> Kind.LAMBDA_EXPRESSION.equals(a.getKind()))
          .filter(a -> hasFunctionAsArg(a, state))
          .noneMatch(
              a ->
                  isFunctionArgSubtypeOf(a, 0, state.getTypeFromString(JAVA_LANG_NUMBER), state)
                      || isFunctionArgSubtypeOf(
                          a, 1, state.getTypeFromString(JAVA_LANG_NUMBER), state))) {
        return false;
      }
    }

    return true;
  }

  private static ClassTree getTopLevelClassTree(VisitorState state) {
    return (ClassTree)
        Streams.findLast(
                Streams.stream(state.getPath().iterator())
                    .filter((Tree t) -> t.getKind() == Kind.CLASS))
            .orElseThrow(() -> new IllegalArgumentException("No enclosing class found"));
  }

  private ImmutableMultimap<String, MethodInvocationTree> methodCallsForSymbol(
      Symbol sym, ClassTree classTree) {
    ImmutableMultimap.Builder<String, MethodInvocationTree> methodMap = ImmutableMultimap.builder();
    classTree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree callTree, Void unused) {
            final MethodSymbol methodSymbol = getSymbol(callTree);
            if (methodSymbol != null && sym.equals(methodSymbol)) {
              methodMap.put(methodSymbol.toString(), callTree);
            }
            return super.visitMethodInvocation(callTree, unused);
          }
        },
        null);

    return methodMap.build();
  }

  private static boolean hasFunctionAsArg(Tree param, VisitorState state) {
    return ASTHelpers.isSameType(
        ASTHelpers.getType(param), state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state);
  }

  private static boolean isFunctionArgSubtypeOf(
      Tree param, int argIndex, Type type, VisitorState state) {
    return ASTHelpers.isSubtype(
        ASTHelpers.getType(param).getTypeArguments().get(argIndex), type, state);
  }

  private static Optional<String> getMappingForFunctionFromTree(Tree param) {
    Optional<Type> type = ofNullable(ASTHelpers.getType(param));
    return (type == null) ? empty() : getMappingForFunction(type.get().toString());
  }

  private static Optional<String> getMappingForFunction(String function) {
    return ofNullable(methodMappings.get(function));
  }

  private static Optional<String> getMappingForApply(String apply) {
    return ofNullable(applyMappings.get(apply));
  }

  private static String getFunctionName(String fullyQualifiedName) {
    return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
  }

  private static String getImportName(String fullyQualifiedName) {
    int cutPosition = fullyQualifiedName.indexOf('<');

    return (cutPosition < 0) ? fullyQualifiedName : fullyQualifiedName.substring(0, cutPosition);
  }
}
