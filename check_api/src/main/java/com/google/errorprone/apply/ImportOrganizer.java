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
package com.google.errorprone.apply;

/** Organizes import statements when patching files. */
public interface ImportOrganizer {

  /**
   * Organize the imports supplied, e.g. insert blank lines between various groups.
   *
   * @param importStrings the imports to organize, the order is undefined. Each string is of the
   *     format {@code import( static)? <identifier>}.
   * @return the list of organized imports, an empty string represents a blank line.
   */
  Iterable<String> organizeImports(Iterable<String> importStrings);

  /**
   * An {@link ImportOrganizer} that sorts import statements according to the Google Java Style
   * Guide, i.e. static first, static and non-static separated by blank line.
   */
  ImportOrganizer STATIC_FIRST_ORGANIZER =
      new BasicImportOrganizer(BasicImportOrganizer.staticFirst());

  /**
   * An {@link ImportOrganizer} that sorts import statements so that non-static imports come first,
   * and static and non-static separated by blank line.
   */
  ImportOrganizer STATIC_LAST_ORGANIZER =
      new BasicImportOrganizer(BasicImportOrganizer.staticLast());
}
