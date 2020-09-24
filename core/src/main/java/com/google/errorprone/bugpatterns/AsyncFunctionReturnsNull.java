/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.errorprone.BugPattern;

/** Checks that {@link AsyncFunction} implementations do not directly {@code return null}. */
@BugPattern(
    name = "AsyncFunctionReturnsNull",
    summary = "AsyncFunction should not return a null Future, only a Future whose result is null.",
    severity = ERROR)
public final class AsyncFunctionReturnsNull extends AbstractAsyncTypeReturnsNull {
  public AsyncFunctionReturnsNull() {
    super(AsyncFunction.class);
  }
}
