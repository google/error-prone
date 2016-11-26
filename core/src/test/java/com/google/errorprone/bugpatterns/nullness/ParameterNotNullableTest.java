/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author kmb@google.com (Kevin Bierhoff) */
@RunWith(JUnit4.class)
public class ParameterNotNullableTest {

  @Test
  public void testMethodInvocationOnParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public static String toString(@Nullable Integer x) {",
            "    // BUG: Diagnostic contains: toString( Integer",
            "    return x.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFieldDereferenceOnParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  private String message;",
            "  public static String toString(@Nullable ParameterDereferenceTest x) {",
            "    // BUG: Diagnostic contains: toString( ParameterDereferenceTest",
            "    return x.message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testArrayAccessOnParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public static int length(@Nullable int[] xs) {",
            "    // BUG: Diagnostic contains: length( int[]",
            "    return xs.length;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDereferenceInLambda() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public static java.util.function.Supplier<String> asSupplier(@Nullable Integer x) {",
            // TODO(kmb): Should complain about dereferences in lambdas and inner classes
            "    return (() -> x.toString());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_parameterNotNullable() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public static String toString(Integer x) {",
            "    return x.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_checkedForNull() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public static String toString(@Nullable Integer x) {",
            "    return x != null ? x.toString() : \"0\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_this() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public String toString(@Nullable Integer x) {",
            "    return this.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_super() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public String toString(@Nullable Integer x) {",
            "    return super.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_local() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  public static String throwing() {",
            "    @Nullable Integer x = null;",
            "    return x.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_field() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ParameterDereferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class ParameterDereferenceTest {",
            "  @Nullable Integer count;",
            "  public String getMessage() {",
            "    return count.toString();",
            "  }",
            "}")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(ParameterNotNullable.class, getClass());
  }
}
