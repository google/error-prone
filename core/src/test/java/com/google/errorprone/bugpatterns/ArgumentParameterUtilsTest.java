/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ArgumentParameterUtils. */
@RunWith(JUnit4.class)
public final class ArgumentParameterUtilsTest {
  @Test
  public void similarityOfArgToParams_identicalToOneParam() {
    List<String> params = ImmutableList.of("keepPath", "dropPath");
    assertThat(ArgumentParameterUtils.similarityOfArgToParams("keepPath", params))
        .containsAllOf(1.0, 0.5);
  }

  @Test
  public void similarityOfArgToParams_similarToOneParam() {
    List<String> params = ImmutableList.of("keepPath", "dropPath");
    assertThat(ArgumentParameterUtils.similarityOfArgToParams("keep", params))
        .containsAllOf(0.6666666666666666, 0.0);
  }

  @Test
  public void lexicalSimilarity_identicalCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("keepPath", "keepPath")).isEqualTo(1.0);
  }

  @Test
  public void lexicalSimilarity_noCommonTermsCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("keepPath", "dropList")).isEqualTo(0.0);
  }

  @Test
  public void lexicalSimilarity_oneCommonTermCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("keep", "keepPath"))
        .isEqualTo(0.6666666666666666);
  }

  @Test
  public void lexicalSimilarity_noCommonTermAndSwapCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("drop", "keepPath")).isEqualTo(0.0);
  }

  @Test
  public void lexicalSimilarity_oneCommonTermAndSwapCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("dropPath", "keepPath")).isEqualTo(0.5);
  }

  @Test
  public void lexicalSimilarity_manyTermsCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("dropPath", "dropThisSpecificPathNow"))
        .isEqualTo(0.5714285714285714);
  }

  @Test
  public void lexicalSimilarity_manyTermsAndSwapCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("keepPath", "dropThisSpecificPathNow"))
        .isEqualTo(0.2857142857142857);
  }

  @Test
  public void lexicalSimilarity_falsePositiveCase() {
    assertThat(ArgumentParameterUtils.lexicalSimilarity("fooBar", "barFoo")).isEqualTo(1.0);
  }
}
