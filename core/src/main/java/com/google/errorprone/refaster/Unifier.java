/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sun.tools.javac.util.List.nil;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// A mutable representation of an attempt to match a template source tree against a target source
/// tree.
///
/// @author Louis Wasserman
public final class Unifier {
  /** Per-compilation-unit cached javac services shared across template match attempts. */
  static final class MatchContext {
    private final Context context;
    private final JCCompilationUnit compilationUnit;
    private final Types types;
    private final TreeMaker treeMaker;
    private final JavaCompiler compiler;
    private final Symtab symtab;
    private final VisitorState visitorState;
    private final Map<String, Optional<ClassSymbol>> resolvedClassCache = new HashMap<>();

    MatchContext(Context context, JCCompilationUnit compilationUnit) {
      this.context = checkNotNull(context);
      this.compilationUnit = checkNotNull(compilationUnit);
      this.types = Types.instance(context);
      this.treeMaker = TreeMaker.instance(context);
      this.compiler = JavaCompiler.instance(context);
      this.symtab = Symtab.instance(context);
      this.visitorState = VisitorState.createForUtilityPurposes(context);
    }

    Context context() {
      return context;
    }

    JCCompilationUnit compilationUnit() {
      return compilationUnit;
    }

    Types types() {
      return types;
    }

    TreeMaker treeMaker() {
      return treeMaker;
    }

    VisitorState visitorState() {
      return visitorState;
    }

    @Nullable ClassSymbol resolveClassOrNull(String qualifiedClass) {
      Optional<ClassSymbol> cached = resolvedClassCache.get(qualifiedClass);
      if (cached != null) {
        return cached.orElse(null);
      }
      ClassSymbol resolved = null;
      try {
        Symbol symbol = compiler.resolveBinaryNameOrIdent(qualifiedClass);
        if (symbol != null
            && !symbol.equals(symtab.errSymbol)
            && symbol instanceof ClassSymbol classSymbol) {
          resolved = classSymbol;
        }
      } catch (NullPointerException ignored) {
        resolved = null;
      }
      resolvedClassCache.put(qualifiedClass, Optional.ofNullable(resolved));
      return resolved;
    }
  }

  private final Bindings bindings;

  private final Context context;
  private final Types types;
  private final TreeMaker treeMaker;
  private final JCCompilationUnit compilationUnit;
  private final @Nullable MatchContext matchContext;
  private final @Nullable TreePath currentPath;
  private final @Nullable ImmutableMap<String, UType> expectedArgTypes;
  private final boolean autoboxing;

  public Unifier(Context context, JCCompilationUnit compilationUnit) {
    this(context, compilationUnit, null, true);
  }

  Unifier(
      Context context,
      JCCompilationUnit compilationUnit,
      @Nullable ImmutableMap<String, UType> expectedArgTypes,
      boolean autoboxing) {
    this.bindings = Bindings.create();
    this.context = checkNotNull(context);
    this.types = Types.instance(context);
    this.treeMaker = TreeMaker.instance(context);
    this.compilationUnit = checkNotNull(compilationUnit);
    this.matchContext = null;
    this.currentPath = null;
    this.expectedArgTypes = expectedArgTypes;
    this.autoboxing = autoboxing;
  }

  Unifier(
      MatchContext matchContext,
      @Nullable TreePath currentPath,
      @Nullable ImmutableMap<String, UType> expectedArgTypes,
      boolean autoboxing) {
    this.bindings = Bindings.create();
    this.context = matchContext.context();
    this.types = matchContext.types();
    this.treeMaker = matchContext.treeMaker();
    this.compilationUnit = matchContext.compilationUnit();
    this.matchContext = matchContext;
    this.currentPath = currentPath;
    this.expectedArgTypes = expectedArgTypes;
    this.autoboxing = autoboxing;
  }

  public Unifier(Context context) {
    this(context, defaultCompilationUnit(context));
  }

  private Unifier(
      Context context,
      Bindings bindings,
      Types types,
      TreeMaker treeMaker,
      JCCompilationUnit compilationUnit,
      @Nullable MatchContext matchContext,
      @Nullable TreePath currentPath,
      @Nullable ImmutableMap<String, UType> expectedArgTypes,
      boolean autoboxing) {
    this.context = context;
    this.bindings = Bindings.create(bindings);
    this.types = types;
    this.treeMaker = treeMaker;
    this.compilationUnit = checkNotNull(compilationUnit);
    this.matchContext = matchContext;
    this.currentPath = currentPath;
    this.expectedArgTypes = expectedArgTypes;
    this.autoboxing = autoboxing;
  }

  private static JCCompilationUnit defaultCompilationUnit(Context context) {
    JCCompilationUnit compilationUnit = TreeMaker.instance(context).TopLevel(nil());
    compilationUnit.packge = Symtab.instance(context).unnamedModule.unnamedPackage;
    return compilationUnit;
  }

  /// Returns a `Unifier` containing all the bindings from this `Unifier`, but which can succeed or
  /// fail independently of this `Unifier`.
  public Unifier fork() {
    return new Unifier(
        context,
        bindings,
        types,
        treeMaker,
        compilationUnit,
        matchContext,
        currentPath,
        expectedArgTypes,
        autoboxing);
  }

  @Nullable VisitorState sharedVisitorState() {
    return matchContext != null ? matchContext.visitorState() : null;
  }

  @Nullable TreePath currentPath() {
    return currentPath;
  }

  @Nullable UType expectedArgType(String argName) {
    return expectedArgTypes != null ? expectedArgTypes.get(argName) : null;
  }

