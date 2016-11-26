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
package com.google.errorprone.fixes;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for BranchSuggestedFixes */
@RunWith(JUnit4.class)
public class BranchedSuggestedFixesTest {

  @Test
  public void testCombinesBranchWithFirst() {
    ImmutableList<SuggestedFix> fixes =
        BranchedSuggestedFixes.builder()
            .startWith(SuggestedFix.builder().addImport("A").build())
            .then()
            .addOption(SuggestedFix.builder().addImport("B").build())
            .addOption(SuggestedFix.builder().addImport("C").build())
            .build()
            .getFixes();

    assertThat(fixes.size()).isEqualTo(2);
    assertThat(fixes.get(0).getImportsToAdd()).containsExactly("import A", "import B");
    assertThat(fixes.get(1).getImportsToAdd()).containsExactly("import A", "import C");
  }

  @Test
  public void testEmptyIfNoProgress() {
    ImmutableList<SuggestedFix> fixes =
        BranchedSuggestedFixes.builder()
            .startWith(SuggestedFix.builder().addImport("A").build())
            .then()
            .then()
            .build()
            .getFixes();
    assertThat(fixes).isEmpty();
  }

  @Test
  public void testEmptyIfResumedProgress() {
    ImmutableList<SuggestedFix> fixes =
        BranchedSuggestedFixes.builder()
            .startWith(SuggestedFix.builder().addImport("A").build())
            .then()
            .then()
            .addOption(SuggestedFix.builder().addImport("B").build())
            .build()
            .getFixes();
    assertThat(fixes).isEmpty();
  }
}
