/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.auto.value.AutoValue;

/**
 * Flags that control the behavior of threadsafety utils to facilitate rolling out new
 * functionality.
 *
 * <p>This has no flags for now, but is still plumbed through to make it easier to flag guard
 * changes to {@link GuardedByChecker} in the future. Otherwise, it's rather difficult.
 */
@AutoValue
public abstract class GuardedByFlags {
  public static GuardedByFlags allOn() {
    return new AutoValue_GuardedByFlags();
  }
}
