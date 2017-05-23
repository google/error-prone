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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of internal methods and flag parsing for new-style Error Prone command-line flags. */
@RunWith(JUnit4.class)
public final class ErrorProneFlagsTest {

  @Test
  public void parseAndGetStringValue() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:SomeArg=SomeValue")
            .parseFlag("-XepOpt:Other:Arg:More:Parts=Long")
            .parseFlag("-XepOpt:EmptyArg=")
            .build();
    assertThat(flags.get("SomeArg")).hasValue("SomeValue");
    assertThat(flags.get("Other:Arg:More:Parts")).hasValue("Long");
    assertThat(flags.get("EmptyArg")).hasValue("");
    assertThat(flags.get("absent")).isEmpty();
  }

  @Test
  public void parseAndGetBoolean() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            // Boolean parsing should ignore case.
            .parseFlag("-XepOpt:Arg1=tRuE")
            .parseFlag("-XepOpt:Arg2=FaLsE")
            .parseFlag("-XepOpt:Arg3=yes")
            .build();
    assertThat(flags.getBoolean("Arg1")).hasValue(true);
    assertThat(flags.getBoolean("Arg2")).hasValue(false);
    assertThrows(IllegalArgumentException.class, () -> flags.getBoolean("Arg3"));
    assertThat(flags.getBoolean("absent")).isEmpty();
  }

  @Test
  public void parseAndGetImplicitTrue() {
    ErrorProneFlags flags = ErrorProneFlags.builder().parseFlag("-XepOpt:SomeArg").build();
    assertThat(flags.getBoolean("SomeArg")).hasValue(true);
  }

  @Test
  public void parseAndGetInteger() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:Arg1=10")
            .parseFlag("-XepOpt:Arg2=20.6")
            .parseFlag("-XepOpt:Arg3=thirty")
            .build();
    assertThat(flags.getInteger("Arg1")).hasValue(10);
    assertThrows(NumberFormatException.class, () -> flags.getInteger("Arg2"));
    assertThrows(NumberFormatException.class, () -> flags.getInteger("Arg3"));
    assertThat(flags.getInteger("absent")).isEmpty();
  }

  @Test
  public void parseAndGetList() {
    ErrorProneFlags flags =
        ErrorProneFlags.builder()
            .parseFlag("-XepOpt:ArgA=1,2,3")
            .parseFlag("-XepOpt:ArgB=4,")
            .parseFlag("-XepOpt:ArgC=5,,,6")
            .parseFlag("-XepOpt:ArgD=7")
            .parseFlag("-XepOpt:ArgE=")
            .build();
    assertThat(flags.getList("ArgA")).hasValue(ImmutableList.of("1", "2", "3"));
    assertThat(flags.getList("ArgB")).hasValue(ImmutableList.of("4", ""));
    assertThat(flags.getList("ArgC")).hasValue(ImmutableList.of("5", "", "", "6"));
    assertThat(flags.getList("ArgD")).hasValue(ImmutableList.of("7"));
    assertThat(flags.getList("ArgE")).hasValue(ImmutableList.of(""));
    assertThat(flags.getList("absent")).isEmpty();
  }

  @Test
  public void plus_secondShouldOverwriteFirst() {
    ErrorProneFlags flags1 =
        ErrorProneFlags.builder().putFlag("a", "FIRST_A").putFlag("b", "FIRST_B").build();
    ErrorProneFlags flags2 =
        ErrorProneFlags.builder().putFlag("b", "b2").putFlag("c", "c2").build();

    ImmutableMap<String, String> expectedCombinedMap =
        ImmutableMap.<String, String>builder()
            .put("a", "FIRST_A")
            .put("b", "b2")
            .put("c", "c2")
            .build();

    Map<String, String> actualCombinedMap = flags1.plus(flags2).getFlagsMap();

    assertThat(actualCombinedMap).containsExactlyEntriesIn(expectedCombinedMap);
  }

  @Test
  public void empty() {
    ErrorProneFlags emptyFlags = ErrorProneFlags.empty();
    assertThat(emptyFlags.isEmpty()).isTrue();
    assertThat(emptyFlags.getFlagsMap().isEmpty()).isTrue();

    ErrorProneFlags nonEmptyFlags = ErrorProneFlags.fromMap(ImmutableMap.of("a", "b"));
    assertThat(nonEmptyFlags.isEmpty()).isFalse();
    assertThat(nonEmptyFlags.getFlagsMap().isEmpty()).isFalse();
  }
}
