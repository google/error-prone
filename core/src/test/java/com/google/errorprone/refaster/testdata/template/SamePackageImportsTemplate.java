/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.refaster.testdata.template;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;

/** Sample template generating references to nested classes in java.util. */
public class SamePackageImportsTemplate<K, V> {
  @BeforeTemplate
  ImmutableMap.Builder<K, V> builderFactory() {
    return ImmutableMap.builder();
  }

  @AfterTemplate
  @UseImportPolicy(ImportPolicy.IMPORT_CLASS_DIRECTLY)
  ImmutableMap.Builder<K, V> builderConstructor() {
    return new ImmutableMap.Builder<>();
  }
}
