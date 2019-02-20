/*
 * Copyright 2011 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.apply;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link SourceFile}s.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@RunWith(JUnit4.class)
public class SourceFileTest {

  private static final String DUMMY_PATH = "java/com/google/foo/bar/FooBar.java";
  private static final String SOURCE_TEXT =
      "// Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\n"
          + "// eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut\n"
          + "// enim ad minim veniam, quis nostrud exercitation ullamco\n"
          + "// laboris nisi ut aliquip ex ea commodo consequat. Duis aute\n"
          + "// irure dolor in reprehenderit in voluptate velit esse cillum dolore\n"
          + "// eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat\n"
          + "// non proident, sunt in culpa qui officia deserunt mollit anim id\n"
          + "// est laborum.\n";

  private SourceFile sourceFile;

  @Before
  public void setUp() {
    sourceFile = new SourceFile(DUMMY_PATH, SOURCE_TEXT);
  }

  @Test
  public void getSourceText() {
    assertThat(sourceFile.getSourceText()).isEqualTo(SOURCE_TEXT);
  }

  @Test
  public void getPath() {
    assertThat(sourceFile.getPath()).isEqualTo(DUMMY_PATH);
  }

  @Test
  public void getLines() {
    List<String> lines = sourceFile.getLines();
    assertThat(lines).hasSize(8);
    assertThat(lines.get(0))
        .isEqualTo("// Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do");
    assertThat(lines.get(7)).isEqualTo("// est laborum.");
  }

  @Test
  public void replaceChars() {
    sourceFile.replaceChars(3, 8, "Sasquatch");
    assertThat(sourceFile.getSourceText()).isEqualTo(SOURCE_TEXT.replace("Lorem", "Sasquatch"));
    assertThat(sourceFile.getLines().get(0))
        .isEqualTo("// Sasquatch ipsum dolor sit amet, consectetur adipisicing elit, sed do");
  }

  @Test
  public void replaceLines() {
    sourceFile.replaceLines(Arrays.asList("Line1", "Line2"));
    assertThat(sourceFile.getSourceText()).isEqualTo("Line1\nLine2\n");
  }

  @Test
  public void replaceLines_numbered() {
    sourceFile.replaceLines(2, 5, Arrays.asList("cat", "dog"));
    assertThat(sourceFile.getSourceText())
        .isEqualTo(
            "// Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\n"
                + "cat\n"
                + "dog\n"
                + "// eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat\n"
                + "// non proident, sunt in culpa qui officia deserunt mollit anim id\n"
                + "// est laborum.\n");
  }

  @Test
  public void getFragmentByChars() {
    assertThat(sourceFile.getFragmentByChars(3, 8)).isEqualTo("Lorem");
  }

  @Test
  public void getFragmentByLines() {
    assertThat(sourceFile.getFragmentByLines(2, 2))
        .isEqualTo("// eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut\n");
    assertThat(sourceFile.getFragmentByLines(8, 8)).isEqualTo("// est laborum.\n");
    assertThat(sourceFile.getFragmentByLines(2, 3))
        .isEqualTo(
            "// eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut\n"
                + "// enim ad minim veniam, quis nostrud exercitation ullamco\n");
    assertThat(sourceFile.getFragmentByLines(1, 8)).isEqualTo(SOURCE_TEXT);
  }
}
