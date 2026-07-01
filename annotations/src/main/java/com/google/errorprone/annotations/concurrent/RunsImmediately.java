/*
 * Copyright 2026 The Error Prone Authors.
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
package com.google.errorprone.annotations.concurrent;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated functional-interface parameter is run synchronously on the calling
 * thread before the method returns, and is not stored or passed to another thread.
 *
 * <p>The {@link GuardedBy} check relies on this to analyze a lambda or method reference passed as
 * the argument in the caller's lock scope.
 */
@Target(PARAMETER)
@Retention(CLASS)
public @interface RunsImmediately {}
