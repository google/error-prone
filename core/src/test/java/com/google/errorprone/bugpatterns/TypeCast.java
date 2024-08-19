/*
 * Copyright 2024 The Error Prone Authors.
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
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.TypeCastTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TypeCast {

  public static final class TypeCastChecker extends BugChecker implements TypeCastTreeMatcher {

    @Override
    public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
      return null;
    }
  }

  @Test
  public void typeCase() {
    CompilationTestHelper.newInstance(TypeCastChecker.class, getClass())
        .addSourceLines(
            "T.java",
            "class T {",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "class T {",
            "    @Target(ElementType.TYPE_USE)",
            "    @interface Nullable {}",
            "    interface A<T> {}",
            "    void f(Object o) {",
            "        var x = (A<@Nullable String>) o;",
            "        // A<@Nullable String> a = null;",
            "    }",
            "}")
        .doTest();
  }
}
