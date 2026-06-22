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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;

/** Tests for {@link UnnecessaryInheritedNestedTypeQualifier}. */
public final class UnnecessaryInheritedNestedTypeQualifierTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          UnnecessaryInheritedNestedTypeQualifier.class, getClass());

  @Test
  public void positiveCaseSuperclassQualifier() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Super.Nested field;
            }
            """)
        .addOutputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Nested field;
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCaseSubclassQualifier() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Sub.Nested field;
            }
            """)
        .addOutputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Nested field;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseShadowed() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub extends Super {
              static class Nested {}

              Super.Nested field1;
              Nested field2;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeCaseNotSubclass() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Other.java",
            """
            class Other {
              Super.Nested field;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void multiLevelInheritance() {
    refactoringHelper
        .addInputLines(
            "Grandparent.java",
            """
            class Grandparent {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Parent.java",
            """
            class Parent extends Grandparent {}
            """)
        .expectUnchanged()
        .addInputLines(
            "Child.java",
            """
            class Child extends Parent {
              Grandparent.Nested f1;
              Parent.Nested f2;
              Child.Nested f3;
            }
            """)
        .addOutputLines(
            "Child.java",
            """
            class Child extends Parent {
              Nested f1;
              Nested f2;
              Nested f3;
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCaseAnonymousClass() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static class Nested {}
              void doSomething() {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              Super s = new Super() {
                Super.Nested field;
              };
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              Super s = new Super() {
                Nested field;
              };
            }
            """)
        .doTest();
  }

  @Test
  public void differentPackagePublicNested() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            public class Super {
              public static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Sub.java",
            """
            package b;

            import a.Super;

            class Sub extends Super {
              Super.Nested field;
            }
            """)
        .addOutputLines(
            "b/Sub.java",
            """
            package b;

            import a.Super;

            class Sub extends Super {
              Nested field;
            }
            """)
        .doTest();
  }

  @Test
  public void differentPackageProtectedNested() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            public class Super {
              protected static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Sub.java",
            """
            package b;

            import a.Super;

            class Sub extends Super {
              Super.Nested field;
            }
            """)
        .addOutputLines(
            "b/Sub.java",
            """
            package b;

            import a.Super;

            class Sub extends Super {
              Nested field;
            }
            """)
        .doTest();
  }

  @Test
  public void testParameterInjector_testParameterValuesProvider_context() {
    refactoringHelper
        .addInputLines(
            "MyProvider.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider;
            import java.util.List;

            class MyProvider extends TestParameterValuesProvider {
              @Override
              protected List<?> provideValues(TestParameterValuesProvider.Context context) {
                return null;
              }
            }
            """)
        .addOutputLines(
            "MyProvider.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider;
            import java.util.List;

            class MyProvider extends TestParameterValuesProvider {
              @Override
              protected List<?> provideValues(Context context) {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truth_subject_factory() {
    refactoringHelper
        .addInputLines(
            "MySubject.java",
            """
            import com.google.common.truth.FailureMetadata;
            import com.google.common.truth.Subject;

            class MySubject extends Subject {
              protected MySubject(FailureMetadata metadata, Object actual) {
                super(metadata, actual);
              }

              static Subject.Factory<MySubject, Object> factory() {
                return MySubject::new;
              }
            }
            """)
        .addOutputLines(
            "MySubject.java",
            """
            import com.google.common.truth.FailureMetadata;
            import com.google.common.truth.Subject;

            class MySubject extends Subject {
              protected MySubject(FailureMetadata metadata, Object actual) {
                super(metadata, actual);
              }

              static Factory<MySubject, Object> factory() {
                return MySubject::new;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedClassOfSubclass_unchanged() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub extends Super {
              class InnerSub {
                Super.Nested field;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nestedInterface() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              interface Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Super.Nested field;
            }
            """)
        .addOutputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Nested field;
            }
            """)
        .doTest();
  }

  @Test
  public void nestedEnum() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            class Super {
              enum Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Super.Nested field;
            }
            """)
        .addOutputLines(
            "Sub.java",
            """
            class Sub extends Super {
              Nested field;
            }
            """)
        .doTest();
  }

  @Test
  public void interfaceSupertype() {
    refactoringHelper
        .addInputLines(
            "SuperInterface.java",
            """
            interface SuperInterface {
              static class Nested {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Sub.java",
            """
            class Sub implements SuperInterface {
              SuperInterface.Nested field;
            }
            """)
        .addOutputLines(
            "Sub.java",
            """
            class Sub implements SuperInterface {
              Nested field;
            }
            """)
        .doTest();
  }
}
