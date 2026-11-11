/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.predicates;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import org.junit.Test;

/** {@link TypePredicates}Test */
public class TypePredicatesTest {

  /** Flags variables whose type is a descendant of any of a few types. */
  @BugPattern(summary = "Descendant", severity = ERROR)
  public static class DescendantOfAnyChecker extends BugChecker implements VariableTreeMatcher {
    private static final TypePredicate PREDICATE =
        TypePredicates.isDescendantOfAny(
            ImmutableList.of(
                "test.DoesNotExist",
                "test.I",
                "java.lang.Cloneable",
                "test.Generic",
                "java.lang.Runnable"));

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return PREDICATE.apply(getType(tree), state) ? describeMatch(tree) : NO_MATCH;
    }
  }

  /** Flags variables whose type is a descendant of Object. */
  @BugPattern(summary = "Descendant of Object", severity = ERROR)
  public static class DescendantOfObjectChecker extends BugChecker implements VariableTreeMatcher {
    private static final TypePredicate PREDICATE =
        TypePredicates.isDescendantOfAny(ImmutableList.of("test.DoesNotExist", "java.lang.Object"));

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return PREDICATE.apply(getType(tree), state) ? describeMatch(tree) : NO_MATCH;
    }
  }

  /**
   * Exercises {@link TypePredicates#isDescendantOfAny(ImmutableList)} on intersection, union, null,
   * method, generic-method, and package types.
   */
  @BugPattern(summary = "Compound and non-class types", severity = ERROR)
  public static class CompoundAndNonClassChecker extends BugChecker
      implements MethodTreeMatcher, CatchTreeMatcher {
    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      var methodSym = getSymbol(tree);
      if (methodSym.getTypeParameters().isEmpty()) {
        return NO_MATCH;
      }
      // Upper bound of <X extends I & Runnable> is an IntersectionClassType (hasTag(CLASS) &&
      // isCompound()). ASTHelpers.isSubtype erases I & Runnable to its first bound I.
      Type intersection = methodSym.getTypeParameters().getFirst().type.getUpperBound();
      assertThat(intersection.isCompound()).isTrue();
      TypePredicate pred =
          TypePredicates.isDescendantOfAny(ImmutableList.<Supplier<Type>>of(s -> intersection));

      for (VariableTree param : tree.getParameters()) {
        Type paramType = getType(param);
        assertThat(pred.apply(paramType, state))
            .isEqualTo(ASTHelpers.isSubtype(paramType, intersection, state));
      }

      // Null, METHOD/FORALL, and PACKAGE types must return false without throwing.
      assertThat(pred.apply(null, state)).isFalse();
      assertThat(pred.apply(methodSym.type, state)).isFalse();
      assertThat(pred.apply(getType(state.getPath().getCompilationUnit().getPackageName()), state))
          .isFalse();

      // The null type is a subtype of every class type, despite not having a supertype closure.
      Type runnable = state.getTypeFromString("java.lang.Runnable");
      TypePredicate isRunnable =
          TypePredicates.isDescendantOfAny(ImmutableList.<Supplier<Type>>of(s -> runnable));
      assertThat(isRunnable.apply(state.getSymtab().botType, state)).isTrue();
      assertThat(isRunnable.apply(state.getSymtab().intType, state)).isFalse();
      return describeMatch(tree);
    }

    @Override
    public Description matchCatch(CatchTree tree, VisitorState state) {
      // Multi-catch parameter type is a UnionClassType (hasTag(CLASS) && isUnion()).
      Type union = getType(tree.getParameter());
      assertThat(union.isUnion()).isTrue();
      TypePredicate pred =
          TypePredicates.isDescendantOfAny(ImmutableList.<Supplier<Type>>of(s -> union));
      Type firstAlt = getType(tree.getParameter().getType());
      assertThat(pred.apply(firstAlt, state))
          .isEqualTo(ASTHelpers.isSubtype(firstAlt, union, state));
      return describeMatch(tree);
    }
  }

  @Test
  public void isDescendantOfAny() {
    CompilationTestHelper.newInstance(DescendantOfAnyChecker.class, getClass())
        .addSourceLines(
            "I.java",
            """
            package test;

            interface I {}
            """)
        .addSourceLines(
            "Generic.java",
            """
            package test;

            class Generic<T> {}
            """)
        .addSourceLines(
            "Test.java",
            """
            package test;

            import java.util.List;

            class Test {
              static class A implements I {}

              static class B extends A {}

              static class C extends Generic<String> {}

              interface J extends I {}

              <T extends B, U extends Object & Runnable> void f(
                  // BUG: Diagnostic contains:
                  A a,
                  // BUG: Diagnostic contains:
                  B b,
                  // BUG: Diagnostic contains:
                  C c,
                  // BUG: Diagnostic contains:
                  J j,
                  // BUG: Diagnostic contains:
                  T t,
                  // The erasure of U is Object.
                  U u,
                  // BUG: Diagnostic contains:
                  String[] array,
                  // BUG: Diagnostic contains:
                  Generic<Integer> generic,
                  int primitive,
                  List<String> list,
                  Object object) {}
            }
            """)
        .doTest();
  }

  @Test
  public void isDescendantOfAny_object() {
    CompilationTestHelper.newInstance(DescendantOfObjectChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            package test;

            class Test {
              void f(
                  // BUG: Diagnostic contains:
                  Runnable r,
                  // BUG: Diagnostic contains:
                  String[] array,
                  // BUG: Diagnostic contains:
                  Object object,
                  int primitive) {}
            }
            """)
        .doTest();
  }

  @Test
  public void isDescendantOfAny_compoundUnionAndNonClassTypes() {
    CompilationTestHelper.newInstance(CompoundAndNonClassChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            package test;

            class Test {
              interface I {}

              static class A implements I {}

              static class BaseEx extends Exception {}

              static class SubEx1 extends BaseEx {}

              static class SubEx2 extends BaseEx {}

              // BUG: Diagnostic contains:
              <X extends I & Runnable> void genericMethod(A matchesFirstBound, Runnable onlySecondBound) {
                try {
                  if (matchesFirstBound.hashCode() > 0) {
                    throw new SubEx1();
                  }
                  throw new SubEx2();
                  // BUG: Diagnostic contains:
                } catch (SubEx1 | SubEx2 e) {
                }
              }

              void plainMethod() {}
            }
            """)
        .doTest();
  }
}
