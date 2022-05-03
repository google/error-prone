/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.DoNotCallChecker.DO_NOT_CALL;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.findClass;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.Modifier;

/**
 * If a method always throws an exception, consider annotating it with {@code @DoNotCall} to prevent
 * calls at compile-time instead failing at runtime.
 */
@BugPattern(
    summary =
        "Consider annotating methods that always throw with @DoNotCall. "
            + "Read more at https://errorprone.info/bugpattern/DoNotCall",
    severity = WARNING)
public class DoNotCallSuggester extends BugChecker implements MethodTreeMatcher {

  // TODO(kak): Consider adding "newInstance" to this list (some frameworks use that method name)
  private static final ImmutableSet<String> METHOD_PREFIXES_TO_IGNORE =
      ImmutableSet.of(
          // likely providing Dagger bindings
          "produce", "provide");

  private static final ImmutableSet<String> METHOD_SUBSTRINGS_TO_IGNORE =
      ImmutableSet.of(
          // common substrings in the names of exception factory methods
          "throw", "fail", "exception", "propagate");

  private static final ImmutableSet<String> ANNOTATIONS_TO_IGNORE =
      ImmutableSet.of(
          // ignore methods that are already annotated w/ @DoNotCall
          DO_NOT_CALL,
          // We exclude methods that are overrides; at call sites, rarely is the variable reference
          // statically typed as the subclass (often it's typed as the interface type), so adding
          // @DoNotCall won't actually help any callers.
          "java.lang.Override",
          // dagger provider / producers
          "dagger.Provides",
          "dagger.producers.Produces",
          // starlark API boundary
          "net.starlark.java.annot.StarlarkMethod");

  private static final ImmutableSet<String> PARENT_CLASS_TO_IGNORE =
      ImmutableSet.of(
          // a Guice module w/ bindings
          "com.google.inject.AbstractModule");

  private static final ImmutableSet<String> RETURNED_SUPER_TYPES_TO_IGNORE =
      ImmutableSet.of(
          // a throwable
          "java.lang.Throwable");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    // if we can't find the method symbol, exit
    MethodSymbol symbol = getSymbol(tree);

    // if the method is abstract, exit
    if (tree.getBody() == null) {
      return NO_MATCH;
    }

    // if the body does not contain exactly 1 statement, exit
    if (tree.getBody().getStatements().size() != 1) {
      return NO_MATCH;
    }

    // if the method is private or overrideable, exit
    if (symbol.getModifiers().contains(Modifier.PRIVATE) || methodCanBeOverridden(symbol)) {
      return NO_MATCH;
    }

    // if the single statement is not a ThrowTree, exit
    StatementTree statement = getOnlyElement(tree.getBody().getStatements());
    if (!(statement instanceof ThrowTree)) {
      return NO_MATCH;
    }

    // if the enclosing class extends from a known frameworks class, exit
    ClassSymbol enclosingClass = enclosingClass(symbol);
    Type enclosingType = getType(findClass(enclosingClass, state));
    for (String parentClass : PARENT_CLASS_TO_IGNORE) {
      Type parentClassType = state.getTypeFromString(parentClass);
      if (isSubtype(enclosingType, parentClassType, state)) {
        return NO_MATCH;
      }
    }

    // if the enclosing class is anonymous, exit
    if (enclosingClass.isAnonymous()) {
      return NO_MATCH;
    }

    // if the method name starts a banned prefix, exit
    String methodName = tree.getName().toString().toLowerCase();
    for (String methodPrefix : METHOD_PREFIXES_TO_IGNORE) {
      if (methodName.startsWith(methodPrefix)) {
        return NO_MATCH;
      }
    }

    // if a method name contais a banned substring, exit
    for (String methodSubstring : METHOD_SUBSTRINGS_TO_IGNORE) {
      if (methodName.contains(methodSubstring)) {
        return NO_MATCH;
      }
    }

    // if the method is annotated with a banned annotation, exit
    for (String annotationToIgnore : ANNOTATIONS_TO_IGNORE) {
      if (hasAnnotation(tree, annotationToIgnore, state)) {
        return NO_MATCH;
      }
    }

    // if the method returns a banned type, exit
    Type returnType = getType(tree.getReturnType());
    for (String returnedSuperType : RETURNED_SUPER_TYPES_TO_IGNORE) {
      Type throwableType = state.getTypeFromString(returnedSuperType);
      if (isSubtype(returnType, throwableType, state)) {
        return NO_MATCH;
      }
    }

    // if the method is an "effective override" (they forgot to add @Override), exit
    if (!findSuperMethods(symbol, state.getTypes()).isEmpty()) {
      return NO_MATCH;
    }

    // otherwise, suggest annotating the method with @DoNotCall
    Type thrownType = getType(((ThrowTree) statement).getExpression());
    // TODO(kak): Consider possibly stripping "java.lang" (users should be familiar with those!)
    SuggestedFix fix =
        SuggestedFix.builder()
            .addImport(DO_NOT_CALL)
            .prefixWith(tree, "@DoNotCall(\"Always throws " + thrownType + "\") ")
            .build();
    return buildDescription(tree)
        .setMessage(
            "Methods that always throw an exception should be annotated with @DoNotCall to prevent"
                + " calls at compilation time vs. at runtime (note that adding @DoNotCall will"
                + " break any existing callers of this API).")
        .addFix(fix)
        .build();
  }
}
