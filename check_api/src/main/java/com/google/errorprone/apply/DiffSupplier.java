/*
 * Copyright 2011 Google Inc. All rights reserved.
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

import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Supplier of file differences.
 *
 * <p>This can be a data source (e.g. an already computed diff file) or it can produce diffs on the
 * fly by reading files from a {@link FileSource} and processing each one by one.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public interface DiffSupplier {

  /**
   * Gets the list of differences
   *
   * @param fileSource the source of source files
   * @param fileNames an optional list of filenames to restrict to. If null, there is no restriction
   *     on file names. This will make more sense for some diff suppliers than others.)
   * @return the diffs
   * @throws IOException if there is an I/O problem while generating the diffs
   */
  Iterable<Diff> getDiffs(FileSource fileSource, @Nullable String[] fileNames) throws IOException;
}
