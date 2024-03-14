/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.ElementKind;

/** See the summary. */
@BugPattern(summary = "Possible class initialization deadlock", severity = WARNING)
public class ClassInitializationDeadlock extends BugChecker implements BugChecker.ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol classSymbol = getSymbol(tree);
    if (classSymbol.isInterface()
        && !ASTHelpers.scope(classSymbol.members())
            .anyMatch(ClassInitializationDeadlock::defaultMethod)) {
      // Interfaces are only recursively initialized by their subtypes if they declare any
      // non-abstract, non-static (i.e. default) methods, see JVMS 5.5.
      // This heuristic ignores interfaces that declare default methods directly, to be fully
      // correct we should consider arbitrary paths between the subtype and the interface that
      // pass through an intermediate interface with default methods.
      return NO_MATCH;
    }
    new SuppressibleTreePathScanner<Void, Void>(state) {

      @Override
      public Void visitClass(ClassTree node, Void unused) {
        scan(node.getMembers(), null);
        return null;
      }

      @Override
      public Void visitBlock(BlockTree tree, Void unused) {
        if (tree.isStatic()) {
          scanForSubtypes(getCurrentPath(), classSymbol, state);
        }
        return null;
      }

      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        if (getSymbol(tree).isStatic()) {
          scanForSubtypes(getCurrentPath(), classSymbol, state);
        }
        return null;
      }
    }.scan(state.getPath(), null);
    return NO_MATCH;
  }

  private void scanForSubtypes(TreePath path, ClassSymbol classSymbol, VisitorState state) {
    new TreePathScanner<Void, Void>() {

      @Override
      public Void visitClass(ClassTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        if (ASTHelpers.constValue(tree) != null) {
          return null;
        }
        if (tree.getIdentifier().contentEquals("class")) {
          return null;
        }
        handle(tree);
        return super.visitMemberSelect(tree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (ASTHelpers.constValue(tree) != null) {
          return null;
        }
        handle(tree);
        return null;
      }

      private void handle(ExpressionTree tree) {
        Symbol use = getSymbol(tree);
        if (!(use instanceof ClassSymbol)) {
          return;
        }
        if (use.equals(classSymbol)) {
          return;
        }
        if (isEffectivelyPrivate(use)
            && ((ClassSymbol) use).getSuperclass().asElement().equals(classSymbol)) {
          // Cycles involving `private` member classes are usually benign, because the private
          // class cannot be directly accessed from outside the current file, so typically accessing
          // it requires first accessing the enclosing class.
          //
          // See also discussion of `private` classes in
          // https://errorprone.info/bugpattern/ClassInitializationDeadlock
          return;
        }
        if (use.getSimpleName().toString().startsWith("AutoValue_")) {
          // Cycles involving AutoValue-generated code are usually benign, because the generated
          // code should only be accessed within the declaration of the corresponding base class,
          // so the base class will always be initialized first.
          //
          // See also discussion of AutoValue in
          // https://errorprone.info/bugpattern/ClassInitializationDeadlock
          return;
        }
        if (use.isSubClass(classSymbol, state.getTypes())) {
          state.reportMatch(
              buildDescription(tree)
                  .setMessage(
                      String.format(
                          "Possible class initialization deadlock: %s is a subclass of the"
                              + " containing class %s",
                          use, classSymbol))
                  .build());
        }
      }
    }.scan(path, null);
  }

  private static boolean defaultMethod(Symbol s) {
    return s.getKind().equals(ElementKind.METHOD) && ((Symbol.MethodSymbol) s).isDefault();
  }
}
