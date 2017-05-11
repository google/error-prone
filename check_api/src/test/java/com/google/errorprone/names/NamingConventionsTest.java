/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.names;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for code in NamingConventions */
@RunWith(JUnit4.class)
public class NamingConventionsTest {

  @Test
  public void splitToLowercaseTerms_separatesTerms_withLowerCamelCase() {
    String identifierName = "camelCaseTerm";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("camel", "case", "term");
  }

  @Test
  public void splitToLowercaseTerms_separatesTerms_withUpperCamelCase() {
    String identifierName = "CamelCaseTerm";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("camel", "case", "term");
  }

  @Test
  public void splitToLowercaseTerms_separatesTrailingDigits_withoutDelimiter() {
    String identifierName = "term123";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("term", "123");
  }

  @Test
  public void splitToLowercaseTerms_doesntSplit_withIntermediateDigits() {
    String identifierName = "i8n";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("i8n");
  }

  @Test
  public void splitToLowercaseTerms_separatesTerms_withUnderscoreSeparator() {
    String identifierName = "UNDERSCORE_SEPARATED";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("underscore", "separated");
  }

  @Test
  public void splitToLowercaseTerms_findsSingleTerm_withOnlyUnderscore() {
    String identifierName = "_____";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("_____");
  }

  @Test
  public void splitToLowercaseTerms_noEmptyTerm_withTrailingUnderscoreDigit() {
    String identifierName = "test_1";

    ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(identifierName);

    assertThat(terms).containsExactly("test", "1");
  }

  @Test
  public void convertToLowerUnderscore_givesSingleUnderscore_fromSingleUnderscore() {
    String identifierName = "_";

    String lowerUnderscore = NamingConventions.convertToLowerUnderscore(identifierName);

    assertThat(lowerUnderscore).isEqualTo("_");
  }

  @Test
  public void convertToLowerUnderscore_separatesTerms_fromCamelCase() {
    String identifierName = "camelCase";

    String lowerUnderscore = NamingConventions.convertToLowerUnderscore(identifierName);

    assertThat(lowerUnderscore).isEqualTo("camel_case");
  }
}
