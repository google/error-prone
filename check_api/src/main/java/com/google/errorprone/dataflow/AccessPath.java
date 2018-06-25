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
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A sequence of field names along with either a variable or a reference (explicit or implicit) to
 * {@code this}
 *
 * <p>For example:
 *
 * <p>{@code x.f.g}, the {@code g} field of the {@code f} field of the local variable {@code x} is
 * represented by {base = Some x, fields = g :: f :: nil}
 *
 * <p>{@code foo.bar}, the {@code bar} field of the {@code foo} field of the implicit {@code this}
 * is represented by {base = None, fields = bar :: foo :: nil}
 *
 * @author bennostein@google.com (Benno Stein)
 */
@AutoValue
public abstract class AccessPath {

  /** If present, base of access path is contained Element; if absent, base is `this` */
  public abstract @Nullable Element base();
  // TODO(b/110226434): Support getters (or arbitrary nullary pure methods) as well as fields?
  public abstract ImmutableList<String> fields();

  private static AccessPath create(@Nullable Element base, ImmutableList<String> fields) {
    return new AutoValue_AccessPath(base, fields);
  }

  /**
   * Creates an AccessPath from field accesses we can track and returns null otherwise (for example,
   * when the receiver of the field access contains a method call or array access)
   */
  @Nullable
  public static AccessPath fromFieldAccess(FieldAccessNode fieldAccess) {
    ImmutableList.Builder<String> pathBuilder = ImmutableList.builder();

    Tree tree = fieldAccess.getTree();
    while (TreeUtils.isFieldAccess(tree)) {
      pathBuilder.add(TreeUtils.getFieldName(tree));
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
    }
    return null;
  }
}
