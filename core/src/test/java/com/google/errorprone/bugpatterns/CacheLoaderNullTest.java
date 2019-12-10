/*
 * Copyright 2019 The Error Prone Authors.
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

/** {@link CacheLoaderNull}Test */
@RunWith(JUnit4.class)
public class CacheLoaderNullTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CacheLoaderNull.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.cache.CacheLoader;",
            "class Test {",
            "  {",
            "    new CacheLoader<String, String>() {",
            "      @Override",
            "      public String load(String key) {",
            "        // BUG: Diagnostic contains:",
            "        return null;",
            "      }",
            "    };",
            "    abstract class MyCacheLoader extends CacheLoader<String, String> {}",
            "    new MyCacheLoader() {",
            "      @Override",
            "      public String load(String key) {",
            "        // BUG: Diagnostic contains:",
            "        return null;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.cache.CacheLoader;",
            "import java.util.function.Supplier;",
            "class Test {",
            "  public String load(String key) {",
            "    return null;",
            "  }",
            "  {",
            "    new CacheLoader<String, String>() {",
            "      @Override",
            "      public String load(String key) {",
            "        Supplier<String> s = () -> { return null; };",
            "        Supplier<String> t = new Supplier<String>() {",
            "          public String get() {",
            "            return null;",
            "          }",
            "        };",
            "        class MySupplier implements Supplier<String> {",
            "          public String get() {",
            "            return null;",
            "          }",
            "        };",
            "        return \"\";",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
