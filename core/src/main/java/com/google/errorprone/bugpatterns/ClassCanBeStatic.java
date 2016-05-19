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
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
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
    if (currentClass == null || !currentClass.hasOuterInstance()) {
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
    if (tree.getExtendsClause() != null) {
      Type extendsType = ASTHelpers.getType(tree.getExtendsClause());
      if (memberOfEnclosing(currentClass, state, extendsType.tsym)) {
        return Description.NO_MATCH;
      }
    }
    if (OuterReferenceScanner.scan((JCTree) tree, currentClass, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, SuggestedFixes.addModifiers(tree, state, Modifier.STATIC));
  }

  /** Is sym a non-static member of an enclosing class of currentClass? */
  private static boolean memberOfEnclosing(
      ClassSymbol currentClass, VisitorState state, Symbol sym) {
    if (sym == null || !sym.hasOuterInstance()) {
      return false;
    }
    for (ClassSymbol encl = currentClass.owner.enclClass();
        encl != null;
        encl = encl.owner != null ? encl.owner.enclClass() : null) {
      if (sym.isMemberOf(encl, state.getTypes())) {
        return true;
      }
    }
    return false;
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
        case TYPE_PARAMETER:
          // declaring a class as non-static just to access a type parameter is silly -
          // why not just re-declare the type parameter instead of capturing it?
          // TODO(cushon): consider making the suggestion anyways, maybe with a fix?
          // fall through
        case FIELD:
        case METHOD:
          if (!tree.sym.isMemberOf(currentClass, state.getTypes())) {
            referencesOuter = true;
          }
          break;
        case CLASS:
          Type enclosing = tree.type.getEnclosingType();
          if (enclosing != null) {
            enclosing.accept(new TypeVariableScanner(), null);
          }
          break;
        default:
          break;
      }
    }

    // check for implicit references to type parameters of the enclosing
    // class in unqualified references to sibling types, e.g.:
    //
    // class Test<T> {
    //   class One {}
    //   class Two {
    //     One one; // implicit Test<T>.One
    //   }
    // }
    private class TypeVariableScanner extends Types.SimpleVisitor<Void, Void> {
      @Override
      public Void visitTypeVar(Type.TypeVar t, Void aVoid) {
        referencesOuter = true;
        return null;
      }

      @Override
      public Void visitClassType(Type.ClassType t, Void aVoid) {
        for (Type a : t.getTypeArguments()) {
          a.accept(this, null);
        }
        if (t.getEnclosingType() != null) {
          t.getEnclosingType().accept(this, null);
        }
        return null;
      }

      @Override
      public Void visitType(Type type, Void unused) {
        return null;
      }
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess tree) {
      super.visitSelect(tree);
      // check for qualified this/super references
      if (tree.name == names._this || tree.name == names._super) {
        referencesOuter = true;
      }
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass tree) {
      super.visitNewClass(tree);
      // check for constructor invocations where the type is a member of an enclosing class,
      // the enclosing instance is passed as an explicit argument
      Type type = ASTHelpers.getType(tree.clazz);
      if (type == null) {
        return;
      }
      if (memberOfEnclosing(currentClass, state, type.tsym)) {
        referencesOuter = true;
      }
    }
  }
}
