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
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.refactors.RefactoringMatcher;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.Name;

import java.util.Arrays;
import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "CollectionIncompatibleType",
    summary = "Java generic collections have non-generic methods...",
    explanation = "...",
category = Category.JDK, maturity = MaturityLevel.EXPERIMENTAL, severity = SeverityLevel.ERROR)
public class CollectionIncompatibleType extends RefactoringMatcher<MethodInvocationTree> {

  public static final List<String> COLLECTION_METHODS = Arrays
      .asList("contains(java.lang.Object)", "remove(java.lang.Object)");

  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!(methodInvocationTree instanceof JCMethodInvocation)) {
      return false;
    }
    if (!(methodInvocationTree.getMethodSelect() instanceof JCFieldAccess)) {
      return false;
    }
    JCExpression expression = ((JCFieldAccess) methodInvocationTree.getMethodSelect())
        .getExpression();
    Type collectionGenericType = ((ClassType) expression.type).typarams_field.get(0);

    JCExpression meth = ((JCMethodInvocation) methodInvocationTree).meth;
    if (!(meth instanceof  JCFieldAccess)) {
      return false;
    }
    Symbol method = ((JCFieldAccess) meth).sym;

    ClassSymbol owner = (ClassSymbol) ((MethodSymbol) method).owner;
    Name collectionName = Name.fromString(state.getNameTable(), "java.util.Collection");
    Type collectionType = state.getSymtab().classes.get(collectionName).type;

    Type listType = owner.type;

    boolean isSubtype = state.getTypes().isCastable(listType, collectionType);

    if (isSubtype && COLLECTION_METHODS.contains(method.toString())) {

    } else {
      return false;
    }

    JCExpression arg0 = ((JCMethodInvocation) methodInvocationTree).args.get(0);
    if (arg0.type.equals(collectionGenericType)) {
      return false;
    }
    return true;
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
