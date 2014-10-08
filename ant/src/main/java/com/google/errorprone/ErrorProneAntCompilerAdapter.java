/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;

/**
 * Adapts the error-prone compiler to be used in an Ant build.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneAntCompilerAdapter extends DefaultCompilerAdapter {
  @Override
  public boolean execute() throws BuildException {
    String[] args = setupModernJavacCommand().getArguments();
    return new ErrorProneCompiler.Builder().build().compile(args).isOK();
  }
}
