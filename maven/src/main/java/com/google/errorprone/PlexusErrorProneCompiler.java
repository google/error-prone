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

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PlexusErrorProneCompiler extends JavacCompiler {
  @Override
  public List compile( CompilerConfiguration config )
      throws CompilerException {
    File destinationDir = new File(config.getOutputLocation() );

    if (!destinationDir.exists()) {
      destinationDir.mkdirs();
    }

    String[] sourceFiles = getSourceFiles(config);

    if ((sourceFiles == null) || ( sourceFiles.length == 0 )) {
      return Collections.EMPTY_LIST;
    }

    if ( ( getLogger() != null ) && getLogger().isInfoEnabled() )
    {
      getLogger().info( "Compiling " + sourceFiles.length + " " +
          "source file" + ( sourceFiles.length == 1 ? "" : "s" ) +
          " to " + destinationDir.getAbsolutePath() );
    }

    String[] args = buildCompilerArguments( config, sourceFiles );

    if (config.isFork()) {
      throw new UnsupportedOperationException(
          "error-prone compiler can only be run in-process");
    } else {
      new ErrorProneCompiler.Builder().build().compile(args);

      return Collections.emptyList();
    }
  }
}
