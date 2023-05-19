/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.common.truth.Truth8.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Regexes}Test */
@RunWith(JUnit4.class)
public final class RegexesTest {

  @Test
  public void positive() {
    assertThat(Regexes.convertRegexToLiteral("hello")).hasValue("hello");
    assertThat(Regexes.convertRegexToLiteral("\\t\\n\\f\\r")).hasValue("\t\n\f\r");
    assertThat(Regexes.convertRegexToLiteral("\\Q.\\E")).hasValue(".");
  }

  @Test
  public void negative() {
    assertThat(Regexes.convertRegexToLiteral("[a-z]+")).isEmpty();
  }
}
