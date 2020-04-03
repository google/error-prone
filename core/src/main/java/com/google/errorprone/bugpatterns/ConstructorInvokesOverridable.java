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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/**
 * Looks for invocations of overridable methods from constructors and similar scopes (instance
 * initializers and variables).
 */
@BugPattern(
    name = "ConstructorInvokesOverridable",
    summary = "Constructors should not invoke overridable methods.",
    severity = WARNING)
public class ConstructorInvokesOverridable extends ConstructorLeakChecker {

  private static final ImmutableSet<Modifier> ENUM_CONSTANT_MODIFIERS =
      Sets.immutableEnumSet(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

  @Override
  protected void traverse(Tree tree, VisitorState state) {
    // If class is final, no method is overridable.
    ClassTree classTree = state.findEnclosing(ClassTree.class);
    if (classTree.getModifiers().getFlags().contains(Modifier.FINAL)) {
      return;
    }
    ClassSymbol classSym = ASTHelpers.getSymbol(classTree);
    // If the class is anonymous, no method is overridable.
    //
    //   "it is impossible to declare a subclass of an anonymous class, despite
    //    an anonymous class being non-final, because an anonymous class cannot
    //    be named by an extends clause"
    //
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.9.5
    if (classSym.isAnonymous()) {
      return;
    }
    //  "An enum declaration is implicitly final unless it contains at least one
    //   enum constant that has a class body"
    //
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html#jls-8.9
    if (classSym.isEnum()) {
      boolean hasSubclass =
          classTree.getMembers().stream()
              .filter(VariableTree.class::isInstance)
              .map(VariableTree.class::cast)
              .filter(
                  variableTree ->
                      variableTree.getModifiers().getFlags().containsAll(ENUM_CONSTANT_MODIFIERS))
              .filter(
                  variableTree ->
                      state.getTypes().isSameType(classSym.type, ASTHelpers.getType(variableTree)))
              .map(VariableTree::getInitializer)
              .filter(NewClassTree.class::isInstance)
              .map(NewClassTree.class::cast)
              .anyMatch(newClassTree -> newClassTree.getClassBody() != null);

      if (!hasSubclass) {
        return;
      }
    }

    Deque<ClassSymbol> nestedClasses = new ArrayDeque<>();

    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitClass(ClassTree node, Void unused) {
            if (isSuppressed(node)) {
              return null;
            }
            nestedClasses.push(ASTHelpers.getSymbol(node));
            super.visitClass(node, null);
            nestedClasses.pop();
            return null;
          }

          @Override
          public Void visitMethod(MethodTree node, Void unused) {
            if (isSuppressed(node)) {
              return null;
            }
            return super.visitMethod(node, null);
          }

          @Override
          public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            if (isOverridable(node)) {
              state.reportMatch(describeMatch(node));
            }
            return super.visitMethodInvocation(node, null);
          }

          @Override
          public Void visitVariable(VariableTree node, Void unused) {
            if (isSuppressed(node)) {
              return null;
            }
            return super.visitVariable(node, null);
          }

          private boolean isOverridable(MethodInvocationTree node) {
            MethodSymbol method = ASTHelpers.getSymbol(node);
            if (method == null
                || method.isConstructor()
                || method.isStatic()
                || method.isPrivate()
                || method.getModifiers().contains(Modifier.FINAL)
                || !method.isMemberOf(classSym, state.getTypes())) {
              return false;
            }

            ExpressionTree receiver = ASTHelpers.getReceiver(node);
            if (receiver != null) {
              Name receiverName;
              switch (receiver.getKind()) {
                case IDENTIFIER:
                  receiverName = ((IdentifierTree) receiver).getName();
                  break;
                case MEMBER_SELECT:
                  receiverName = ((MemberSelectTree) receiver).getIdentifier();
                  break;
                default:
                  return false;
              }
              return (receiverName.contentEquals("this") || receiverName.contentEquals("super"))
                  && classSym.equals(ASTHelpers.getReceiverType(node).tsym);
            }

            for (ClassSymbol nestedClass : nestedClasses) {
              if (method.isMemberOf(nestedClass, state.getTypes())) {
                return false;
              }
            }

            return true;
          }
        },
        null);
  }
}
