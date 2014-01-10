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

package com.google.errorprone;

/**
 * Validates an {@code @BugPattern} annotation for wellformedness.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class BugPatternValidator {

  public static void validate(BugPattern pattern) throws ValidationException {
    if (pattern == null) {
      throw new ValidationException("No @BugPattern provided");
    }

    // linkType must be consistent with link element.
    switch (pattern.linkType()) {
      case CUSTOM:
        if (pattern.link().isEmpty()) {
          throw new ValidationException("Expected a custom link but none was provided");
        }
        break;
      case WIKI: case NONE:
        if (!pattern.link().isEmpty()) {
          throw new ValidationException("Expected no custom link but found: " + pattern.link());
        }
        break;
    }

    // suppressibility must be consistent with customAnnotationType.
    switch (pattern.suppressibility()) {
      case CUSTOM_ANNOTATION:
        if (pattern.customSuppressionAnnotation() == BugPattern.NoCustomSuppression.class) {
          throw new ValidationException("Expected a custom suppression annotation but none was "
              + "provided");
        }
        if (pattern.customSuppressionAnnotation() == SuppressWarnings.class) {
          throw new ValidationException("Custom suppression annotation may not use "
              + "@SuppressWarnings");
        }
        break;
      case SUPPRESS_WARNINGS: case UNSUPPRESSIBLE:
        if (pattern.customSuppressionAnnotation() != BugPattern.NoCustomSuppression.class) {
          throw new ValidationException("Expected no custom suppression annotation but found one "
              + "of type: " + pattern.customSuppressionAnnotation().getCanonicalName());
        }
        break;
    }
  }
}
