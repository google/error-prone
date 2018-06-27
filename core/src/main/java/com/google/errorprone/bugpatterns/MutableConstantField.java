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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** @author dorir@google.com (Dori Reuveni) */
@BugPattern(
    name = "MutableConstantField",
    category = JDK,
    summary =
        "Constant field declarations should use the immutable type (such as ImmutableList) instead"
            + " of the general collection interface type (such as List)",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class MutableConstantField extends BugChecker implements VariableTreeMatcher {

  private static final String BIND = "com.google.inject.testing.fieldbinder.Bind";

  private static final MultiMatcher<VariableTree, AnnotationTree> VAR_TO_BIND_ANNOTATION =
      annotations(AT_LEAST_ONE, isSameType(BIND));

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {

    if (!isConstantField(ASTHelpers.getSymbol(tree))) {
      return Description.NO_MATCH;
    }

    Tree rhsTree = tree.getInitializer();
    Type rhsType = ASTHelpers.getType(rhsTree);
    if (rhsType == null || !ImmutableCollections.isImmutableType(rhsType)) {
      return Description.NO_MATCH;
    }

    Tree lhsTree = tree.getType();
    Type lhsType = ASTHelpers.getType(lhsTree);
    if (lhsType == null || ImmutableCollections.isImmutableType(lhsType)) {
      return Description.NO_MATCH;
    }

    MultiMatchResult<AnnotationTree> hasBindAnnotations =
        VAR_TO_BIND_ANNOTATION.multiMatchResult(tree, state);
    if (hasBindAnnotations.matches()) {
      return Description.NO_MATCH;
    }

    String lhsTypeQualifiedName = getTypeQualifiedName(lhsType);
    String newLhsTypeQualifiedName =
        ImmutableCollections.mutableToImmutable(lhsTypeQualifiedName)
            .orElse(getTypeQualifiedName(rhsType));

    Type newLhsType = state.getTypeFromString(newLhsTypeQualifiedName);
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    fixBuilder.replace(
        getTypeTree(lhsTree),
        SuggestedFixes.qualifyType(state, fixBuilder, newLhsType.asElement()));
    SuggestedFix fix = fixBuilder.build();

    return describeMatch(lhsTree, fix);
  }

  private static String getTypeQualifiedName(Type type) {
    return type.tsym.getQualifiedName().toString();
  }

  private static boolean isConstantField(Symbol sym) {
    return sym.getKind() == ElementKind.FIELD
        && isStaticFinalField(sym)
        && isConstantFieldName(sym.getSimpleName().toString());
  }

  private static boolean isStaticFinalField(Symbol sym) {
    return sym.isStatic() && sym.getModifiers().contains(Modifier.FINAL);
  }

  private static boolean isConstantFieldName(String fieldName) {
    return fieldName.toUpperCase().equals(fieldName);
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
