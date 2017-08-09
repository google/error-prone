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

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Names;

/** Analyzes trees for references to their enclosing instance. */
public class CanBeStaticAnalyzer extends TreeScanner {

  /** Returns true if the tree references its enclosing class. */
  public static boolean referencesOuter(Tree tree, Symbol owner, VisitorState state) {
    CanBeStaticAnalyzer scanner = new CanBeStaticAnalyzer(owner, state);
    ((JCTree) tree).accept(scanner);
    return scanner.referencesOuter;
  }

  private final Names names;
  private final Symbol owner;
  private final VisitorState state;

  private boolean referencesOuter = false;

  private CanBeStaticAnalyzer(Symbol owner, VisitorState state) {
    this.owner = owner;
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
        if (!isOwnedBy(tree.sym, owner, state.getTypes())) {
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

  private boolean isOwnedBy(Symbol sym, Symbol owner, Types types) {
    if (sym.owner == owner) {
      return true;
    }
    if (owner instanceof TypeSymbol) {
      return sym.isMemberOf((TypeSymbol) owner, types);
    }
    return false;
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
    if (memberOfEnclosing(owner, state, type.tsym)) {
      referencesOuter = true;
    }
  }

  @Override
  public void visitReference(JCMemberReference tree) {
    super.visitReference(tree);
    if (tree.getMode() != ReferenceMode.NEW) {
      return;
    }
    if (memberOfEnclosing(owner, state, tree.expr.type.tsym)) {
      referencesOuter = true;
    }
  }

  /** Is sym a non-static member of an enclosing class of currentClass? */
  static boolean memberOfEnclosing(Symbol owner, VisitorState state, Symbol sym) {
    if (sym == null || !sym.hasOuterInstance()) {
      return false;
    }
    for (ClassSymbol encl = owner.owner.enclClass();
        encl != null;
        encl = encl.owner != null ? encl.owner.enclClass() : null) {
      if (sym.isMemberOf(encl, state.getTypes())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitAnnotation(JCAnnotation tree) {
    // skip annotations; the keys of key/value pairs look like unqualified method invocations
  }
}
