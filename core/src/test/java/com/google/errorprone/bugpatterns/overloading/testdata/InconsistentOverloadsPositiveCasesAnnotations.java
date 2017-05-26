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

package com.google.errorprone.bugpatterns.overloading.testdata;

import javax.annotation.Nullable;

public abstract class InconsistentOverloadsPositiveCasesAnnotations {

  @interface Bar {}

  @interface Baz {}

  // BUG: Diagnostic contains: foo(String x, String y, Object z)
  abstract void foo(@Nullable Object z, String y, @Nullable String x);

  abstract void foo(@Nullable String x);

  // BUG: Diagnostic contains: foo(String x, String y)
  abstract void foo(String y, @Nullable String x);

  // BUG: Diagnostic contains: quux(Object object, String string)
  int quux(String string, @Bar @Baz Object object) {
    return string.hashCode() + quux(object);
  }

  int quux(@Bar @Baz Object object) {
    return object.hashCode();
  }

  // BUG: Diagnostic contains: quux(Object object, String string, int x, int y)
  abstract int quux(String string, int x, int y, @Bar @Baz Object object);

  abstract int norf(@Bar @Baz String string);

  // BUG: Diagnostic contains: norf(String string, Object object)
  abstract int norf(Object object, @Baz @Bar String string);
}
