/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Api}. */
@RunWith(JUnit4.class)
public final class ApiTest {

  private static final ImmutableSet<String> UNPARSEABLE_APIS =
      ImmutableSet.of(
          "",
          "#()",
          "#(java.lang.String)",
          "#foo()",
          "#foo(java.lang.String)",
          "java.lang.String",
          "java.lang.String##foo()",
          "java.lang.String#fo#o()",
          "java.lang.String#()",
          "java.lang.String#foo)()",
          "java.lang.String#foo)(",
          "java.lang.String#<init>(,)",
          "java.lang.String#get(int[][)",
          "java.lang.String#get(int[[])",
          "java.lang.String#get(int[]])",
          "java.lang.String#get(int])",
          "java.lang.String#get(int[)",
          "java.lang.String#get(int[]a)",
          "java.lang.String#<>()",
          "java.lang.String#hi<>()",
          "java.lang.String#<>hi()",
          "java.lang.String#hi<init>()",
          "java.lang.String#<init>hi()",
          "java.lang.String#<iniT>()",
          "java.lang.String#<init>((java.lang.String)",
          "java.lang.String#<init>(java.lang.String",
          "java.lang.String#<init>(java.lang.String,)",
          "java.lang.String#<init>(,java.lang.String)",
          "java.lang.String#<init>(java.lang.String))");

  @Test
  public void parseApi_badInputs() {
    // TODO(b/223670489): would be nice to use expectThrows() here
    for (String badApi : UNPARSEABLE_APIS) {
      assertThrows(
          "Api.parse(\"" + badApi + "\")", IllegalArgumentException.class, () -> Api.parse(badApi));
    }
  }

  @Test
  public void parseApi_constructorWithoutParams() {
    String string = "com.google.async.promisegraph.testing.TestPromiseGraphModule#<init>()";
    Api api = Api.parse(string);
    assertThat(api.className())
        .isEqualTo("com.google.async.promisegraph.testing.TestPromiseGraphModule");
    assertThat(api.methodName()).isEqualTo("<init>");
    assertThat(api.parameterTypes()).isEmpty();
    assertThat(api.isConstructor()).isTrue();
    assertThat(api.toString()).isEqualTo(string);
  }

  @Test
  public void parseApi_constructorWithParams() {
    String string = "com.google.api.client.http.GenericUrl#<init>(java.lang.String)";
    Api api = Api.parse(string);
    assertThat(api.className()).isEqualTo("com.google.api.client.http.GenericUrl");
    assertThat(api.methodName()).isEqualTo("<init>");
    assertThat(api.parameterTypes()).containsExactly("java.lang.String").inOrder();
    assertThat(api.isConstructor()).isTrue();
    assertThat(api.toString()).isEqualTo(string);
  }

  @Test
  public void parseApi_methodWithoutParams() {
    String string = "com.google.api.services.drive.model.File#getId()";
    Api api = Api.parse(string);
    assertThat(api.className()).isEqualTo("com.google.api.services.drive.model.File");
    assertThat(api.methodName()).isEqualTo("getId");
    assertThat(api.parameterTypes()).isEmpty();
    assertThat(api.isConstructor()).isFalse();
    assertThat(api.toString()).isEqualTo(string);
  }

  @Test
  public void parseApi_methodWithParamsAndSpaces() {
    String string =
        "  com.google.android.libraries.stitch.binder.Binder"
            + "#get(android.content.Context , java.lang.Class) ";
    Api api = Api.parse(string);
    assertThat(api.methodName()).isEqualTo("get");
    assertThat(api.parameterTypes())
        .containsExactly("android.content.Context", "java.lang.Class")
        .inOrder();
    assertThat(api.isConstructor()).isFalse();
    assertThat(api.toString()).isEqualTo(whitespace().removeFrom(string));
  }

  @Test
  public void parseApi_methodWithArray() {
    String string =
        "com.google.inject.util.Modules.OverriddenModuleBuilder#with(com.google.inject.Module[],int[][][])";
    Api api = Api.parse(string);
    assertThat(api.className()).isEqualTo("com.google.inject.util.Modules.OverriddenModuleBuilder");
    assertThat(api.methodName()).isEqualTo("with");
    assertThat(api.parameterTypes())
        .containsExactly("com.google.inject.Module[]", "int[][][]")
        .inOrder();
    assertThat(api.isConstructor()).isFalse();
    assertThat(api.toString()).isEqualTo(string);
  }

  @Test
  public void parseApi_methodWithVarargs_b231250004() {
    String string = "com.beust.jcommander.JCommander#<init>(java.lang.Object,java.lang.String...)";
    Api api = Api.parse(string);
    assertThat(api.className()).isEqualTo("com.beust.jcommander.JCommander");
    assertThat(api.methodName()).isEqualTo("<init>");
    assertThat(api.parameterTypes())
        .containsExactly("java.lang.Object", "java.lang.String...")
        .inOrder();
    assertThat(api.isConstructor()).isTrue();
    assertThat(api.toString()).isEqualTo(string);
  }
}
