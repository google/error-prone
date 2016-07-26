/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.api.JavacTool;
import javax.tools.JavaCompiler;

/**
 * An Error Prone compiler that implements {@link javax.tools.JavaCompiler}.
 *
 * <p>Runs all built-in checks by default.
 *
 * @author nik
 */
public class ErrorProneJavaCompiler extends BaseErrorProneJavaCompiler {

  public ErrorProneJavaCompiler() {
    this(JavacTool.create());
  }

  ErrorProneJavaCompiler(JavaCompiler javacTool) {
    super(javacTool, BuiltInCheckerSuppliers.defaultChecks());
  }

  public ErrorProneJavaCompiler(ScannerSupplier scannerSupplier) {
    super(JavacTool.create(), scannerSupplier);
  }
}
