/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** @author glorioso@google.com (Nick Glorioso) */
@BugPattern(
    name = "CompatibleWithAnnotationMisuse",
    summary = "@CompatibleWith's value is not a type argument.",
    severity = ERROR,
    category = JDK)
public class CompatibleWithMisuse extends BugChecker implements AnnotationTreeMatcher {

  private static final Matcher<AnnotationTree> IS_COMPATIBLE_WITH_ANNOTATION =
      Matchers.isType(CompatibleWith.class.getCanonicalName());

  @Override
  public Description matchAnnotation(AnnotationTree annoTree, VisitorState state) {
    if (!IS_COMPATIBLE_WITH_ANNOTATION.matches(annoTree, state)) {
      return Description.NO_MATCH;
    }

    // Hunt for type args on the declared method
    // TODO(glorioso): Once annotation is TYPE_USE, make sure that the node is actually a method
    // parameter
    MethodTree methodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    MethodSymbol declaredMethod = ASTHelpers.getSymbol(methodTree);

    // If this method overrides other methods, ensure that none of them have @CompatibleWith.
    // This restriction may need to be removed to allow more complex declaration hierarchies.
    for (MethodSymbol methodSymbol :
        ASTHelpers.findSuperMethods(declaredMethod, state.getTypes())) {
      if (methodSymbol.params().stream()
          .anyMatch(p -> ASTHelpers.hasAnnotation(p, CompatibleWith.class, state))) {
        return describeWithMessage(
            annoTree,
            String.format(
                "This method overrides a method in %s that already has @CompatibleWith",
                methodSymbol.owner.getSimpleName()));
      }
    }

    List<TypeVariableSymbol> potentialTypeVars =
        new ArrayList<>(declaredMethod.getTypeParameters());

    // Check enclosing types (not superclasses)
    ClassSymbol cs = (ClassSymbol) declaredMethod.owner;
    do {
      potentialTypeVars.addAll(cs.getTypeParameters());
      cs = cs.isInner() ? cs.owner.enclClass() : null;
    } while (cs != null);

    if (potentialTypeVars.isEmpty()) {
      return describeWithMessage(
          annoTree, "There are no type arguments in scope to match against.");
    }

    Set<String> validNames =
        potentialTypeVars.stream()
            .map(TypeVariableSymbol::getSimpleName)
            .map(Object::toString)
            .collect(toImmutableSet());
    String constValue = valueArgumentFromCompatibleWithAnnotation(annoTree);

    if (constValue == null || constValue.isEmpty()) {
      return describeWithMessage(
          annoTree,
          String.format(
              "The value of @CompatibleWith must not be empty (valid arguments are %s)",
              printTypeArgs(validNames)));
    }

    return validNames.contains(constValue)
        ? Description.NO_MATCH
        : describeWithMessage(
            annoTree,
            String.format(
                "%s is not a valid type argument. Valid arguments are: %s",
                constValue, printTypeArgs(validNames)));
  }

  // @CompatibleWith("X"), @CompatibleWith(value = "X"),
  // @CompatibleWith(SOME_FIELD_WHOSE_CONSTANT_VALUE_IS_X)
  // => X
  // This function assumes the the annotation tree will only have one argument, of type String, that
  // is required.
  private String valueArgumentFromCompatibleWithAnnotation(AnnotationTree tree) {
    ExpressionTree argumentValue = Iterables.getOnlyElement(tree.getArguments());
    if (argumentValue.getKind() != Kind.ASSIGNMENT) {
      // :-| Annotation symbol broken. Punt?
      return null;
    }

    return ASTHelpers.constValue(((AssignmentTree) argumentValue).getExpression(), String.class);
  }

  private String printTypeArgs(Set<String> validNames) {
    return Joiner.on(", ").join(validNames);
  }

  private Description describeWithMessage(Tree tree, String message) {
    return buildDescription(tree).setMessage(message).build();
  }
}
