/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.analysis;

import com.google.errorprone.DescriptionListener;
import com.google.errorprone.matchers.Suppressible;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

/**
 * A non-recursive analysis analyzing the specific AST node argument and reporting zero or more
 * analysis results to a {@code DescriptionListener}.
 *
 * @author Louis Wasserman
 */
public interface LocalAnalysis extends Suppressible {  
  void analyze(TreePath tree, Context context, AnalysesConfig configuration,
      DescriptionListener listener);
}
