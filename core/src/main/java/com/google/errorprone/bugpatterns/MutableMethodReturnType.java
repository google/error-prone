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

import static com.google.common.base.Preconditions.checkState;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.Optional;
import java.util.function.Predicate;

/** @author dorir@google.com (Dori Reuveni) */
@BugPattern(
    name = "MutableMethodReturnType",
    category = JDK,
    summary =
        "Method return type should use the immutable type (such as ImmutableList) instead of"
            + " the general collection interface type (such as List)",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class MutableMethodReturnType extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> ANNOTATED_WITH_PRODUCES_OR_PROVIDES =
      InjectMatchers.hasProvidesAnnotation();

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);

    if (methodSymbol.isConstructor()) {
      return Description.NO_MATCH;
    }

    if (ASTHelpers.methodCanBeOverridden(methodSymbol)) {
      return Description.NO_MATCH;
    }

    if (ANNOTATED_WITH_PRODUCES_OR_PROVIDES.matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    Type returnType = methodSymbol.getReturnType();
    if (ImmutableCollections.isImmutableType(returnType)) {
      return Description.NO_MATCH;
    }

    ImmutableSet<ClassType> returnStatementsTypes = getMethodReturnTypes(methodTree);
    if (returnStatementsTypes.isEmpty()) {
      return Description.NO_MATCH;
    }
    boolean alwaysReturnsImmutableType =
        returnStatementsTypes.stream().allMatch(ImmutableCollections::isImmutableType);
    if (!alwaysReturnsImmutableType) {
      return Description.NO_MATCH;
    }

    Optional<String> immutableReturnType =
        ImmutableCollections.mutableToImmutable(getTypeQualifiedName(returnType));
    if (!immutableReturnType.isPresent()) {
      immutableReturnType =
          getCommonImmutableTypeForAllReturnStatementsTypes(returnStatementsTypes);
    }
    if (!immutableReturnType.isPresent()) {
      return Description.NO_MATCH;
    }

    Type newReturnType = state.getTypeFromString(immutableReturnType.get());
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    fixBuilder.replace(
        getTypeTree(methodTree.getReturnType()),
        SuggestedFixes.qualifyType(state, fixBuilder, newReturnType.asElement()));
    SuggestedFix fix = fixBuilder.build();

    return describeMatch(methodTree.getReturnType(), fix);
  }

  private static Optional<String> getCommonImmutableTypeForAllReturnStatementsTypes(
      ImmutableSet<ClassType> returnStatementsTypes) {
    checkState(!returnStatementsTypes.isEmpty());

    ClassType arbitraryClassType = returnStatementsTypes.asList().get(0);
    ImmutableList<String> superTypes = getImmutableSuperTypesForClassType(arbitraryClassType);

    return superTypes.stream()
        .filter(areAllReturnStatementsAssignable(returnStatementsTypes))
        .findFirst();
  }

  private static Predicate<String> areAllReturnStatementsAssignable(
      ImmutableSet<ClassType> returnStatementsTypes) {
    return s ->
        returnStatementsTypes.stream()
            .map(MutableMethodReturnType::getImmutableSuperTypesForClassType)
            .allMatch(c -> c.contains(s));
  }

  private static ImmutableList<String> getImmutableSuperTypesForClassType(ClassType classType) {
    ImmutableList.Builder<String> immutableSuperTypes = ImmutableList.builder();

    ClassType superType = classType;
    while (superType.supertype_field instanceof ClassType) {
      if (ImmutableCollections.isImmutableType(superType)) {
        immutableSuperTypes.add(getTypeQualifiedName(superType.asElement().type));
      }
      superType = (ClassType) superType.supertype_field;
    }

    return immutableSuperTypes.build();
  }

  private static String getTypeQualifiedName(Type type) {
    return type.tsym.getQualifiedName().toString();
  }

  private static ImmutableSet<ClassType> getMethodReturnTypes(MethodTree methodTree) {
    ImmutableSet.Builder<ClassType> returnTypes = ImmutableSet.builder();
    methodTree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitReturn(ReturnTree node, Void unused) {
            Type type = ASTHelpers.getType(node.getExpression());
            if (type instanceof ClassType) {
              returnTypes.add((ClassType) type);
            }

            return null;
          }
        },
        null /* unused */);
    return returnTypes.build();
  }

  private static Tree getTypeTree(Tree tree) {
    return tree.accept(GET_TYPE_TREE_VISITOR, null /* unused */);
  }

  private static final SimpleTreeVisitor<Tree, Void> GET_TYPE_TREE_VISITOR =
      new SimpleTreeVisitor<Tree, Void>() {
        @Override
        public Tree visitIdentifier(IdentifierTree tree, Void unused) {
          return tree;
        }

        @Override
        public Tree visitParameterizedType(ParameterizedTypeTree tree, Void unused) {
          return tree.getType();
        }
      };
}
