/*
 * Copyright 2023 The Error Prone Authors.
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

/** Tests for the {@link ReturnAtTheEndOfVoidFunction}. */
@RunWith(JUnit4.class)
public class ReturnAtTheEndOfVoidFunctionTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(ReturnAtTheEndOfVoidFunction.class, getClass());

  @Test
  public void lastReturnIsDeleted() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public void stuff() {",
            "    int x = 5;",
            "    return;",
            "  }",
            "}")
        .addOutputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public void stuff() {",
            "    int x = 5;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lastReturnIsNotDeleted() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public int stuff() {",
            "    int x = 5;",
            "    return x;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returnAtDifferentPositionIsNotDeleted() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public void stuff() {",
            "    int x = 5;",
            "    if(x > 2) {",
            "     return;",
            "    }",
            "    int z = 2173;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void emptyFunctionIsUnchanged() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public void nothing() {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nullReturnInVoidIsUnchanged() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public Void nothing() {",
            "    return null;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void constructorIsCleaned() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public Builder() {",
            "    return;",
            "  }",
            "}")
        .addOutputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public final class Builder {",
            "  public Builder() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void abstractDoesntCrash() {
    helper
        .addInputLines(
            "Builder.java",
            "package com.google.gporeba;",
            "public abstract class Builder {",
            "  public abstract void stuff();",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
