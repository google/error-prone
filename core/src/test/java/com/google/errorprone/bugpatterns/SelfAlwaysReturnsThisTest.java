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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link SelfAlwaysReturnsThis}. */
@RunWith(JUnit4.class)
public class SelfAlwaysReturnsThisTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(SelfAlwaysReturnsThis.class, getClass());

  @Test
  public void testSelfReturnsThis() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsThis_withCast() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    return (Builder) this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsThis_withCastAndTryCatch() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    try {",
            "      return (Builder) this;",
            "    } catch (ClassCastException e) {",
            // sometimes people log here?
            "      throw e;",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsThis_withMultipleReturnStatements() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    if (System.currentTimeMillis() % 2 == 0) {",
            "      return this;",
            "    } else {",
            "      return this;",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsThis_withTwoStatementCast() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    // sometimes people write comments here :-)",
            "    Builder self = (Builder) this;",
            "    return self;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsThis_withImplComment() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    // this is an impl comment",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsThis_withInlineComment() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    return /* self */ this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelfReturnsNewBuilder() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    return new Builder();",
            "  }",
            "}")
        .addOutputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self() {",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSelf_voidReturn() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public void self() {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelf_differentReturnType() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public String self() {",
            "    return \"hi\";",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelf_static() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public static Builder self() {",
            "    return new Builder();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelf_notNamedSelf() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder selfie() {",
            "    return new Builder();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelf_hasParams() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public final class Builder {",
            "  public Builder self(int foo) {",
            "    return new Builder();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSelf_abstract() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.frobber;",
            "public abstract class Builder {",
            "  public abstract Builder self();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  // TODO(kak): add a test for the inheritance style Builder (which requires a (T) cast).
}
