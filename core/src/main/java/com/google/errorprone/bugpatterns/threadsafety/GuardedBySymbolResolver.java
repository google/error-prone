/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.bugpatterns.threadsafety.IllegalGuardedBy.checkGuardedBy;

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import javax.lang.model.element.ElementKind;

/**
 * A symbol resolver used while binding guardedby expressions from string literals.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class GuardedBySymbolResolver implements GuardedByBinder.Resolver {

  private final ClassSymbol enclosingClass;
  private final Tree decl;
  private final JCTree.JCCompilationUnit compilationUnit;
  private final Context context;
  private final Types types;

  public static GuardedBySymbolResolver from(Tree tree, VisitorState visitorState) {
    return GuardedBySymbolResolver.from(
        ASTHelpers.getSymbol(tree).owner.enclClass(),
        visitorState.getPath().getCompilationUnit(),
        visitorState.context,
        tree);
  }

  public static GuardedBySymbolResolver from(
      ClassSymbol owner, CompilationUnitTree compilationUnit, Context context, Tree leaf) {
    return new GuardedBySymbolResolver(owner, compilationUnit, context, leaf);
  }

  private GuardedBySymbolResolver(
      ClassSymbol enclosingClass, CompilationUnitTree compilationUnit, Context context, Tree leaf) {
    this.compilationUnit = (JCCompilationUnit) compilationUnit;
    this.enclosingClass = enclosingClass;
    this.context = context;
    this.types = Types.instance(context);
    this.decl = leaf;
  }

  public Context context() {
    return context;
  }

  public ClassSymbol enclosingClass() {
    return enclosingClass;
  }

  @Override
  public Symbol resolveIdentifier(IdentifierTree node) {
    String name = node.getName().toString();

    if (name.equals("this")) {
      return enclosingClass;
    }

    // TODO(cushon): consider disallowing this? It's the only case where the lock description
    // isn't legal java.
    if (name.equals("itself")) {
      Symbol sym = ASTHelpers.getSymbol(decl);
      if (sym == null) {
        throw new IllegalGuardedBy(decl.getClass().toString());
      }
      return sym;
    }

    Symbol.VarSymbol field = getField(enclosingClass, name);
    if (field != null) {
      return field;
    }

    Symbol type = resolveType(name, SearchSuperTypes.YES);
    if (type != null) {
      return type;
    }

    throw new IllegalGuardedBy(name);
  }

  @Override
  public Symbol.MethodSymbol resolveMethod(
      MethodInvocationTree node, javax.lang.model.element.Name name) {
    return getMethod(enclosingClass, name.toString());
  }

  @Override
  public Symbol.MethodSymbol resolveMethod(
      MethodInvocationTree node,
      GuardedByExpression base,
      javax.lang.model.element.Name identifier) {
    Symbol baseSym =
        base.kind() == GuardedByExpression.Kind.THIS ? enclosingClass : base.type().asElement();
    return getMethod(baseSym, identifier.toString());
  }

  private Symbol.MethodSymbol getMethod(Symbol classSymbol, String name) {
    return getMember(Symbol.MethodSymbol.class, ElementKind.METHOD, classSymbol, name);
  }

  @Override
  public Symbol resolveSelect(GuardedByExpression base, MemberSelectTree node) {
    Symbol baseSym =
        base.kind() == GuardedByExpression.Kind.THIS ? enclosingClass : base.type().asElement();
    return getField(baseSym, node.getIdentifier().toString());
  }

  private Symbol.VarSymbol getField(Symbol classSymbol, String name) {
    return getMember(Symbol.VarSymbol.class, ElementKind.FIELD, classSymbol, name);
  }

  private <T extends Symbol> T getMember(
      Class<T> type, ElementKind kind, Symbol classSymbol, String name) {
    if (classSymbol.type == null) {
      return null;
    }
    for (Type t : types.closure(classSymbol.type)) {
      Scope scope = t.tsym.members();
      for (Symbol sym : scope.getSymbolsByName(getName(name))) {
        if (sym.getKind().equals(kind)) {
          return type.cast(sym);
        }
      }
    }
    if (classSymbol.owner != null
        && classSymbol != classSymbol.owner
        && classSymbol.owner instanceof Symbol.ClassSymbol) {
      T sym = getMember(type, kind, classSymbol.owner, name);
      if (sym != null) {
        return sym;
      }
    }
    if (classSymbol.hasOuterInstance()) {
      T sym = getMember(type, kind, classSymbol.type.getEnclosingType().asElement(), name);
      if (sym != null) {
        return sym;
      }
    }
    return null;
  }

  @Override
  public Symbol resolveTypeLiteral(ExpressionTree expr) {
    checkGuardedBy(expr instanceof IdentifierTree, "bad type literal: %s", expr);
    IdentifierTree ident = (IdentifierTree) expr;
    Symbol type = resolveType(ident.getName().toString(), SearchSuperTypes.YES);
    if (type instanceof Symbol.ClassSymbol) {
      return type;
    }
    return null;
  }

  private static enum SearchSuperTypes {
    YES,
    NO
  }

  /**
   * Resolves a simple name as a type. Considers super classes, lexically enclosing classes, and
   * then arbitrary types available in the current environment.
   */
  private Symbol resolveType(String name, SearchSuperTypes searchSuperTypes) {
    Symbol type = null;
    if (searchSuperTypes == SearchSuperTypes.YES) {
      type = getSuperType(enclosingClass, name);
    }
    if (enclosingClass.getSimpleName().contentEquals(name)) {
      type = enclosingClass;
    }
    if (type == null) {
      type = getLexicallyEnclosing(enclosingClass, name);
    }
    if (type == null) {
      type = attribIdent(name);
    }
    checkGuardedBy(
        !(type instanceof Symbol.PackageSymbol),
        "All we could find for '%s' was a package symbol.",
        name);
    return type;
  }

  private Symbol getSuperType(Symbol symbol, String name) {
    for (Type t : types.closure(symbol.type)) {
      if (t.asElement().getSimpleName().contentEquals(name)) {
        return t.asElement();
      }
    }
    return null;
  }

  private Symbol getLexicallyEnclosing(ClassSymbol symbol, String name) {
    Symbol current = symbol.owner;
    while (true) {
      if (current == null || current.getSimpleName().contentEquals(name)) {
        return current;
      }

      if (current != current.owner && current.owner instanceof Symbol.ClassSymbol) {
        current = current.owner;
      } else {
        return null;
      }
    }
  }

  private Symbol attribIdent(String name) {
    Attr attr = Attr.instance(context);
    TreeMaker tm = TreeMaker.instance(context);
    return attr.attribIdent(tm.Ident(getName(name)), compilationUnit);
  }

  private Name getName(String name) {
    return Names.instance(context).fromString(name);
  }

  @Override
  public Symbol resolveEnclosingClass(ExpressionTree expr) {
    checkGuardedBy(expr instanceof IdentifierTree, "bad type literal: %s", expr);
    IdentifierTree ident = (IdentifierTree) expr;
    Symbol type = resolveType(ident.getName().toString(), SearchSuperTypes.NO);
    if (type instanceof Symbol.ClassSymbol) {
      return type;
    }
    return null;
  }
}
