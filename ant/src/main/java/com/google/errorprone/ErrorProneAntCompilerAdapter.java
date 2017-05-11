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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.internal.NonDelegatingClassLoader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;

/**
 * Adapts the error-prone compiler to be used in an Ant build.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneAntCompilerAdapter extends DefaultCompilerAdapter {
  public static class AntRunner implements Function<String[], Boolean> {
    @Override
    public Boolean apply(String[] args) {
      return ErrorProneCompiler.compile(args).isOK();
    }
  }

  @Override
  public boolean execute() throws BuildException {
    ClassLoader originalLoader = ErrorProneCompiler.class.getClassLoader();
    URL[] urls;
    if (originalLoader instanceof URLClassLoader) {
      urls = ((URLClassLoader) originalLoader).getURLs();
    } else if (originalLoader instanceof AntClassLoader) {
      String[] pieces = ((AntClassLoader) originalLoader).getClasspath().split(":");
      urls = new URL[pieces.length];
      for (int i = 0; i < pieces.length; ++i) {
        try {
          urls[i] = Paths.get(pieces[i]).toUri().toURL();
        } catch (MalformedURLException e) {
          throw new BuildException(e);
        }
      }
    } else {
      throw new BuildException("Unexpected ClassLoader: " + originalLoader.getClass());
    }

    ClassLoader loader =
        NonDelegatingClassLoader.create(
            ImmutableSet.<String>of(Function.class.getName()), urls, originalLoader);

    String[] args = setupModernJavacCommand().getArguments();

    try {
      Class<?> runnerClass = Class.forName(AntRunner.class.getName(), true, loader);
      @SuppressWarnings("unchecked")
      Function<String[], Boolean> runner = (Function<String[], Boolean>) runnerClass.newInstance();
      return runner.apply(args);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError("Unable to create runner.", e);
    }
  }
}
