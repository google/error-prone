/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author pvisontay@google.com */
@RunWith(JUnit4.class)
public final class BinderIdentityRestoredDangerouslyTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(BinderIdentityRestoredDangerously.class, getClass())
          .addSourceFile("testdata/stubs/android/os/Binder.java")
          .setArgs(ImmutableList.of("-XDandroidCompatible=true"));

  @Test
  public void releasedInFinallyBlock_shouldBeOkay() {
    compilationHelper
        .addSourceLines(
            "InFinally.java",
            "import android.os.Binder;",
            "public class InFinally {",
            "  void foo() {",
            "    long identity = Binder.clearCallingIdentity();",
            "    try {",
            "      // Do something (typically Binder IPC) ",
            "    } finally {",
            "      Binder.restoreCallingIdentity(identity);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void releasedInFinallyBlock_shouldWarn() {
    compilationHelper
        .addSourceLines(
            "InFinally.java",
            "import android.os.Binder;",
            "public class InFinally {",
            "  void foo() {",
            "    long identity = Binder.clearCallingIdentity();",
            "    // Do something (typically Binder IPC) ",
            "    // BUG: Diagnostic contains: Binder.restoreCallingIdentity() in a finally block",
            "    Binder.restoreCallingIdentity(identity);",
            "  }",
            "}")
        .doTest();
  }
}
