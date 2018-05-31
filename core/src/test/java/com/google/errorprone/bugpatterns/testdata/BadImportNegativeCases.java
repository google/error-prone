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
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;

/**
 * Tests for {@link BadImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
public class BadImportNegativeCases {
  public void qualified() {
    ImmutableList.Builder<String> qualified;
    com.google.common.collect.ImmutableList.Builder<String> fullyQualified;
    ImmutableList.Builder raw;

    new ImmutableList.Builder<String>();
  }

  static class Nested {
    static class Builder {}

    void useNestedBuilder() {
      new Builder();
    }
  }
}
