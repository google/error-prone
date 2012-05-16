/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.refactors.collectionIncompatibleType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.refactors.RefactoringMatcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import java.util.Arrays;
import java.util.List;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "CollectionIncompatibleType",
    summary = "Java generic collections have non-generic methods...",
    explanation = "TODO",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class CollectionIncompatibleType extends RefactoringMatcher<MethodInvocationTree> {

  public static final List<String> COLLECTION_METHODS = Arrays
      .asList("contains(java.lang.Object)", "remove(java.lang.Object)");

  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree methodSelect = methodInvocationTree.getMethodSelect();
    if (!(methodSelect instanceof JCFieldAccess)) {
      return false;
    }
    JCFieldAccess methodSelectFieldAccess = (JCFieldAccess) methodSelect;
    if (!COLLECTION_METHODS.contains(methodSelectFieldAccess.sym.toString())) {
      return false;
    }

    Type collectionType = state.getSymtab().classes.get(state.getName("java.util.Collection")).type;
    Type accessedReferenceType = ((MethodSymbol) methodSelectFieldAccess.sym).owner.type;
    if (!state.getTypes().isCastable(accessedReferenceType, collectionType)) {
      return false;
    }

    Type collectionGenericType = ((ClassType) methodSelectFieldAccess.getExpression().type).typarams_field.get(0);
    Type arg0Type = ((JCMethodInvocation) methodInvocationTree).args.get(0).type;
    return !state.getTypes().isCastable(arg0Type, collectionGenericType);
  }

  @Override
  public Refactor refactor(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return new Refactor(methodInvocationTree, refactorMessage,
        new SuggestedFix().replace(methodInvocationTree, "false"));
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private CollectionIncompatibleType matcher = new CollectionIncompatibleType();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
