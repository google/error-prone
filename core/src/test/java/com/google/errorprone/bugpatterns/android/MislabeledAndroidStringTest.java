/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.CompilationTestHelper;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author kmb@google.com (Kevin Bierhoff) */
@RunWith(JUnit4.class)
public class MislabeledAndroidStringTest {

  @Test
  public void testMatchFullyQualified() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/android/MatchFullyQualifiedTest.java",
            "package com.google.errorprone.bugpatterns.android;",
            "public class MatchFullyQualifiedTest {",
            "  public int getStringId() {",
            "    // BUG: Diagnostic contains: android.R.string.ok",
            "    // android.R.string.yes is not \"Yes\" but \"OK\"; prefer android.R.string.ok",
            "    return android.R.string.yes;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatchWithImport() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/android/MatchWithImportTest.java",
            "package com.google.errorprone.bugpatterns.android;",
            "import android.R;",
            "public class MatchWithImportTest {",
            "  public int getStringId() {",
            "    // BUG: Diagnostic contains: R.string.cancel",
            "    // android.R.string.no is not \"No\" but \"Cancel\";",
            "    // prefer android.R.string.cancel",
            "    return R.string.no;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testUseInField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/android/MatchUseInFieldTest.java",
            "package com.google.errorprone.bugpatterns.android;",
            "public class MatchUseInFieldTest {",
            "  // BUG: Diagnostic contains: android.R.string.ok",
            "  // android.R.string.yes is not \"Yes\" but \"OK\"; prefer android.R.string.ok",
            "  private static final int SAY_YES = android.R.string.yes;",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/android/FineStringTest.java",
            "package com.google.errorprone.bugpatterns.android;",
            "import android.R;",
            "public class FineStringTest {",
            "  public int getStringId() {",
            "    return R.string.copy;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Ensures that {@link MislabeledAndroidString#ASSUMED_MEANINGS} is complete, which is important
   * for generating readable diagnostic messages.
   */
  @Test
  public void testAssumedMeanings() {
    for (Map.Entry<String, String> label : MislabeledAndroidString.MISLEADING.entrySet()) {
      assertThat(MislabeledAndroidString.ASSUMED_MEANINGS).containsKey(label.getKey());
      assertThat(MislabeledAndroidString.ASSUMED_MEANINGS).containsKey(label.getValue());
    }
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(MislabeledAndroidString.class, getClass())
        .addSourceFile("testdata/stubs/android/R.java");
  }
}
