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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link CanIgnoreReturnValueSuggester}. */
@RunWith(JUnit4.class)
public class CanIgnoreReturnValueSuggesterTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(CanIgnoreReturnValueSuggester.class, getClass());

  @Test
  public void testSimpleCase() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
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
        .doTest();
  }

  @Test
  public void testSimpleCaseAlreadyAnnotatedWithCirv() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSimpleCaseAlreadyAnnotatedWithCrv() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "public final class Client {",
            "  @CheckReturnValue", // this is "wrong" -- the checker could fix it though!
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSimpleCaseWithNestedLambda() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.util.function.Function;",
            "public final class Client {",
            "  public Client getValue() {",
            "    new Function<String, String>() {",
            "      @Override",
            "      public String apply(String in) {",
            "        return \"kurt\";",
            "      }",
            "    };",
            "    return this;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "import java.util.function.Function;",
            "public final class Client {",
            "  @CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    new Function<String, String>() {",
            "      @Override",
            "      public String apply(String in) {",
            "        return \"kurt\";",
            "      }",
            "    };",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnotherMethodDoesntReturnThis() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Client getValue1() {",
            "    return this;",
            "  }",
            "  public Client getValue2() {",
            "    return new Client();",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CanIgnoreReturnValue;",
            "public final class Client {",
            "  @CanIgnoreReturnValue",
            "  public Client getValue1() {",
            "    return this;",
            "  }",
            "  public Client getValue2() {",
            "    return new Client();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNestedCase() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Client getValue() {",
            "    if (true) {",
            "      return new Client();",
            "    }",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testNestedCaseBothReturningThis() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Client getValue() {",
            "    if (true) {",
            "      return this;",
            "    }",
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
            "    if (true) {",
            "      return this;",
            "    }",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCapitalVoidReturnType() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Void getValue() {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testLowerVoidReturnType() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Void getValue() {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testConstructor() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Client() {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSometimesThrows() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Client getValue() {",
            "    if (true) throw new UnsupportedOperationException();",
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
            "    if (true) throw new UnsupportedOperationException();",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAlwaysThrows() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public Client getValue() {",
            "    throw new UnsupportedOperationException();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSimpleCaseWithSimpleNameConflict() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public @interface CanIgnoreReturnValue {}",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  public @interface CanIgnoreReturnValue {}",
            "  @com.google.errorprone.annotations.CanIgnoreReturnValue",
            "  public Client getValue() {",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }
}
