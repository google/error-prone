/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isDirectImplementationOf;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * BugPattern to detect classes which implement android.os.Parcelable but don't have public static
 * CREATOR.
 *
 * @author Sumit Bhagwani (bhagwani@google.com)
 */
@BugPattern(
    name = "ParcelableCreator",
    summary = "Detects classes which implement Parcelable but don't have CREATOR",
    severity = SeverityLevel.ERROR)
public class ParcelableCreator extends BugChecker implements ClassTreeMatcher {

  /** Matches if a non-public non-abstract class/interface is subtype of android.os.Parcelable */
  private static final Matcher<ClassTree> PARCELABLE_MATCHER =
      allOf(isDirectImplementationOf("android.os.Parcelable"), not(hasModifier(Modifier.ABSTRACT)));

  private static final Matcher<VariableTree> PARCELABLE_CREATOR_MATCHER =
      allOf(
          Matchers.isSubtypeOf("android.os.Parcelable$Creator"),
          hasModifier(Modifier.STATIC),
          hasModifier(Modifier.PUBLIC));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    if (!PARCELABLE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Symbol parcelableCreatorSymbol = state.getSymbolFromString("android.os.Parcelable$Creator");
    if (parcelableCreatorSymbol == null) {
      return Description.NO_MATCH;
    }

    ClassType classType = ASTHelpers.getType(tree);
    for (Tree member : tree.getMembers()) {
      if (member.getKind() != Kind.VARIABLE) {
        continue;
      }

      VariableTree variableTree = (VariableTree) member;
      if (PARCELABLE_CREATOR_MATCHER.matches(variableTree, state)) {
        if (isVariableClassCreator(variableTree, state, classType, parcelableCreatorSymbol)) {
          return Description.NO_MATCH;
        }
      }
    }

    return describeMatch(tree);
  }

  private static boolean isVariableClassCreator(
      VariableTree variableTree,
      VisitorState state,
      ClassType classType,
      Symbol parcelableCreatorSymbol) {
    Tree typeTree = variableTree.getType();
    Type type = ASTHelpers.getType(typeTree);
    Types types = state.getTypes();
    Type superType = types.asSuper(type, parcelableCreatorSymbol);
    if (superType == null) {
      return false;
    }
    List<Type> typeArguments = superType.getTypeArguments();
    if (typeArguments.isEmpty()) {
      // raw creator
      return true;
    }
    return ASTHelpers.isSubtype(classType, Iterables.getOnlyElement(typeArguments), state);
  }
}
