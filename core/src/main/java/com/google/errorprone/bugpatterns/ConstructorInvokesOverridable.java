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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/**
 * Looks for invocations of overridable methods from constructors and similar scopes (instance
 * initializers and variables).
 */
@BugPattern(
    name = "ConstructorInvokesOverridable",
    summary = "Constructors should not invoke overridable methods.",
    category = JDK,
    severity = WARNING)
public class ConstructorInvokesOverridable extends ConstructorLeakChecker {

  @Override
  protected void traverse(Tree tree, VisitorState state) {
    // If class is final, no method is overridable.
    ClassTree classTree = state.findEnclosing(ClassTree.class);
    if (classTree.getModifiers().getFlags().contains(Modifier.FINAL)) {
      return;
    }
    ClassSymbol classSym = ASTHelpers.getSymbol(classTree);

    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree node, Void data) {
            MethodSymbol method = ASTHelpers.getSymbol(node);
            if (method != null
                && !method.isConstructor()
                && !method.isStatic()
                && !method.isPrivate()
                && !method.getModifiers().contains(Modifier.FINAL)
                && isOnThis(node)
                && method.isMemberOf(classSym, state.getTypes())) {
              state.reportMatch(describeMatch(node));
            }
            return super.visitMethodInvocation(node, data);
          }
        },
        null);
  }

  private static boolean isOnThis(MethodInvocationTree tree) {
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    if (receiver == null) {
      return true;
    }
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
    return receiverName.contentEquals("this") || receiverName.contentEquals("super");
  }
}
