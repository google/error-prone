/*
 * Copyright 2017 Google Inc. All rights reserved.
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

import com.google.errorprone.apply.ImportOrganizer;

/** Parse import order strings. */
public class ImportOrderParser {

  /**
   * Parse import order string and create appropriate {@link ImportOrganizer}.
   *
   * @param importOrder the import order, either static-first or static-last.
   * @return the {@link ImportOrganizer}
   */
  public static ImportOrganizer getImportOrganizer(String importOrder) {
    switch (importOrder) {
      case "static-first":
        return ImportOrganizer.STATIC_FIRST_ORGANIZER;
      case "static-last":
        return ImportOrganizer.STATIC_LAST_ORGANIZER;
      case "android-static-first":
        return ImportOrganizer.ANDROID_STATIC_FIRST_ORGANIZER;
      case "android-static-last":
        return ImportOrganizer.ANDROID_STATIC_LAST_ORGANIZER;
      default:
        throw new IllegalStateException("Unknown import order: '" + importOrder + "'");
    }
  }
}
