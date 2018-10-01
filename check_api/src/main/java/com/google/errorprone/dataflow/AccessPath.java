/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.dataflow;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A sequence of field names or autovalue accessors, along with a receiver: either a variable or a
 * reference (explicit or implicit) to {@code this}. Fields and autovalue accessors are stored as
 * strings, with a "()" appended to accessor names to distinguish them from fields of the same name
 *
 * <p>For example:
 *
 * <p>{@code x.f.g}, the {@code g} field of the {@code f} field of the local variable {@code x} is
 * represented by {base = Some x, fields = "g" :: "f" :: nil}
 *
 * <p>{@code foo.bar}, the {@code bar} field of the {@code foo} field of the implicit {@code this}
 * is represented by {base = None, fields = "bar" :: "foo" :: nil}
 *
 * <p>{@code x.foo().foo}, the {@code foo} field of the {@code foo()} autovalue accessor of the
 * local variable {@code x} is represented by {base = Some x, fields = "foo" :: "foo()" :: nil}
 *
 * @author bennostein@google.com (Benno Stein)
 */
@AutoValue
public abstract class AccessPath {

  /** If present, base of access path is contained Element; if absent, base is `this` */
  public abstract @Nullable Element base();

  public abstract ImmutableList<String> path();

  private static AccessPath create(@Nullable Element base, ImmutableList<String> path) {
    return new AutoValue_AccessPath(base, path);
  }

  /**
   * Check whether {@code tree} is an AutoValue accessor. A tree is an AutoValue accessor iff:
   *
   * <ul>
   *   <li>it is a method invocation
   *   <li>of an abstract method
   *   <li>with 0 arguments
   *   <li>defined on a class annotated @AutoValue
   * </ul>
   *
   * <p>Public visibility for use in NullnessPropagationTransfer#returnValueNullness
   */
  public static boolean isAutoValueAccessor(Tree tree) {
    if (!(tree instanceof MethodInvocationTree)) {
      return false;
    }

    JCMethodInvocation invocationTree = (JCMethodInvocation) tree;
    // methodSelect is always either a field access (e.g. `obj.foo()`) or identifier (e.g. `foo()`)
    JCExpression methodSelect = invocationTree.getMethodSelect();
    Type rcvrType =
        (methodSelect instanceof JCFieldAccess)
            ? ((JCFieldAccess) methodSelect).selected.type
            : ((JCIdent) methodSelect).sym.owner.type;

    return invocationTree.getArguments().isEmpty() // 0 arguments
        && // abstract
        TreeUtils.elementFromUse(invocationTree).getModifiers().contains(Modifier.ABSTRACT)
        && // class, not interface
        rcvrType.tsym.getKind() == ElementKind.CLASS
        && // annotated @AutoValue
        MoreAnnotations.getDeclarationAndTypeAttributes(rcvrType.tsym)
            .map(Object::toString)
            .anyMatch("@com.google.auto.value.AutoValue"::equals);
  }

  /**
   * Creates an AccessPath from field reads / AutoValue accessor we can track and returns null
   * otherwise (for example, when the receiver of the field access contains an array access or
   * non-AutoValue method call.
   */
  @Nullable
  public static AccessPath fromFieldAccess(FieldAccessNode fieldAccess) {
    ImmutableList.Builder<String> pathBuilder = ImmutableList.builder();

    Tree tree = fieldAccess.getTree();
    boolean isFieldAccess;
    while ((isFieldAccess = TreeUtils.isFieldAccess(tree)) || isAutoValueAccessor(tree)) {
      if (isFieldAccess) {
        pathBuilder.add(TreeUtils.getFieldName(tree));
      } else {
        // Must be an AutoValue accessor, since the `while` condition held but the `if` didn't.
        // Unwrap the method select from the call
        tree = ((MethodInvocationTree) tree).getMethodSelect();
        pathBuilder.add(TreeUtils.getMethodName(tree) + "()");
      }

      if (tree.getKind() == Kind.IDENTIFIER) {
        // Implicit `this` receiver
        return AccessPath.create(/*base=*/ null, pathBuilder.build());
      }

      tree = ((MemberSelectTree) tree).getExpression();
    }

    // Explicit `this` receiver
    if (tree.getKind() == Kind.IDENTIFIER
        && ((IdentifierTree) tree).getName().contentEquals("this")) {
      return AccessPath.create(/*base=*/ null, pathBuilder.build());
    }

    // Local variable receiver
    if (tree.getKind() == Kind.IDENTIFIER) {
      return AccessPath.create(TreeUtils.elementFromTree(tree), pathBuilder.build());
    }

    return null;
  }

  public static AccessPath fromLocalVariable(LocalVariableNode node) {
    return AccessPath.create(node.getElement(), ImmutableList.of());
  }

  public static AccessPath fromVariableDecl(VariableDeclarationNode node) {
    return AccessPath.create(TreeUtils.elementFromDeclaration(node.getTree()), ImmutableList.of());
  }

  /**
   * Returns an AccessPath representing {@code node} if {@code node} is representable as an access
   * path and null otherwise
   */
  @Nullable
  public static AccessPath fromNodeIfTrackable(Node node) {
    if (node instanceof LocalVariableNode) {
      return fromLocalVariable((LocalVariableNode) node);
    } else if (node instanceof VariableDeclarationNode) {
      return fromVariableDecl((VariableDeclarationNode) node);
    } else if (node instanceof FieldAccessNode) {
      return fromFieldAccess((FieldAccessNode) node);
    } else if (node instanceof AssignmentNode) {
      return fromNodeIfTrackable(((AssignmentNode) node).getTarget());
    }
    return null;
  }
}
