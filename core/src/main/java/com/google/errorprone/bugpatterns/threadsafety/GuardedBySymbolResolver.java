/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.bugpatterns.threadsafety.IllegalGuardedBy.checkGuardedBy;
import static java.util.Objects.requireNonNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
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
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;

/**
 * A symbol resolver used while binding guardedby expressions from string literals.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class GuardedBySymbolResolver implements GuardedByBinder.Resolver {

  private final ClassSymbol enclosingClass;
  private final MethodInfo method;
  private final Tree decl;
  private final JCTree.JCCompilationUnit compilationUnit;
  private final Context context;
  private final VisitorState visitorState;
  private final Types types;

  public static GuardedBySymbolResolver from(Tree tree, VisitorState visitorState) {
    return GuardedBySymbolResolver.from(
        ASTHelpers.getSymbol(tree).owner.enclClass(),
        MethodInfo.create(tree, visitorState),
        visitorState.getPath().getCompilationUnit(),
        visitorState.context,
        tree,
        visitorState);
  }

  public static GuardedBySymbolResolver from(
      ClassSymbol owner,
      MethodInfo method,
      CompilationUnitTree compilationUnit,
      Context context,
      Tree leaf,
      VisitorState visitorState) {
    return new GuardedBySymbolResolver(owner, method, compilationUnit, context, leaf, visitorState);
  }

  private GuardedBySymbolResolver(
      ClassSymbol enclosingClass,
      MethodInfo method,
      CompilationUnitTree compilationUnit,
      Context context,
      Tree leaf,
      VisitorState visitorState) {
    this.compilationUnit = (JCCompilationUnit) compilationUnit;
    this.enclosingClass = requireNonNull(enclosingClass);
    this.method = method;
    this.context = context;
    this.types = visitorState.getTypes();
    this.decl = leaf;
    this.visitorState = visitorState;
  }

  public Context context() {
    return context;
  }

  public VisitorState visitorState() {
    return visitorState;
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

    VarSymbol field = getField(enclosingClass, name);
    if (field != null) {
      return field;
    }

    VarSymbol param = getParam(method, name);
    if (param != null) {
      return param;
    }

    Symbol type = resolveType(name, SearchSuperTypes.YES);
    if (type != null) {
      return type;
    }

    throw new IllegalGuardedBy(name);
  }

  @Override
  public MethodSymbol resolveMethod(MethodInvocationTree node, Name name) {
    return getMethod(enclosingClass, name.toString());
  }

  @Override
  public MethodSymbol resolveMethod(
      MethodInvocationTree node, GuardedByExpression base, Name identifier) {
    Symbol baseSym =
        base.kind() == GuardedByExpression.Kind.THIS ? enclosingClass : base.type().asElement();
    return getMethod(baseSym, identifier.toString());
  }

  private MethodSymbol getMethod(Symbol classSymbol, String name) {
    return getMember(MethodSymbol.class, ElementKind.METHOD, classSymbol, name);
  }

  @Override
  public Symbol resolveSelect(GuardedByExpression base, MemberSelectTree node) {
    Symbol baseSym =
        base.kind() == GuardedByExpression.Kind.THIS ? enclosingClass : base.type().asElement();
    return getField(baseSym, node.getIdentifier().toString());
  }

  private VarSymbol getField(Symbol classSymbol, String name) {
    return getMember(VarSymbol.class, ElementKind.FIELD, classSymbol, name);
  }

  private <T extends Symbol> T getMember(
      Class<T> type, ElementKind kind, Symbol classSymbol, String name) {
    if (classSymbol.type == null) {
      return null;
    }
    for (Type t : types.closure(classSymbol.type)) {
      Scope scope = t.tsym.members();
      for (Symbol sym : scope.getSymbolsByName(visitorState.getName(name))) {
        if (sym.getKind().equals(kind)) {
          return type.cast(sym);
        }
      }
    }
    if (classSymbol.hasOuterInstance()) {
      T sym = getMember(type, kind, classSymbol.type.getEnclosingType().asElement(), name);
      if (sym != null) {
        return sym;
      }
    }
    if (classSymbol.owner != null
        && classSymbol != classSymbol.owner
        && classSymbol.owner instanceof Symbol.ClassSymbol) {
      T sym = getMember(type, kind, classSymbol.owner, name);
      if (sym != null && sym.isStatic()) {
        return sym;
      }
    }
    return null;
  }

  @Nullable
  private VarSymbol getParam(@Nullable MethodInfo method, String name) {
    if (method == null) {
      return null;
    }
    int idx = 0;
    for (VarSymbol param : method.sym().getParameters()) {
      if (!param.getSimpleName().contentEquals(name)) {
        idx++;
        continue;
      }
      ExpressionTree arg = method.argument(idx);
      if (arg != null) {
        Symbol sym = ASTHelpers.getSymbol(arg);
        if (sym instanceof VarSymbol) {
          return (VarSymbol) sym;
        }
      }
      return param;
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

  private enum SearchSuperTypes {
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

  private static Symbol getLexicallyEnclosing(ClassSymbol symbol, String name) {
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
    TreeMaker tm = visitorState.getTreeMaker();
    return attr.attribIdent(tm.Ident(visitorState.getName(name)), compilationUnit);
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

  /** Information about a method that is associated with a {@link GuardedBy} annotation. */
  @AutoValue
  abstract static class MethodInfo {
    /** The method symbol. */
    abstract MethodSymbol sym();

    /**
     * The method arguments, if the site is a method invocation expression for a method annotated
     * with {@code @GuardedBy}.
     */
    @Nullable
    abstract ImmutableList<ExpressionTree> arguments();

    @Nullable
    ExpressionTree argument(int idx) {
      return arguments() != null ? arguments().get(idx) : null;
    }

    static MethodInfo create(MethodSymbol sym) {
      return create(sym, null);
    }

    static MethodInfo create(MethodSymbol sym, ImmutableList<ExpressionTree> arguments) {
      // There may be more arguments than parameters due to varargs, but there shouldn't be fewer
      checkArgument(arguments == null || arguments.size() >= sym.getParameters().size());
      return new AutoValue_GuardedBySymbolResolver_MethodInfo(sym, arguments);
    }

    static MethodInfo create(Tree tree, VisitorState visitorState) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (!(sym instanceof MethodSymbol)) {
        return null;
      }
      MethodSymbol methodSym = (MethodSymbol) sym;
      Tree parent = visitorState.getPath().getParentPath().getLeaf();
      if (!(parent instanceof MethodInvocationTree)) {
        return create(methodSym);
      }
      MethodInvocationTree invocation = (MethodInvocationTree) parent;
      if (!invocation.getMethodSelect().equals(tree)) {
        return create(methodSym);
      }
      return create(
          methodSym, ImmutableList.copyOf(((MethodInvocationTree) parent).getArguments()));
    }
  }
}
