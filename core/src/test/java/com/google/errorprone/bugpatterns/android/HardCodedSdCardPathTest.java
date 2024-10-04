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

package com.google.errorprone.bugpatterns.android;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
@RunWith(JUnit4.class)
public class HardCodedSdCardPathTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(HardCodedSdCardPath.class, getClass());

  @Test
  public void matchingCode_onAndroid() {
    compilationHelper
        .setArgs(ImmutableList.of("-XDandroidCompatible=true"))
        .addSourceLines(
            "HardCodedSdCardPathPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class HardCodedSdCardPathPositiveCases {
              // BUG: Diagnostic contains: Environment
              static final String PATH1 = "/sdcard";

              // BUG: Diagnostic contains: Environment
              static final String PATH2 = "/sdcard/file1";

              // BUG: Diagnostic contains: Environment
              static final String PATH3 = "/mnt/sdcard/file2";

              // BUG: Diagnostic contains: Environment
              static final String PATH4 = "/" + "sd" + "card";

              // BUG: Diagnostic contains: Environment
              static final String PATH5 = "/system/media/sdcard";

              // BUG: Diagnostic contains: Environment
              static final String PATH6 = "/system/media/sdcard/file3";

              // BUG: Diagnostic contains: Environment
              static final String PATH7 = "file://sdcard/file2";

              // BUG: Diagnostic contains: Environment
              static final String PATH8 = "file:///sdcard/file2";

              // BUG: Diagnostic contains: Context
              static final String PATH9 = "/data/data/dir/file";

              // BUG: Diagnostic contains: Context
              static final String PATH10 = "/data/user/file1";

              static final String FRAGMENT1 = "/data";

              static final String FRAGMENT2 = "/user";

              // BUG: Diagnostic contains: Context
              static final String PATH11 = "/data" + "/" + "user";
            }""")
        .doTest();
  }

  @Test
  public void matchingCode_notOnAndroid() {
    compilationHelper
        .setArgs(ImmutableList.of("-XDandroidCompatible=false"))
        .addSourceLines(
            "HardCodedSdCardPathMatchingCode.java",
            """
            public class HardCodedSdCardPathMatchingCode {
              static final String PATH1 = "/sdcard";
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .setArgs(ImmutableList.of("-XDandroidCompatible=true"))
        .addSourceLines(
            "HardCodedSdCardPathNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class HardCodedSdCardPathNegativeCases {
              static final String PATH1 = "/home/sdcard";

              static final String PATH2 = "/data/file1";

              static final String FRAGMENT1 = "/root";

              static final String FRAGMENT2 = "sdcard";

              static final String PATH3 = FRAGMENT1 + "/" + FRAGMENT2;

              static final String PATH4 = "/data/dir/file2";

              static final String FRAGMENT3 = "/data";

              static final String FRAGMENT4 = "1user";

              static final String PATH5 = FRAGMENT3 + "/" + FRAGMENT4;
            }""")
        .doTest();
  }
}
