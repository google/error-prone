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

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import java.lang.reflect.Proxy;
import javax.annotation.Nullable;

/**
 * {@link UTree} representation of a {@link InstanceOfTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UInstanceOf extends UExpression {
  public static UInstanceOf create(UExpression expression, UTree<?> type) {
    return new AutoValue_UInstanceOf(expression, type);
  }

  public abstract UExpression getExpression();

  public abstract UTree<?> getType();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    InstanceOfTree proxy =
        (InstanceOfTree)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {InstanceOfTree.class},
                (unused, method, args) -> {
                  switch (method.getName()) {
                    case "getPattern":
                      // TODO(cushon): support refaster template matching on instanceof patterns
                      return null;
                    case "getExpression":
                      return getExpression();
                    case "getType":
                      return getType();
                    default:
                      try {
                        return method.invoke(UInstanceOf.this, args);
                      } catch (IllegalArgumentException e) {
                        throw new LinkageError(method.getName(), e);
                      }
                  }
                });
    return visitor.visitInstanceOf(proxy, data);
  }

  @Override
  public Kind getKind() {
    return Kind.INSTANCE_OF;
  }

  @Override
  public JCInstanceOf inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().TypeTest(getExpression().inline(inliner), getType().inline(inliner));
  }

  @Override
  @Nullable
  public Choice<Unifier> visitInstanceOf(InstanceOfTree instanceOf, @Nullable Unifier unifier) {
    return getExpression()
        .unify(instanceOf.getExpression(), unifier)
        .thenChoose(unifications(getType(), instanceOf.getType()));
  }
}
