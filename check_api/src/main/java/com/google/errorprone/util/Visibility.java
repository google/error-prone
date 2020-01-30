/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * Describes visibilities available via VisibleForTesting annotations, and provides methods to
 * establish whether a given {@link Tree} should be visible.
 */
public enum Visibility implements Comparable<Visibility> {
  // In order of ascending visibility.
  NONE {
    @Override
    public boolean shouldBeVisible(Tree tree, VisitorState state) {
      return false;
    }

    @Override
    public boolean shouldBeVisible(Symbol symbol, VisitorState state) {
      return false;
    }

    @Override
    public String description() {
      return "not be used";
    }
  },
  PRIVATE {
    @Override
    public boolean shouldBeVisible(Tree tree, VisitorState state) {
      return shouldBeVisible(getSymbol(tree), state);
    }

    @Override
    public boolean shouldBeVisible(Symbol symbol, VisitorState state) {
      Symbol outermostClass = null;
      for (Tree parent : state.getPath()) {
        if (parent instanceof ClassTree) {
          outermostClass = getSymbol(parent);
        }
      }
      return outermostClass == null || ASTHelpers.outermostClass(symbol).equals(outermostClass);
    }

    @Override
    public String description() {
      return "private visibility";
    }
  },
  PACKAGE_PRIVATE {
    @Override
    public boolean shouldBeVisible(Tree tree, VisitorState state) {
      return PACKAGE_PRIVATE.shouldBeVisible(getSymbol(tree), state);
    }

    @Override
    public boolean shouldBeVisible(Symbol symbol, VisitorState state) {
      JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
      PackageSymbol packge = compilationUnit.packge;
      // TODO(ghm): Should we handle the default (unnamed) package here?
      return symbol.packge().equals(packge);
    }

    @Override
    public String description() {
      return "default (package private) visibility";
    }
  },
  PROTECTED {
    // Rules https://docs.oracle.com/javase/specs/jls/se7/html/jls-6.html#jls-6.6.2
    @Override
    public boolean shouldBeVisible(Tree tree, VisitorState state) {
      if (PACKAGE_PRIVATE.shouldBeVisible(tree, state)) {
        return true;
      }
      Symbol symbol = getSymbol(tree);
      ClassTree classTree = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
      if (symbol.isStatic()) {
        if (classTree == null) {
          return false;
        }
        return hasEnclosingClassExtending(enclosingClass(symbol).type, state, classTree);
      }
      // Anonymous subclasses will match on the synthetic constructor.
      if (tree instanceof NewClassTree) {
        return false;
      }
      if (!(tree instanceof ExpressionTree)) {
        return hasEnclosingClassExtending(enclosingClass(symbol).type, state, classTree);
      }
      if (tree instanceof MemberSelectTree
          && ((MemberSelectTree) tree).getIdentifier().contentEquals("super")) {
        // Allow qualified super calls.
        return true;
      }
      if (tree instanceof MemberSelectTree || tree instanceof MemberReferenceTree) {
        ExpressionTree receiver = ASTHelpers.getReceiver((ExpressionTree) tree);
        return receiver.toString().equals("super")
            || hasEnclosingClassOfSuperType(getType(receiver), state, classTree);
      }
      // If there's no receiver, we must be accessing it via an implicit "this", or we're
      // using unqualified "super".
      return true;
    }

    @Override
    public boolean shouldBeVisible(Symbol symbol, VisitorState state) {
      if (PACKAGE_PRIVATE.shouldBeVisible(symbol, state)) {
        return true;
      }
      ClassTree classTree = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
      return classTree != null
          && hasEnclosingClassExtending(enclosingClass(symbol).type, state, classTree);
    }

    private boolean hasEnclosingClassOfSuperType(
        Type type, VisitorState state, ClassTree classTree) {
      for (ClassSymbol encl = getSymbol(classTree); encl != null; encl = enclosingClass(encl)) {
        if (encl.isStatic()) {
          break;
        }
        if (ASTHelpers.isSubtype(type, encl.type, state)) {
          return true;
        }
      }
      return false;
    }

    private boolean hasEnclosingClassExtending(Type type, VisitorState state, ClassTree classTree) {
      for (ClassSymbol encl = getSymbol(classTree); encl != null; encl = enclosingClass(encl)) {
        if (encl.isStatic()) {
          break;
        }
        if (ASTHelpers.isSubtype(encl.type, type, state)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String description() {
      return "protected visibility";
    }
  },
  PUBLIC {
    @Override
    public boolean shouldBeVisible(Tree tree, VisitorState state) {
      return true;
    }

    @Override
    public boolean shouldBeVisible(Symbol symbol, VisitorState state) {
      return true;
    }

    @Override
    public String description() {
      return "public visibility";
    }
  };

  /**
   * Whether {@code tree} should be visible from the path in {@code state} assuming we're in prod
   * code.
   */
  public abstract boolean shouldBeVisible(Tree tree, VisitorState state);

  /**
   * Whether {@code symbol} should be visible from the path in {@code state} assuming we're in prod
   * code.
   */
  public abstract boolean shouldBeVisible(Symbol symbol, VisitorState state);

  public static Visibility fromModifiers(Set<Modifier> modifiers) {
    if (modifiers.contains(Modifier.PRIVATE)) {
      return Visibility.PRIVATE;
    }
    if (modifiers.contains(Modifier.PUBLIC)) {
      return Visibility.PUBLIC;
    }
    if (modifiers.contains(Modifier.PROTECTED)) {
      return Visibility.PROTECTED;
    }
    return Visibility.PACKAGE_PRIVATE;
  }


  public boolean isAtLeastAsRestrictiveAs(Visibility visibility) {
    return compareTo(visibility) <= 0;
  }

  public boolean isMoreVisibleThan(Visibility visibility) {
    return compareTo(visibility) > 0;
  }

  /**
   * A fragment describing this visibility, to fit in a phrase like "restricted to [...]".
   *
   * <p>This is complicated by {@code NONE}, which doesn't describe a Java visibility.
   */
  public abstract String description();
}
