/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow;

import org.checkerframework.dataflow.cfg.node.LocalVariableNode;

/** Read-only access to {@link LocalStore} for convenience. */
public interface LocalVariableValues<T> {
  /**
   * Provides the nullness values of local variables (as far as they can be determined). If the
   * nullness value cannot be definitively determined (for example, because the variable is a
   * parameter with no assignments within the method and wasn't given an explicit initial value),
   * {@code defaultValue} will be returned.
   */
  T valueOfLocalVariable(LocalVariableNode node, T defaultValue);
}
