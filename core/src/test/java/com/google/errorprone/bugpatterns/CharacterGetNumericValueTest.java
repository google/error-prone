/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.CompilationTestHelper;
import com.ibm.icu.lang.UCharacter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CharacterGetNumericValue} */
@RunWith(JUnit4.class)
public class CharacterGetNumericValueTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(CharacterGetNumericValue.class, getClass());

  @Test
  public void characterGetNumericValue_unexpectedBehavior() {
    // Verify that the unexpected behavior still holds
    // Expect 'A' - 'Z' to map to 10 - 35
    for (int c = (int) 'A'; c < (int) 'Z'; c++) {
      assertThat(Character.getNumericValue((char) c)).isEqualTo(c - (int) 'A' + 10);
      assertThat(Character.getNumericValue(c)).isEqualTo(c - (int) 'A' + 10);
    }
    assertThat(Character.getNumericValue('Ⅴ' /* U+2164, Roman numeral 5 */)).isEqualTo(5);
    assertThat(Character.getNumericValue('V')).isEqualTo(31);
  }

  @Test
  public void uCharacterGetNumericValue_unexpectedBehavior() {
    // Verify that the unexpected behavior still holds
    // Expect 'A' - 'Z' to map to 10 - 35
    for (int c = (int) 'A'; c < (int) 'Z'; c++) {
      assertThat(UCharacter.getNumericValue((char) c)).isEqualTo(c - (int) 'A' + 10);
      assertThat(UCharacter.getNumericValue(c)).isEqualTo(c - (int) 'A' + 10);
    }
    assertThat(UCharacter.getNumericValue('Ⅴ' /* U+2164, Roman numeral 5 */)).isEqualTo(5);
    assertThat(UCharacter.getNumericValue('V')).isEqualTo(31);
  }

  @Test
  public void characterDigit_expectedBehavior() {
    assertThat(Character.digit('Z', 36)).isEqualTo(35);
    assertThat(Character.digit('௧' /* U+0BE7, Tamil digit 1 */, 36)).isEqualTo(1);
    assertThat(Character.digit('௲' /* U+0BF2, Tamil number 1000 */, 36)).isEqualTo(-1);
    assertThat(Character.digit('Ⅴ' /* U+2164, Roman numeral 5 */, 36)).isEqualTo(-1);
  }

  @Test
  public void uCharacterDigit_expectedBehavior() {
    assertThat(UCharacter.digit('Z', 36)).isEqualTo(35);
    assertThat(UCharacter.digit('௧' /* U+0BE7, Tamil digit 1 */, 36)).isEqualTo(1);
    assertThat(UCharacter.digit('௲' /* U+0BF2, Tamil number 1000 */, 36)).isEqualTo(-1);
    assertThat(UCharacter.digit('Ⅴ' /* U+2164, Roman numeral 5 */, 36)).isEqualTo(-1);
  }

  @Test
  public void character_getNumericValue_char() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.lang.Character;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: CharacterGetNumericValue",
            "    Character.getNumericValue('A');",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void character_getNumericValue_int() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.lang.Character;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: CharacterGetNumericValue",
            "    Character.getNumericValue(41);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void uCharacter_getNumericValue_char() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.ibm.icu.lang.UCharacter;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: CharacterGetNumericValue",
            "    UCharacter.getNumericValue(41);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void uCharacter_getNumericValue_int() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.ibm.icu.lang.UCharacter;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: CharacterGetNumericValue",
            "    UCharacter.getNumericValue(41);",
            "  }",
            "}")
        .doTest();
  }
}
