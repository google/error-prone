/*
 * Copyright 2018 Error Prone Authors. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

@BugPattern(
    name = "UnusedPrivateField",
    summary = "Unused private field",
    category = JDK,
    severity = SUGGESTION
)
public class UnusedPrivateField extends BugChecker implements BugChecker.MemberSelectTreeMatcher,
    BugChecker.VariableTreeMatcher, BugChecker
        .CompilationUnitTreeEndMatcher, BugChecker.IdentifierTreeMatcher {

  private final Set<VariableTree> declaredFields = new LinkedHashSet<>();
  private final Set<Symbol> usedFields = new LinkedHashSet<>();

  @Override
  public Set<Description> endCompilationUnit(
      CompilationUnitTree tree, VisitorState state) {
    ImmutableSet.Builder<Description> builder = ImmutableSet.builder();
    for (VariableTree decl: declaredFields) {
      Symbol s = ASTHelpers.getSymbol(decl);
      if (!usedFields.contains(s)) {
        builder.add(describeMatch(decl));
      }
    }
    declaredFields.clear();
    usedFields.clear();
    return builder.build();
  }

  @Override
  public Description matchMemberSelect(
      MemberSelectTree tree, VisitorState state) {
    return handlePossibleFieldRead(tree);
  }

  @Override
  public Description matchIdentifier(
      IdentifierTree tree, VisitorState state) {
    return handlePossibleFieldRead(tree);
  }

  @Override
  public Description matchVariable(
      VariableTree tree, VisitorState state) {
    Symbol s = ASTHelpers.getSymbol(tree);
    if (isPrivateField(s)) {
      declaredFields.add(tree);
    }
    return Description.NO_MATCH;
  }

  private Description handlePossibleFieldRead(Tree tree) {
    Symbol s = ASTHelpers.getSymbol(tree);
    if (isPrivateField(s)) {
      usedFields.add(s);
    }
    return Description.NO_MATCH;
  }

  private boolean isPrivateField(Symbol s) {
    return s.getKind().equals(ElementKind.FIELD) && s.isPrivate();
  }

}
