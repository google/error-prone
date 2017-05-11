/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import java.util.Map;

/**
 * Test for an {@code ImportPolicy} importing nested classes directly.
 *
 * @author Louis Wasserman
 */
public class ImportClassDirectlyTemplate<K, V> {
  @BeforeTemplate
  void forEachKeys(Map<K, V> map) {
    for (K k : map.keySet()) {
      System.out.println(k + " " + map.get(k));
    }
  }

  @AfterTemplate
  @UseImportPolicy(ImportPolicy.IMPORT_CLASS_DIRECTLY)
  void forEachEntries(Map<K, V> map) {
    for (Map.Entry<K, V> entry : map.entrySet()) {
      System.out.println(entry.getKey() + " " + entry.getValue());
    }
  }
}
