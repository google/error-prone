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

package com.google.errorprone.bugpatterns;

import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.Enums;
import com.google.errorprone.CompilationTestHelper;
import javax.lang.model.element.Modifier;
import org.junit.Test;

/** Tests for {@link ValueClassEquals}. */
public class ValueClassEqualsTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ValueClassEquals.class, getClass());

  @Test
  public void positive() {
    assume().that(Enums.getIfPresent(Modifier.class, "VALUE").isPresent()).isTrue();
    compilationHelper
        .setArgs(
            "--enable-preview",
            "--source",
            String.valueOf(Runtime.version().feature()),
            "-Xlint:-preview")
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: ValueClassEquals
              static value class MissingBoth {
                private final String s;

                MissingBoth(String s) {
                  this.s = s;
                }
              }

              // BUG: Diagnostic contains: ValueClassEquals
              static value class MissingHashCode {
                private final int x;

                MissingHashCode(int x) {
                  this.x = x;
                }

                @Override
                public boolean equals(Object obj) {
                  return super.equals(obj);
                }
              }

              // BUG: Diagnostic contains: ValueClassEquals
              static value class MissingEquals {
                private final int x;

                MissingEquals(int x) {
                  this.x = x;
                }

                @Override
                public int hashCode() {
                  return super.hashCode();
                }
              }

              interface EqualsInterface {
                @Override
                boolean equals(Object obj);

                @Override
                int hashCode();
              }

              // BUG: Diagnostic contains: ValueClassEquals
              static value class ImplementsInterfaceWithoutOverriding implements EqualsInterface {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    assume().that(Enums.getIfPresent(Modifier.class, "VALUE").isPresent()).isTrue();
    compilationHelper
        .setArgs(
            "--enable-preview",
            "--source",
            String.valueOf(Runtime.version().feature()),
            "-Xlint:-preview")
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              static value class ExplicitEqualsAndHashCode {
                private final String s;

                ExplicitEqualsAndHashCode(String s) {
                  this.s = s;
                }

                @Override
                public boolean equals(Object obj) {
                  return obj instanceof ExplicitEqualsAndHashCode other
                      && Objects.equals(s, other.s);
                }

                @Override
                public int hashCode() {
                  return Objects.hashCode(s);
                }
              }

              static value class ExplicitSuperDelegation {
                private final int x;

                ExplicitSuperDelegation(int x) {
                  this.x = x;
                }

                @Override
                public boolean equals(Object obj) {
                  return super.equals(obj);
                }

                @Override
                public int hashCode() {
                  return super.hashCode();
                }
              }

              static abstract value class AbstractValueWithoutEquals {
                private final int x;

                AbstractValueWithoutEquals(int x) {
                  this.x = x;
                }
              }

              static abstract value class AbstractValueWithEquals {
                @Override
                public final boolean equals(Object obj) {
                  return super.equals(obj);
                }

                @Override
                public final int hashCode() {
                  return super.hashCode();
                }
              }

              static value class ConcreteInheritingEquals extends AbstractValueWithEquals {}

              static final class IdentityClassWithoutEquals {}
            }
            """)
        .doTest();
  }
}
