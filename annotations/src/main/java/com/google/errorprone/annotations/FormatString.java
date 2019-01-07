/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.annotations;

import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for method parameter declarations which denotes that actual parameters will be used as
 * a format string in printf-style formatting.
 *
 * <p>This is an optional annotation used along with the {@link FormatMethod} annotation to denote
 * which parameter in a format method is the format string. All parameters after the format string
 * are assumed to be printf-style arguments for the format string. For example, the following
 * snippet declares that {@code logMessage} will be used as a format string with {@code args} passed
 * as arguments to the format string:
 *
 * <pre>
 * public class Foo {
 *   &#064;FormatMethod void doBarAndLogFailure(&#064;FormatString String logMessage,
 *       Object... args) {...}
 * }</pre>
 *
 * <p>See {@link FormatMethod} for more information.
 */
@Documented
@Retention(CLASS)
@Target({ElementType.PARAMETER})
public @interface FormatString {}
