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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * @author alexloh@google.com (Alex Loh)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "ClassCanBeStatic",
  summary = "Inner class is non-static but does not reference enclosing class",
  explanation =
      "An inner class should be static unless it references members"
          + "of its enclosing class. An inner class that is made non-static unnecessarily"
          + "uses more memory and does not make the intent of the class clear.",
  category = JDK,
  maturity = EXPERIMENTAL,
  severity = ERROR
)
public class ClassCanBeStatic extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(final ClassTree tree, final VisitorState state) {
    final ClassSymbol currentClass = ASTHelpers.getSymbol(tree);
    if (!currentClass.hasOuterInstance()) {
      return Description.NO_MATCH;
    }
    if (currentClass.getNestingKind() != NestingKind.MEMBER) {
      // local or anonymous classes can't be static
      return Description.NO_MATCH;
    }
    if (currentClass.owner.enclClass().hasOuterInstance()) {
      // class is nested inside an inner class, so it can't be static
      return Description.NO_MATCH;
    }
    if (OuterReferenceScanner.scan((JCTree) tree, currentClass, state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree, SuggestedFix.addModifier(tree, Modifier.STATIC, state));
  }

  private static class OuterReferenceScanner extends TreeScanner {

    private static boolean scan(JCTree tree, ClassSymbol currentClass, VisitorState state) {
      OuterReferenceScanner scanner = new OuterReferenceScanner(currentClass, state);
      tree.accept(scanner);
      return scanner.referencesOuter;
    }

    private final Names names;
    private final ClassSymbol currentClass;
    private final VisitorState state;

    private boolean referencesOuter = false;

    public OuterReferenceScanner(ClassSymbol currentClass, VisitorState state) {
      this.currentClass = currentClass;
      this.state = state;
      this.names = Names.instance(state.context);
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
      // check for unqualified references to instance members (fields and methods) declared
      // in an enclosing scope
      if (tree.sym.isStatic()) {
        return;
      }
      switch (tree.sym.getKind()) {
        case FIELD:
        case METHOD:
          break;
        default:
          return;
      }
      if (!tree.sym.isMemberOf(currentClass, state.getTypes())) {
        referencesOuter = true;
      }
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess tree) {
      // check for qualified this references
      if (tree.name == names._this) {
        referencesOuter = true;
      }
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass that) {
      // check for constructor invocations where the type is a member of an enclosing class,
      // the enclosing instance is passed as an explicit argument
      Symbol sym = ASTHelpers.getSymbol(that);
      if (sym.isStatic()) {
        return;
      }
      for (ClassSymbol encl = currentClass.owner.enclClass();
          encl != null;
          encl = encl.owner != null ? encl.owner.enclClass() : null) {
        if (sym.isMemberOf(encl, state.getTypes())) {
          referencesOuter = true;
          break;
        }
      }
    }
  }
}
