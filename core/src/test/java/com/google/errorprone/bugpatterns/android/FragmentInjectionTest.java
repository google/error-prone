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

/** @author epmjohnston@google.com (Emily P.M. Johnston) */
@RunWith(JUnit4.class)
public final class FragmentInjectionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FragmentInjection.class, getClass())
          .addSourceFile("testdata/stubs/android/preference/PreferenceActivity.java")
          .setArgs(ImmutableList.of("-XDandroidCompatible=true"));

  @Test
  public void isValidFragmentNotImplementedOnPreferenceActivity() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "// BUG: Diagnostic contains: does not implement isValidFragment",
            "class MyPrefActivity extends PreferenceActivity {}")
        .doTest();
  }

  @Test
  public void methodNamedIsValidFragmentButDoesNotOverride() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "// BUG: Diagnostic contains: does not implement isValidFragment",
            "class MyPrefActivity extends PreferenceActivity {",
            "  protected boolean isValidFragment(String fragment, String unused) {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentTriviallyImplemented() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  // BUG: Diagnostic contains: isValidFragment unconditionally returns true",
            "  protected boolean isValidFragment(String fragment) {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentReturnsConstantField() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  static final boolean known = true;",
            "  // BUG: Diagnostic contains: isValidFragment unconditionally returns true",
            "  protected boolean isValidFragment(String fragment) {",
            "    return known;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentReturnsFalse() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  protected boolean isValidFragment(String fragment) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentReturnsBoxedTrue() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  protected boolean isValidFragment(String fragment) {",
            "    return Boolean.valueOf(true);", // No warning, not a compile time constant.
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentReturnsVariable() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  boolean unknown;",
            "  protected boolean isValidFragment(String fragment) {",
            "    return unknown;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentFullyImplemented() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  protected boolean isValidFragment(String fragment) {",
            "    if (\"VALID_FRAGMENT\".equals(fragment)) {",
            "      return true;",
            "    }",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodWithSameSignatureImplementedOnOtherClass() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "class MyPrefActivity {",
            "  protected boolean isValidFragment(String fragment) {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentImpelementedOnSuperClass() {
    compilationHelper
        .addSourceLines(
            "MySuperPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MySuperPrefActivity extends PreferenceActivity {",
            "  protected boolean isValidFragment(String fragment) {",
            "    if (\"VALID_FRAGMENT\".equals(fragment)) {",
            "      return true;",
            "    }",
            "    return false;",
            "  }",
            "}")
        .addSourceLines(
            "MyPrefActivity.java",
            // Okay, implemented on super class.
            "class MyPrefActivity extends MySuperPrefActivity {}")
        .doTest();
  }

  @Test
  public void isValidFragmentImpelementedOnAbstractSuperClass() {
    compilationHelper
        .addSourceLines(
            "MySuperPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "abstract class MySuperPrefActivity extends PreferenceActivity {",
            "  protected boolean isValidFragment(String fragment) {",
            "    if (\"VALID_FRAGMENT\".equals(fragment)) {",
            "      return true;",
            "    }",
            "    return false;",
            "  }",
            "}")
        .addSourceLines(
            "MyPrefActivity.java",
            // Okay, implemented on super class.
            "class MyPrefActivity extends MySuperPrefActivity {}")
        .doTest();
  }

  @Test
  public void abstractClassWithoutIsValidFragmentIsOkay() {
    compilationHelper
        .addSourceLines(
            "MyAbstractPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            // Okay, abstract so implementing class can implement isValidFragment.
            "abstract class MyAbstractPrefActivity extends PreferenceActivity {}")
        .doTest();
  }

  @Test
  public void noIsValidFragmentOnAbstractSuperClassOrImplementation() {
    compilationHelper
        .addSourceLines(
            "MyAbstractPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            // Don't emit warning since it's abstract.
            "abstract class MyAbstractPrefActivity extends PreferenceActivity {}")
        .addSourceLines(
            "MyPrefActivity.java",
            "// BUG: Diagnostic contains: does not implement isValidFragment",
            "class MyPrefActivity extends MyAbstractPrefActivity {}")
        .doTest();
  }

  @Test
  public void isValidFragmentTriviallyImplementedOnAbstractClass() {
    compilationHelper
        .addSourceLines(
            "MyAbstractPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "abstract class MyAbstractPrefActivity extends PreferenceActivity {",
            "  // BUG: Diagnostic contains: isValidFragment unconditionally returns true",
            "  protected boolean isValidFragment(String fragment) {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void isValidFragmentThrowsExceptionReturnsTrue() {
    // N.B. In future we may make an exception for methods which include throw statements.
    // In that case, just reverse this test (remove the BUG comment below).
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  // BUG: Diagnostic contains: isValidFragment unconditionally returns true",
            "  protected boolean isValidFragment(String fragment) {",
            "    if (\"VALID_FRAGMENT\".equals(fragment)) {",
            "      throw new RuntimeException(\"Not a valid fragment!\");",
            "    }",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ifTrueElseTrue() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  // BUG: Diagnostic contains: isValidFragment unconditionally returns true",
            "  protected boolean isValidFragment(String fragment) {",
            "    if (\"VALID_FRAGMENT\".equals(fragment)) {",
            "      return true;",
            "    }",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalLocalVariableIsConstant() {
    compilationHelper
        .addSourceLines(
            "MyPrefActivity.java",
            "import android.preference.PreferenceActivity;",
            "class MyPrefActivity extends PreferenceActivity {",
            "  // BUG: Diagnostic contains: isValidFragment unconditionally returns true",
            "  protected boolean isValidFragment(String fragment) {",
            "    final boolean constTrue = true;",
            "    return constTrue;",
            "  }",
            "}")
        .doTest();
  }
}
