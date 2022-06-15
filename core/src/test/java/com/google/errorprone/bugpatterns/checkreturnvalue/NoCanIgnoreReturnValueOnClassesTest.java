/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.errorprone.bugpatterns.checkreturnvalue.NoCanIgnoreReturnValueOnClasses.CTOR_COMMENT;
import static com.google.errorprone.bugpatterns.checkreturnvalue.NoCanIgnoreReturnValueOnClasses.METHOD_COMMENT;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link NoCanIgnoreReturnValueOnClasses}. */
@RunWith(JUnit4.class)
public final class NoCanIgnoreReturnValueOnClassesTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(
          NoCanIgnoreReturnValueOnClasses.class, getClass());

  @Test
  public void testSimpleCase_returnsThis() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@CanIgnoreReturnValue",
            "public final class Client {",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testSimpleCase_returnsSelf() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@CanIgnoreReturnValue",
            "public final class Client {",
            "  public Client getValue() {",
            "    return self();",
            "  }",
            "  private Client self() {",
            "    return this;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    return self();",
            "  }",
            "  @CanIgnoreReturnValue",
            "  private Client self() {",
            "    return this;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testSimpleCase_returnsNewInstance() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@CanIgnoreReturnValue",
            "public final class Client {",
            "  public Client getValue() {",
            "    return new Client();",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue" + METHOD_COMMENT,
            "  public Client getValue() {",
            "    return new Client();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testSimpleCase_explicitConstructor() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@CanIgnoreReturnValue",
            "public final class Client {",
            "  Client() {}",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue" + CTOR_COMMENT,
            "  Client() {}",
            "  @CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testNestedClasses_cirvAndCrv() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "@CanIgnoreReturnValue",
            "public final class Client {",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "  @CheckReturnValue",
            "  public static final class Nested {",
            "    public int getValue() {",
            "      return 42;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "  @CheckReturnValue",
            "  public static final class Nested {",
            "    public int getValue() {",
            "      return 42;",
            "    }",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testNestedClasses_bothCirv() {
    helper
        .addInputLines(
            "User.java",
            "package com.google.gaia;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@CanIgnoreReturnValue",
            "public final class User {",
            "  public User persist() {",
            "    return this;",
            "  }",
            "  public static final class Builder {",
            "    public Builder setFirstName(String firstName) {",
            "      return this;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "User.java",
            "package com.google.gaia;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class User {",
            "  @CanIgnoreReturnValue",
            "  public User persist() {",
            "    return this;",
            "  }",
            "  public static final class Builder {",
            "    @CanIgnoreReturnValue",
            "    public Builder setFirstName(String firstName) {",
            "      return this;",
            "    }",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testAutoValue() {
    helper
        .addInputLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@AutoValue",
            "@CanIgnoreReturnValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setName(String value);",
            "    abstract Animal build();",
            "  }",
            "}")
        .addOutputLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@AutoValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setName(String value);",
            "    abstract Animal build();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testAutoValueBuilder() {
    helper
        .addInputLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@AutoValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  @CanIgnoreReturnValue",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setName(String value);",
            "    abstract Animal build();",
            "  }",
            "}")
        .addOutputLines(
            "Animal.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@AutoValue",
            "abstract class Animal {",
            "  abstract String name();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setName(String value);",
            "    abstract Animal build();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testNestedAutoValue() {
    helper
        .addInputLines(
            "Outer.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "@CanIgnoreReturnValue",
            "public class Outer {",
            "  public String name() {",
            "    return null;",
            "  }",
            "  @AutoValue",
            "  abstract static class Inner {",
            "    abstract String id();",
            "  }",
            "}")
        .addOutputLines(
            "Outer.java",
            "package com.google.frobber;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public class Outer {",
            "  @CanIgnoreReturnValue" + METHOD_COMMENT,
            "  public String name() {",
            "    return null;",
            "  }",
            "  @AutoValue",
            "  abstract static class Inner {",
            "    abstract String id();",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
