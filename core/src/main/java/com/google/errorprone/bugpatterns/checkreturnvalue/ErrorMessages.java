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

import static java.util.stream.Collectors.joining;

import java.util.List;

/** Error messages used by {@link com.google.errorprone.bugpatterns.CheckReturnValue}. */
public class ErrorMessages {
  private ErrorMessages() {}

  /**
   * Error message for when an annotation used by {@link Rules#mapAnnotationSimpleName} is applied
   * to a void-returning method.
   *
   * @param elementType the plural of the {@link java.lang.annotation.ElementType} of the annotated
   *     element (e.g., "methods").
   */
  public static String annotationOnVoid(String annotation, String elementType) {
    return String.format("@%s may not be applied to void-returning %s", annotation, elementType);
  }

  /**
   * Error message for when annotations mapped to conflicting {@link ResultUsePolicy}s are applied
   * to the same element.
   *
   * @param elementType the {@link java.lang.annotation.ElementType} of the annotated element (e.g.,
   *     "method" or "class").
   */
  public static String conflictingAnnotations(List<String> annotations, String elementType) {
    return annotations.stream().map(a -> "@" + a).collect(joining(" and "))
        + " cannot be applied to the same "
        + elementType;
  }

  /**
   * Error message for when
   *
   * <ol>
   *   <li>the result of a method or constructor invocation is ignored, and
   *   <li>the {@link ResultUsePolicy} of the invoked method or constructor evaluates to {@link
   *       ResultUsePolicy#EXPECTED}.
   * </ol>
   */
  public static String invocationResultIgnored(
      String shortCall, String assignmentToUnused, String apiTrailer) {
    String shortCallWithoutNew = removeNewPrefix(shortCall);
    return String.format(
        "The result of `%s` must be used\n"
            + "If you really don't want to use the result, then assign it to a variable:"
            + " `%s`.\n"
            + "\n"
            + "If callers of `%s` shouldn't be required to use its result,"
            + " then annotate it with `@CanIgnoreReturnValue`.\n"
            + "%s",
        shortCall, assignmentToUnused, shortCallWithoutNew, apiTrailer);
  }

  /**
   * Error message for when
   *
   * <ol>
   *   <li>a method or constructor is referenced in such a way that its return value would be
   *       ignored if invoked through the reference, and
   *   <li>the {@link ResultUsePolicy} of the referenced method or constructor evaluates to {@link
   *       ResultUsePolicy#EXPECTED}.
   * </ol>
   */
  public static String methodReferenceIgnoresResult(
      String shortCall,
      String methodReference,
      String implementedMethod,
      String assignmentLambda,
      String apiTrailer) {
    String shortCallWithoutNew = removeNewPrefix(shortCall);
    return String.format(
        "The result of `%s` must be used\n"
            + "`%s` acts as an implementation of `%s`"
            + " -- which is a `void` method, so it doesn't use the result of `%s`.\n"
            + "\n"
            + "To use the result, you may need to restructure your code.\n"
            + "\n"
            + "If you really don't want to use the result, then switch to a lambda that assigns"
            + " it to a variable: `%s`.\n"
            + "\n"
            + "If callers of `%s` shouldn't be required to use its result,"
            + " then annotate it with `@CanIgnoreReturnValue`.\n"
            + "%s",
        shortCall,
        methodReference,
        implementedMethod,
        shortCall,
        assignmentLambda,
        shortCallWithoutNew,
        apiTrailer);
  }

  private static String removeNewPrefix(String shortCall) {
    if (shortCall.startsWith("new ")) {
      return shortCall.substring("new ".length());
    } else {
      return shortCall;
    }
  }
}
