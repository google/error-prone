/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.errorprone;

import com.google.errorprone.apply.CustomImportOrganizer;
import com.google.errorprone.apply.ImportOrganizer;

/** Parse import order strings. */
public final class ImportOrderParser {

  /**
   * Parse import order string and create appropriate {@link ImportOrganizer}.
   *
   * @param importOrder the import order, either static-first or static-last.
   * @return the {@link ImportOrganizer}
   */
  public static ImportOrganizer getImportOrganizer(String importOrder) {
    return switch (importOrder) {
      case "static-first" -> ImportOrganizer.STATIC_FIRST_ORGANIZER;
      case "static-last" -> ImportOrganizer.STATIC_LAST_ORGANIZER;
      case "android-static-first" -> ImportOrganizer.ANDROID_STATIC_FIRST_ORGANIZER;
      case "android-static-last" -> ImportOrganizer.ANDROID_STATIC_LAST_ORGANIZER;
      case "idea" -> ImportOrganizer.IDEA_ORGANIZER;
      default -> {
        if (importOrder.startsWith("custom:"))
          yield new CustomImportOrganizer(importOrder.substring(7));
        throw new IllegalStateException("Unknown import order: '" + importOrder + "'");
      }
    };
  }

  private ImportOrderParser() {}
}
