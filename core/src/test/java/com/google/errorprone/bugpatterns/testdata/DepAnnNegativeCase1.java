/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

/** @deprecated */
@Deprecated
public class DepAnnNegativeCase1 {

  /** @deprecated */
  @Deprecated
  public DepAnnNegativeCase1() {}

  /** @deprecated */
  @Deprecated int myField;

  /** @deprecated */
  @Deprecated
  enum Enum {
    VALUE,
  }

  /** @deprecated */
  @Deprecated
  interface Interface {}

  /** @deprecated */
  @Deprecated
  public void deprecatedMethood() {}

  @Deprecated
  public void deprecatedMethoodWithoutComment() {}

  /** deprecated */
  public void deprecatedMethodWithMalformedComment() {}

  /** @deprecated */
  @SuppressWarnings("dep-ann")
  public void suppressed() {}

  public void newMethod() {}
}
