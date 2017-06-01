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

/**
 * All the differences to be applied to a source file to be applied in a refactoring.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public interface Diff {
  /** Gets the name of the file this difference applies to */
  public String getRelevantFileName();

  /**
   * Applies this difference to the supplied {@code sourceFile}.
   *
   * @throws DiffNotApplicableException if the diff could not be applied to the source file
   */
  public void applyDifferences(SourceFile sourceFile) throws DiffNotApplicableException;
}
