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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link VisitorState}. */
@RunWith(JUnit4.class)
public class VisitorStateTest {

  @Test
  public void symbolFromString_defaultPackage() {
    assertThat(VisitorState.inferBinaryName("InDefaultPackage")).isEqualTo("InDefaultPackage");
  }

  @Test
  public void symbolFromString_nestedTypeInDefaultPackage() {
    assertThat(VisitorState.inferBinaryName("InDefaultPackage.Nested"))
        .isEqualTo("InDefaultPackage$Nested");
  }

  @Test
  public void symbolFromString_regularClass() {
    assertThat(VisitorState.inferBinaryName("test.RegularClass")).isEqualTo("test.RegularClass");
    assertThat(VisitorState.inferBinaryName("com.google.RegularClass"))
        .isEqualTo("com.google.RegularClass");
  }

  @Test
  public void symbolFromString_nestedTypeInRegularPackage() {
    assertThat(VisitorState.inferBinaryName("test.RegularClass.Nested"))
        .isEqualTo("test.RegularClass$Nested");
    assertThat(VisitorState.inferBinaryName("com.google.RegularClass.Nested"))
        .isEqualTo("com.google.RegularClass$Nested");
  }
}
