/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.util.List;
import javax.lang.model.element.Name;

/**
 * {@code UTree} representation of a {@code ClassTree} for anonymous inner class matching.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UClassDecl extends USimpleStatement implements ClassTree {
  public static UClassDecl create(UMethodDecl... members) {
    return create(ImmutableList.copyOf(members));
  }

  public static UClassDecl create(Iterable<UMethodDecl> members) {
    return new AutoValue_UClassDecl(ImmutableList.copyOf(members));
  }

  @AutoValue
  abstract static class UnifierWithRemainingMembers {
    static UnifierWithRemainingMembers create(
        Unifier unifier, Iterable<UMethodDecl> remainingMembers) {
      return new AutoValue_UClassDecl_UnifierWithRemainingMembers(
          unifier, ImmutableList.copyOf(remainingMembers));
    }

    abstract Unifier unifier();

    abstract ImmutableList<UMethodDecl> remainingMembers();

    static final Function<Unifier, UnifierWithRemainingMembers> withRemaining(
        final Iterable<UMethodDecl> remainingMembers) {
      return (Unifier unifier) -> create(unifier, remainingMembers);
    }
  }

  private static Function<UnifierWithRemainingMembers, Choice<UnifierWithRemainingMembers>> match(
      final Tree tree) {
    return (final UnifierWithRemainingMembers state) -> {
      final ImmutableList<UMethodDecl> currentMembers = state.remainingMembers();
      Choice<Integer> methodChoice =
          Choice.from(
              ContiguousSet.create(
                  Range.closedOpen(0, currentMembers.size()), DiscreteDomain.integers()));
      return methodChoice.thenChoose(
          (Integer i) -> {
            ImmutableList<UMethodDecl> remainingMembers =
                ImmutableList.<UMethodDecl>builder()
                    .addAll(currentMembers.subList(0, i))
                    .addAll(currentMembers.subList(i + 1, currentMembers.size()))
                    .build();
            UMethodDecl chosenMethod = currentMembers.get(i);
            Unifier unifier = state.unifier().fork();
            /*
             * If multiple methods use the same parameter name, preserve the last parameter
             * name from the target code.  For example, given a @BeforeTemplate with
             *
             *    int get(int index) {...}
             *    int set(int index, int value) {...}
             *
             * and target code with the lines
             *
             *    int get(int i) {...}
             *    int set(int j) {...}
             *
             * then use "j" in place of index in the @AfterTemplates.
             */
            for (UVariableDecl param : chosenMethod.getParameters()) {
              unifier.clearBinding(param.key());
            }
            return chosenMethod
                .unify(tree, unifier)
                .transform(UnifierWithRemainingMembers.withRemaining(remainingMembers));
          });
    };
  }

  @Override
  public Choice<Unifier> visitClass(ClassTree node, Unifier unifier) {
    Choice<UnifierWithRemainingMembers> path =
        Choice.of(UnifierWithRemainingMembers.create(unifier, getMembers()));
    for (Tree targetMember : node.getMembers()) {
      if (!(targetMember instanceof MethodTree)
          || ((MethodTree) targetMember).getReturnType() != null) {
        // skip synthetic constructors
        path = path.thenChoose(match(targetMember));
      }
    }
    return path.condition(s -> s.remainingMembers().isEmpty())
        .transform(UnifierWithRemainingMembers::unifier);
  }

  @Override
  public JCClassDecl inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .AnonymousClassDef(
            inliner.maker().Modifiers(0L),
            List.convert(JCTree.class, inliner.inlineList(getMembers())));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitClass(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.CLASS;
  }

  @Override
  public UTree<?> getExtendsClause() {
    return null;
  }

  @Override
  public ImmutableList<UTree<?>> getImplementsClause() {
    return ImmutableList.of();
  }

  @Override
  public abstract ImmutableList<UMethodDecl> getMembers();

  @Override
  public ModifiersTree getModifiers() {
    return null;
  }

  @Override
  public Name getSimpleName() {
    return null;
  }

  @Override
  public ImmutableList<TypeParameterTree> getTypeParameters() {
    return ImmutableList.of();
  }
}
