/*
 * Copyright 2020 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ReturnsNullCollection}Test */
@RunWith(JUnit4.class)
public class ReturnsNullCollectionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ReturnsNullCollection.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "import java.util.Map;",
            "class Test {",
            "  Collection<String> methodReturnsNullCollection() {",
            "  // BUG: Diagnostic contains: ReturnsNullCollection",
            "    return null;",
            "  }",
            "  List<String> methodReturnsNullList() {",
            "  // BUG: Diagnostic contains: ReturnsNullCollection",
            "    return null;",
            "  }",
            "  Map<String, String> methodReturnsNullMap() {",
            "  // BUG: Diagnostic contains: ReturnsNullCollection",
            "    return null;",
            "  }",
            "  List<String> methodReturnsNullListConditionally(boolean foo) {",
            "    if (foo) {",
            "      // BUG: Diagnostic contains: ReturnsNullCollection",
            "      return null;",
            "    }",
            "    return new ArrayList();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "import java.util.HashMap;",
            "import java.util.List;",
            "import java.util.Map;",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nullable",
            "  Collection<String> methodReturnsNullCollection() {",
            "    return null;",
            "  }",
            "  @Nullable",
            "  List<String> methodReturnsNullList() {",
            "    return null;",
            "  }",
            "  @Nullable",
            "  Map<String, String> methodReturnsNullMap() {",
            "    return null;",
            "  }",
            "  @Nullable",
            "  HashMap<String, String> methodReturnsNullHashMap() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }
}
