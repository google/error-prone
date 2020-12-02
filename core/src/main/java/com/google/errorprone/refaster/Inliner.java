/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.SubContext;
import com.google.errorprone.refaster.UTypeVar.TypeWithExpression;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Infer;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.Map;
import java.util.Set;

/**
 * A context representing all the dependencies necessary to reconstruct a pretty-printable source
 * tree from a {@code UTree} based on a set of substitutions.
 *
 * @author Louis Wasserman
 */
public final class Inliner {
  private final Context context;
  private final Set<String> importsToAdd;
  private final Set<String> staticImportsToAdd;
  public final Bindings bindings;

  private final Map<String, TypeVar> typeVarCache;

  public Inliner(Context context, Bindings bindings) {
    this.context = new SubContext(context);
    this.bindings = new Bindings(bindings).unmodifiable();
    this.importsToAdd = Sets.newHashSet();
    this.staticImportsToAdd = Sets.newHashSet();
    this.typeVarCache = Maps.newHashMap();
  }

  public void addImport(String qualifiedImport) {
    if (!qualifiedImport.startsWith("java.lang")) {
      importsToAdd.add(qualifiedImport);
    }
  }

  public void addStaticImport(String qualifiedImport) {
    staticImportsToAdd.add(qualifiedImport);
  }

  public ClassSymbol resolveClass(CharSequence qualifiedClass)
      throws CouldNotResolveImportException {
    try {
      Symbol symbol =
          JavaCompiler.instance(context).resolveBinaryNameOrIdent(qualifiedClass.toString());
      if (symbol.equals(symtab().errSymbol) || !(symbol instanceof ClassSymbol)) {
        throw new CouldNotResolveImportException(qualifiedClass);
      } else {
        return (ClassSymbol) symbol;
      }
    } catch (NullPointerException e) {
      throw new CouldNotResolveImportException(qualifiedClass);
    }
  }

  public Context getContext() {
    return context;
  }

  public Types types() {
    return Types.instance(context);
  }

  public Symtab symtab() {
    return Symtab.instance(context);
  }

  public Enter enter() {
    return Enter.instance(context);
  }

  public Names names() {
    return Names.instance(context);
  }

  public TreeMaker maker() {
    return TreeMaker.instance(context);
  }

  public Infer infer() {
    return Infer.instance(context);
  }

  public ImportPolicy importPolicy() {
    return ImportPolicy.instance(context);
  }

  public Name asName(CharSequence str) {
    return names().fromString(str.toString());
  }

  private static final Types.SimpleVisitor<JCExpression, Inliner> INLINE_AS_TREE =
      new Types.SimpleVisitor<JCExpression, Inliner>() {
        @Override
        public JCExpression visitType(Type t, Inliner inliner) {
          return inliner.maker().Type(t);
        }

        @Override
        public JCExpression visitClassType(ClassType type, Inliner inliner) {
          ClassSymbol classSym = (ClassSymbol) type.tsym;
          JCExpression classExpr =
              inliner
                  .importPolicy()
                  .classReference(
                      inliner,
                      ASTHelpers.outermostClass(classSym).getQualifiedName().toString(),
                      classSym.getQualifiedName().toString());
          List<JCExpression> argExprs = List.nil();
          for (Type argType : type.getTypeArguments()) {
            argExprs = argExprs.append(visit(argType, inliner));
          }
          return argExprs.isEmpty() ? classExpr : inliner.maker().TypeApply(classExpr, argExprs);
        }

        @Override
        public JCExpression visitWildcardType(WildcardType type, Inliner inliner) {
          TreeMaker maker = inliner.maker();
          return maker.Wildcard(maker.TypeBoundKind(type.kind), visit(type.type, inliner));
        }

        @Override
        public JCExpression visitArrayType(ArrayType type, Inliner inliner) {
          return inliner.maker().TypeArray(visit(type.getComponentType(), inliner));
        }
      };

  /** Inlines the syntax tree representing the specified type. */
  public JCExpression inlineAsTree(Type type) {
    return INLINE_AS_TREE.visit(type, this);
  }

  public <V> V getBinding(Bindings.Key<V> key) {
    V value = bindings.getBinding(key);
    if (value == null) {
      throw new IllegalStateException("No binding for " + key);
    }
    return value;
  }

  public <V> Optional<V> getOptionalBinding(Bindings.Key<V> key) {
    return Optional.fromNullable(bindings.getBinding(key));
  }

  public <R> com.sun.tools.javac.util.List<R> inlineList(
      Iterable<? extends Inlineable<? extends R>> elements) throws CouldNotResolveImportException {
    ListBuffer<R> result = new ListBuffer<>();
    for (Inlineable<? extends R> e : elements) {
      if (e instanceof URepeated) {
        // URepeated is bound to a list of expressions.
        URepeated repeated = (URepeated) e;
        for (JCExpression expr : getBinding(repeated.key())) {
          @SuppressWarnings("unchecked")
          // URepeated is an Inlineable<JCExpression>, so if e is also an Inlineable<? extends R>,
          // then R must be ? super JCExpression.
          R r = (R) expr;
          result.append(r);
        }
      } else {
        result.append(e.inline(this));
      }
    }
    return result.toList();
  }

  public Set<String> getImportsToAdd() {
    return ImmutableSet.copyOf(importsToAdd);
  }

  public Set<String> getStaticImportsToAdd() {
    return ImmutableSet.copyOf(staticImportsToAdd);
  }

  public TypeVar inlineAsVar(UTypeVar var) throws CouldNotResolveImportException {
    /*
     * In order to handle recursively bounded type variables without a stack overflow,
     * we first cache a type var with no bounds, then we inline the bounds.
     */
    TypeVar typeVar = typeVarCache.get(var.getName());
    if (typeVar != null) {
      return typeVar;
    }
    Name name = asName(var.getName());
    TypeSymbol sym = new TypeVariableSymbol(0, name, null, symtab().noSymbol);
    typeVar = new TypeVar(sym, /* bound= */ null, /* lower= */ symtab().botType);
    sym.type = typeVar;
    typeVarCache.put(var.getName(), typeVar);
    // Any recursive uses of var will point to the same TypeVar object generated above.
    setUpperBound(typeVar, var.getUpperBound().inline(this));
    typeVar.lower = var.getLowerBound().inline(this);
    return typeVar;
  }

  private static void setUpperBound(TypeVar typeVar, Type bound) {
    // https://bugs.openjdk.java.net/browse/JDK-8193367
    try {
      TypeVar.class.getMethod("setUpperBound", Type.class).invoke(typeVar, bound);
      return;
    } catch (ReflectiveOperationException e) {
      // continue below
    }
    try {
      TypeVar.class.getField("bound").set(typeVar, bound);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  Type inlineTypeVar(UTypeVar var) throws CouldNotResolveImportException {
    Optional<TypeWithExpression> typeVarBinding = getOptionalBinding(var.key());
    if (typeVarBinding.isPresent()) {
      return typeVarBinding.get().type();
    } else {
      return inlineAsVar(var);
    }
  }
}
