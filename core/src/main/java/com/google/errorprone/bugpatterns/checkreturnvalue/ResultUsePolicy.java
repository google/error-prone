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

/** Policy for use of a method or constructor's result. */
public enum ResultUsePolicy {
  /**
   * Use of the result is expected except in certain contexts where the method is being used in a
   * way such that not using the result is likely correct. Examples include when the result type at
   * the callsite is {@code java.lang.Void} and when the surrounding context seems to be testing
   * that the method throws an exception.
   */
  EXPECTED,
  /** Use of the result is optional. */
  OPTIONAL,
  /** It is unspecified whether the result should be used or not. */
  UNSPECIFIED,
}
