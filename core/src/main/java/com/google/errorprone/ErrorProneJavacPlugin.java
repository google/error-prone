/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.auto.service.AutoService;
import com.google.errorprone.BaseErrorProneJavaCompiler.RefactoringTask;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;

/** A javac {@link Plugin} that runs Error Prone. */
@AutoService(Plugin.class)
public class ErrorProneJavacPlugin implements Plugin {
  @Override
  public String getName() {
    return "ErrorProne";
  }

  @Override
  public void init(JavacTask javacTask, String... args) {
    Context context = ((BasicJavacTask) javacTask).getContext();
    BaseErrorProneJavaCompiler.setupMessageBundle(context);
    RefactoringCollection[] refactoringCollection = {null};
    javacTask.addTaskListener(
        BaseErrorProneJavaCompiler.createAnalyzer(
            BuiltInCheckerSuppliers.defaultChecks(),
            ErrorProneOptions.processArgs(args),
            context,
            refactoringCollection));
    if (refactoringCollection[0] != null) {
      javacTask.addTaskListener(new RefactoringTask(context, refactoringCollection[0]));
    }
  }
}