  boolean autoboxing() {
    return autoboxing;
  }

  @Nullable ClassSymbol resolveClassOrNull(String qualifiedClass) {
    if (matchContext != null) {
      return matchContext.resolveClassOrNull(qualifiedClass);
    }
    try {
      Symbol symbol = JavaCompiler.instance(context).resolveBinaryNameOrIdent(qualifiedClass);
      if (symbol != null
          && !symbol.equals(Symtab.instance(context).errSymbol)
          && symbol instanceof ClassSymbol classSymbol) {
        return classSymbol;
      }
    } catch (NullPointerException ignored) {
      return null;
    }
    return null;
  }

  public JCCompilationUnit compilationUnit() {
    return compilationUnit;
  }

  public Types types() {
    return types;
  }

  public JCExpression thisExpression(Type type) {
    return treeMaker.This(type);
  }

  public Inliner createInliner() {
    return new Inliner(context, bindings, compilationUnit);
  }

  /// Returns an [Inliner] for code that will replace `location`, which lets the inliner check which
  /// names are in scope there.
  Inliner createInliner(JCTree location) {
    return new Inliner(
        context, bindings, compilationUnit, TreePath.getPath(compilationUnit, location));
  }

  public <V> @Nullable V getBinding(Bindings.Key<V> key) {
    return bindings.getBinding(key);
  }

  @CanIgnoreReturnValue
  public <V> V putBinding(Bindings.Key<V> key, V value) {
    checkArgument(!bindings.containsKey(key), "Cannot bind %s more than once", key);
    return bindings.putBinding(key, value);
  }

  public <V> V replaceBinding(Bindings.Key<V> key, V value) {
    checkArgument(bindings.containsKey(key), "Binding for %s does not exist", key);
    return bindings.putBinding(key, value);
  }

  public void clearBinding(Bindings.Key<?> key) {
    bindings.clearBinding(key);
  }

  public Bindings getBindings() {
    return bindings;
  }

  public Context getContext() {
    return context;
  }

  @Override
  public String toString() {
    return "Unifier{" + bindings + "}";
  }

  public static <T, U extends Unifiable<? super T>> Function<Unifier, Choice<Unifier>> unifications(
      @Nullable U unifiable, @Nullable T target) {
    return (Unifier unifier) -> unifyNullable(unifier, unifiable, target);
  }

  public static <T, U extends Unifiable<? super T>> Choice<Unifier> unifyNullable(
      Unifier unifier, @Nullable U unifiable, @Nullable T target) {
    if (target == null && unifiable == null) {
      return Choice.of(unifier);
    } else if (target == null || unifiable == null) {
      return Choice.none();
    } else {
      return unifiable.unify(target, unifier);
    }
  }

  public static <T, U extends Unifiable<? super T>> Function<Unifier, Choice<Unifier>> unifications(
      @Nullable List<U> toUnify, @Nullable List<? extends T> targets) {
    return unifications(toUnify, targets, /* allowVarargs= */ false);
  }

  public static <T, U extends Unifiable<? super T>> Function<Unifier, Choice<Unifier>> unifications(
      @Nullable List<U> toUnify, @Nullable List<? extends T> targets, boolean allowVarargs) {
    return (Unifier unifier) -> unifyList(unifier, toUnify, targets, allowVarargs);
  }

  /// Returns all successful unification paths from the specified `Unifier` unifying the specified
  /// lists, disallowing varargs.
  public static <T, U extends Unifiable<? super T>> Choice<Unifier> unifyList(
      Unifier unifier, @Nullable List<U> toUnify, @Nullable List<? extends T> targets) {
    return unifyList(unifier, toUnify, targets, /* allowVarargs= */ false);
  }

  /// Returns all successful unification paths from the specified `Unifier` unifying the specified
  /// lists, allowing varargs if and only if `allowVarargs` is true.
  public static <T, U extends Unifiable<? super T>> Choice<Unifier> unifyList(
      Unifier unifier,
      @Nullable List<U> toUnify,
      @Nullable List<? extends T> targets,
      boolean allowVarargs) {
    if (toUnify == null && targets == null) {
      return Choice.of(unifier);
    } else if (toUnify == null
        || targets == null
        || (allowVarargs
            ? toUnify.size() - 1 > targets.size()
            : toUnify.size() != targets.size())) {
      return Choice.none();
    }
    Choice<Unifier> choice = Choice.of(unifier);
    int index;
    for (index = 0; index < toUnify.size(); index++) {
      U toUnifyNext = toUnify.get(index);
      if (allowVarargs && toUnifyNext instanceof URepeated repeated) {
        int startIndex = index;
        if (index + 1 != toUnify.size()) {
          return Choice.none();
        }
        return choice.mapIfPresent(u -> {
          List<JCExpression> expressions = new ArrayList<>();
          for (int j = startIndex; j < targets.size(); j++) {
            Optional<Unifier> forked =
                repeated.unify((JCTree) targets.get(j), u.fork()).findFirst();
            if (forked.isEmpty()) {
              return Optional.empty();
            }
            JCExpression boundExpr = repeated.getUnderlyingBinding(forked.get());
            if (boundExpr == null) {
              return Optional.empty();
            }
            expressions.add(boundExpr);
          }
          u.putBinding(repeated.key(), expressions);
          return Optional.of(u);
        });
      }
      if (index >= targets.size()) {
        return Choice.none();
      }
      choice = choice.flatMap(unifications(toUnifyNext, targets.get(index)));
    }
    if (index < targets.size()) {
      return Choice.none();
    }
    return choice;
  }
}
