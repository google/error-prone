/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link SelfEquals} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class SelfEqualsTest {
  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(SelfEquals.class, getClass());
  }

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "SelfEqualsPositiveCase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            import org.junit.Assert;

            /**
             * Positive test cases for {@link SelfEquals} check.
             *
             * @author eaftan@google.com (Eddie Aftandilian)
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfEqualsPositiveCase {
              protected String simpleField;

              public boolean test1(Object obj) {
                if (obj == null || getClass() != obj.getClass()) {
                  return false;
                }
                SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
                // BUG: Diagnostic contains: simpleField.equals(other.simpleField);
                return simpleField.equals(simpleField);
              }

              public boolean test2(SelfEqualsPositiveCase obj) {
                if (obj == null || getClass() != obj.getClass()) {
                  return false;
                }
                SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
                // BUG: Diagnostic contains: simpleField.equals(other.simpleField);
                return simpleField.equals(this.simpleField);
              }

              public boolean test3(SelfEqualsPositiveCase obj) {
                if (obj == null || getClass() != obj.getClass()) {
                  return false;
                }
                SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
                // BUG: Diagnostic contains: this.simpleField.equals(other.simpleField);
                return this.simpleField.equals(simpleField);
              }

              public boolean test4(SelfEqualsPositiveCase obj) {
                if (obj == null || getClass() != obj.getClass()) {
                  return false;
                }
                SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
                // BUG: Diagnostic contains: this.simpleField.equals(other.simpleField);
                return this.simpleField.equals(this.simpleField);
              }

              public boolean test5(SelfEqualsPositiveCase obj) {
                if (obj == null || getClass() != obj.getClass()) {
                  return false;
                }
                SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
                // BUG: Diagnostic contains:
                return equals(this);
              }

              public void testAssertTrue(SelfEqualsPositiveCase obj) {
                Assert.assertTrue(obj.equals(obj));
              }

              public void testAssertThat(SelfEqualsPositiveCase obj) {
                assertThat(obj.equals(obj)).isTrue();
              }

              @Override
              public boolean equals(Object obj) {
                if (obj == null || getClass() != obj.getClass()) {
                  return false;
                }
                SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
                return simpleField.equals(((SelfEqualsPositiveCase) other).simpleField);
              }

              private static class SubClass extends SelfEqualsPositiveCase {
                @Override
                public boolean equals(Object obj) {
                  if (obj == null || getClass() != obj.getClass()) {
                    return false;
                  }
                  SubClass other = (SubClass) obj;
                  return simpleField.equals(((SubClass) other).simpleField);
                }
              }

              public void testSub() {
                SubClass sc = new SubClass();
                // BUG: Diagnostic contains:
                sc.equals(sc);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "SelfEqualsNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            /**
             * Negative test cases for {@link SelfEquals} check.
             *
             * @author alexeagle@google.com (Alex Eagle)
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfEqualsNegativeCases {
              private String field;

              @Override
              public int hashCode() {
                return field != null ? field.hashCode() : 0;
              }

              @Override
              public boolean equals(Object o) {
                if (!(o instanceof SelfEqualsNegativeCases)) {
                  return false;
                }

                SelfEqualsNegativeCases other = (SelfEqualsNegativeCases) o;
                return field.equals(other.field);
              }

              public boolean test() {
                return Boolean.TRUE.toString().equals(Boolean.FALSE.toString());
              }

              public void testAssertThatEq(SelfEqualsNegativeCases obj) {
                assertThat(obj).isEqualTo(obj);
              }

              public void testAssertThatNeq(SelfEqualsNegativeCases obj) {
                assertThat(obj).isNotEqualTo(obj);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveFix() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              <T> boolean f() {
                T t = null;
                int y = 0;
                // BUG: Diagnostic contains:
                return t.equals(t);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCase_guava() {
    compilationHelper
        .addSourceLines(
            "SelfEqualsGuavaPositiveCase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.base.Objects;

            /**
             * @author alexeagle@google.com (Alex Eagle)
             */
            public class SelfEqualsGuavaPositiveCase {
              private String field = "";

              @Override
              public boolean equals(Object o) {
                if (this == o) {
                  return true;
                }
                if (o == null || getClass() != o.getClass()) {
                  return false;
                }
                SelfEqualsGuavaPositiveCase other = (SelfEqualsGuavaPositiveCase) o;
                boolean retVal;
                // BUG: Diagnostic contains: Objects.equal(field, other.field)
                retVal = Objects.equal(field, field);
                // BUG: Diagnostic contains: Objects.equal(other.field, this.field)
                retVal &= Objects.equal(field, this.field);
                // BUG: Diagnostic contains: Objects.equal(this.field, other.field)
                retVal &= Objects.equal(this.field, field);
                // BUG: Diagnostic contains: Objects.equal(this.field, other.field)
                retVal &= Objects.equal(this.field, this.field);

                return retVal;
              }

              @Override
              public int hashCode() {
                return Objects.hashCode(field);
              }

              public static void test() {
                ForTesting tester = new ForTesting();
                // BUG: Diagnostic contains: Objects.equal(tester.testing.testing, tester.testing)
                Objects.equal(tester.testing.testing, tester.testing.testing);
              }

              private static class ForTesting {
                public ForTesting testing;
                public String string;
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase_guava() {
    compilationHelper
        .addSourceLines(
            "SelfEqualsGuavaNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.base.Objects;

            /**
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class SelfEqualsGuavaNegativeCases {
              private String field;

              @Override
              public boolean equals(Object o) {
                if (this == o) {
                  return true;
                }
                if (o == null || getClass() != o.getClass()) {
                  return false;
                }

                SelfEqualsGuavaNegativeCases other = (SelfEqualsGuavaNegativeCases) o;
                return Objects.equal(field, other.field);
              }

              @Override
              public int hashCode() {
                return field != null ? field.hashCode() : 0;
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void enclosingStatement() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Objects;

            class Test {
              Object a = new Object();
              // BUG: Diagnostic contains:
              boolean b = Objects.equal(a, a);
            }
            """)
        .doTest();
  }
}
