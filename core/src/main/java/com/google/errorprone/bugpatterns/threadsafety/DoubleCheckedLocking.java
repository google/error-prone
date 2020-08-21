/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
// TODO(cushon): allow @LazyInit on fields as a suppression mechanism?
@BugPattern(
    name = "DoubleCheckedLocking",
    summary = "Double-checked locking on non-volatile fields is unsafe",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class DoubleCheckedLocking extends BugChecker implements IfTreeMatcher {
  @Override
  public Description matchIf(IfTree outerIf, VisitorState state) {
    DCLInfo info = findDCL(outerIf);
    if (info == null) {
      return Description.NO_MATCH;
    }
    switch (info.sym().getKind()) {
      case FIELD:
        return handleField(info.outerIf(), info.sym(), state);
      case LOCAL_VARIABLE:
        return handleLocal(info, state);
      default:
        return Description.NO_MATCH;
    }
  }

  /**
   * Report a {@link Description} if a field used in double-checked locking is not volatile.
   *
   * <p>If the AST node for the field declaration can be located in the current compilation unit,
   * suggest adding the volatile modifier.
   */
  private Description handleField(IfTree outerIf, VarSymbol sym, VisitorState state) {
    if (sym.getModifiers().contains(Modifier.VOLATILE)) {
      return Description.NO_MATCH;
    }
    if (isImmutable(sym.type, state)) {
      return Description.NO_MATCH;
    }
    Description.Builder builder = buildDescription(outerIf);
    JCTree fieldDecl = findFieldDeclaration(state.getPath(), sym);
    if (fieldDecl != null) {
      builder.addFix(SuggestedFixes.addModifiers(fieldDecl, state, Modifier.VOLATILE));
    }
    return builder.build();
  }

  private static final ImmutableSet<String> IMMUTABLE_PRIMITIVES =
      ImmutableSet.of(
          java.lang.Boolean.class.getName(),
          java.lang.Byte.class.getName(),
          java.lang.Short.class.getName(),
          java.lang.Integer.class.getName(),
          java.lang.Character.class.getName(),
          java.lang.Float.class.getName(),
          java.lang.String.class.getName());

  /**
   * Recognize a small set of known-immutable types that are safe for DCL even without a volatile
   * field.
   */
  private static boolean isImmutable(Type type, VisitorState state) {
    switch (type.getKind()) {
      case BOOLEAN:
      case BYTE:
      case SHORT:
      case INT:
      case CHAR:
      case FLOAT:
        return true;
      case LONG:
      case DOUBLE:
        // double-width primitives aren't written atomically
        return true;
      default:
        break;
    }
    return IMMUTABLE_PRIMITIVES.contains(
        state.getTypes().erasure(type).tsym.getQualifiedName().toString());
  }

  /**
   * Report a diagnostic for an instance of DCL on a local variable. A match is only reported if a
   * non-volatile field is written to the variable after acquiring the lock and before the second
   * null-check on the local.
   *
   * <p>e.g.
   *
   * <pre>{@code
   * if ($X == null) {
   *   synchronized (...) {
   *     $X = myNonVolatileField;
   *     if ($X == null) {
   *       ...
   *     }
   *     ...
   *   }
   * }
   * }</pre>
   */
  private Description handleLocal(DCLInfo info, VisitorState state) {
    JCExpressionStatement expr = getChild(info.synchTree().getBlock(), JCExpressionStatement.class);
    if (expr == null) {
      return Description.NO_MATCH;
    }
    if (expr.getStartPosition() > getStartPosition(info.innerIf())) {
      return Description.NO_MATCH;
    }
    if (!(expr.getExpression() instanceof JCAssign)) {
      return Description.NO_MATCH;
    }
    JCAssign assign = (JCAssign) expr.getExpression();
    if (!Objects.equals(ASTHelpers.getSymbol(assign.getVariable()), info.sym())) {
      return Description.NO_MATCH;
    }
    Symbol sym = ASTHelpers.getSymbol(assign.getExpression());
    if (!(sym instanceof VarSymbol)) {
      return Description.NO_MATCH;
    }
    VarSymbol fvar = (VarSymbol) sym;
    if (fvar.getKind() != ElementKind.FIELD) {
      return Description.NO_MATCH;
    }
    return handleField(info.outerIf(), fvar, state);
  }

  /** Information about an instance of DCL. */
  @AutoValue
  abstract static class DCLInfo {
    /** The outer if statement */
    abstract IfTree outerIf();

    /** The synchronized statement */
    abstract SynchronizedTree synchTree();

    /** The inner if statement * */
    abstract IfTree innerIf();

    /** The variable (local or field) that is double-checked */
    abstract VarSymbol sym();

    static DCLInfo create(
        IfTree outerIf, SynchronizedTree synchTree, IfTree innerIf, VarSymbol sym) {
      return new AutoValue_DoubleCheckedLocking_DCLInfo(outerIf, synchTree, innerIf, sym);
    }
  }

  /**
   * Matches an instance of DCL. The canonical pattern is:
   *
   * <pre>{@code
   * if ($X == null) {
   *   synchronized (...) {
   *     if ($X == null) {
   *       ...
   *     }
   *     ...
   *   }
   * }
   * }</pre>
   *
   * Gaps before the synchronized or inner 'if' statement are ignored, and the operands in the
   * null-checks are accepted in either order.
   */
  @Nullable
  static DCLInfo findDCL(IfTree outerIf) {
    // TODO(cushon): Optional.ifPresent...
    ExpressionTree outerIfTest = getNullCheckedExpression(outerIf.getCondition());
    if (outerIfTest == null) {
      return null;
    }
    SynchronizedTree synchTree = getChild(outerIf.getThenStatement(), SynchronizedTree.class);
    if (synchTree == null) {
      return null;
    }
    IfTree innerIf = getChild(synchTree.getBlock(), IfTree.class);
    if (innerIf == null) {
      return null;
    }
    ExpressionTree innerIfTest = getNullCheckedExpression(innerIf.getCondition());
    if (innerIfTest == null) {
      return null;
    }
    Symbol outerSym = ASTHelpers.getSymbol(outerIfTest);
    if (!Objects.equals(outerSym, ASTHelpers.getSymbol(innerIfTest))) {
      return null;
    }
    if (!(outerSym instanceof VarSymbol)) {
      return null;
    }
    VarSymbol var = (VarSymbol) outerSym;
    return DCLInfo.create(outerIf, synchTree, innerIf, var);
  }

  /**
   * Matches comparisons to null (e.g. {@code foo == null}) and returns the expression being tested.
   */
  private static ExpressionTree getNullCheckedExpression(ExpressionTree condition) {
    condition = stripParentheses(condition);
    if (!(condition instanceof BinaryTree)) {
      return null;
    }
    BinaryTree bin = (BinaryTree) condition;
    ExpressionTree other;
    if (bin.getLeftOperand().getKind() == Kind.NULL_LITERAL) {
      other = bin.getRightOperand();
    } else if (bin.getRightOperand().getKind() == Kind.NULL_LITERAL) {
      other = bin.getLeftOperand();
    } else {
      return null;
    }
    return other;
  }

  /**
   * Visits (possibly nested) block statements and returns the first child statement with the given
   * class.
   */
  private static <T> T getChild(StatementTree tree, final Class<T> clazz) {
    return tree.accept(
        new SimpleTreeVisitor<T, Void>() {
          @Override
          protected T defaultAction(Tree node, Void p) {
            if (clazz.isInstance(node)) {
              return clazz.cast(node);
            }
            return null;
          }

          @Override
          public T visitBlock(BlockTree node, Void p) {
            return visit(node.getStatements());
          }

          private T visit(List<? extends Tree> tx) {
            for (Tree t : tx) {
              T r = t.accept(this, null);
              if (r != null) {
                return r;
              }
            }
            return null;
          }
        },
        null);
  }

  /**
   * Performs a best-effort search for the AST node of a field declaration.
   *
   * <p>It will only find fields declared in a lexically enclosing scope of the current location.
   * Since double-checked locking should always be used on a private field, this should be
   * reasonably effective.
   */
  @Nullable
  private static JCTree findFieldDeclaration(TreePath path, VarSymbol var) {
    for (TreePath curr = path; curr != null; curr = curr.getParentPath()) {
      Tree leaf = curr.getLeaf();
      if (!(leaf instanceof JCClassDecl)) {
        continue;
      }
      for (JCTree tree : ((JCClassDecl) leaf).getMembers()) {
        if (Objects.equals(var, ASTHelpers.getSymbol(tree))) {
          return tree;
        }
      }
    }
    return null;
  }
}
