/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.base.Ascii.toLowerCase;
import static com.google.common.base.Ascii.toUpperCase;
import static java.lang.Character.isDigit;

import com.google.errorprone.ErrorProneFlags;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

/** Utilities for dealing with identifier names. */
final class IdentifierNames {

  private final boolean allowInitialismsInTypeName;

  @Inject
  IdentifierNames(ErrorProneFlags flags) {
    this.allowInitialismsInTypeName =
        flags.getBoolean("IdentifierName:AllowInitialismsInTypeName").orElse(false);
  }

  static boolean isConformantLowerCamelName(String name) {
    return underscoresAreFlankedByDigits(name)
        && !isUpperCase(name.charAt(0))
        && !PROBABLE_INITIALISM.matcher(name).find();
  }

  boolean isConformantTypeName(String name) {
    return underscoresAreFlankedByDigits(name)
        && isUpperCase(name.charAt(0))
        && (allowInitialismsInTypeName || !PROBABLE_INITIALISM.matcher(name).find());
  }

  private static boolean underscoresAreFlankedByDigits(String name) {
    if (name.startsWith("_") || name.endsWith("_")) {
      return false;
    }
    for (int i = 1; i < name.length() - 1; i++) {
      if (name.charAt(i) == '_') {
        boolean flankedByDigits = isDigit(name.charAt(i - 1)) && isDigit(name.charAt(i + 1));
        if (!flankedByDigits) {
          return false;
        }
      }
    }
    return true;
  }

  String fixInitialismsIfNeeded(String input) {
    return allowInitialismsInTypeName ? input : fixInitialisms(input);
  }

  static String fixInitialisms(String input) {
    return PROBABLE_INITIALISM
        .matcher(input)
        .replaceAll(r -> Matcher.quoteReplacement(titleCase(r.group(1)) + r.group(2)));
  }

  private static String titleCase(String input) {
    String lower = toLowerCase(input);
    return toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  private static final Pattern PROBABLE_INITIALISM = Pattern.compile("([A-Z]{2,})([A-Z][^A-Z]|$)");
}
