/*
 * Copyright 2016 Google Inc. All rights reserved.
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

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.service.AutoService;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A javac plugin that compiles Refaster rules to a {@code .analyzer} file.
 *
 * @author lowasser@google.com
 */
@AutoService(Plugin.class)
public class RefasterRuleCompiler implements Plugin {
  @Override
  public String getName() {
    return "RefasterRuleCompiler";
  }

  @Override
  public void init(JavacTask javacTask, String... args) {
    Iterator<String> itr = Arrays.asList(args).iterator();
    String path = null;
    while (itr.hasNext()) {
      if (itr.next().equals("--out")) {
        path = itr.next();
        break;
      }
    }
    checkArgument(path != null, "No --out specified");

    javacTask.addTaskListener(
        new RefasterRuleCompilerAnalyzer(
            ((BasicJavacTask) javacTask).getContext(), FileSystems.getDefault().getPath(path)));
  }
}
