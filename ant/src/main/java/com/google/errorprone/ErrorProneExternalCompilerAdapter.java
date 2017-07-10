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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.LoaderUtils;

/** Ant component to launch an external javac with error-prone enabled. */
public class ErrorProneExternalCompilerAdapter extends DefaultCompilerAdapter {
  private Path classpath;
  private String memoryStackSize;
  private List<Argument> jvmArgs = new ArrayList<>();

  public void setClasspath(Path classpath) {
    this.classpath = classpath;
  }

  public Path createClasspath() {
    if (classpath == null) {
      classpath = new Path(getProject());
    }

    return classpath.createPath();
  }

  public void setClasspathRef(Reference r) {
    createClasspath().setRefid(r);
  }

  public void setMemoryStackSize(String memoryStackSize) {
    this.memoryStackSize = memoryStackSize;
  }

  public Argument createJvmArg() {
    Argument arg = new Argument();
    jvmArgs.add(arg);
    return arg;
  }

  @Override
  public boolean execute() throws BuildException {
    if (getJavac().isForkedJavac()) {
      attributes.log("Using external Error Prone compiler", Project.MSG_VERBOSE);
      Commandline cmd = new Commandline();
      cmd.setExecutable(JavaEnvUtils.getJdkExecutable("java"));
      if (memoryStackSize != null) {
        cmd.createArgument().setValue("-Xss" + memoryStackSize);
      }
      String memoryParameterPrefix = "-X";
      if (memoryInitialSize != null) {
        cmd.createArgument().setValue(memoryParameterPrefix + "ms" + this.memoryInitialSize);
        // Prevent setupModernJavacCommandlineSwitches() from doing it also
        memoryInitialSize = null;
      }
      if (memoryMaximumSize != null) {
        cmd.createArgument().setValue(memoryParameterPrefix + "mx" + this.memoryMaximumSize);
        // Prevent setupModernJavacCommandlineSwitches() from doing it also
        memoryMaximumSize = null;
      }
      for (Argument arg : jvmArgs) {
        for (String part : arg.getParts()) {
          cmd.createArgument().setValue(part);
        }
      }

      Path bootclasspath = new Path(getProject());
      addResourceSource(
          bootclasspath, "com/google/errorprone/ErrorProneExternalCompilerAdapter.class");
      cmd.createArgument().setValue("-Xbootclasspath/p:" + bootclasspath);

      if (classpath != null) {
        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);
      }

      cmd.createArgument().setValue(ErrorProneCompiler.class.getName());
      // This is the first argument that may be spilled to a file.
      // The ant API describes it as the first argument which is a
      // file, but in fact only uses it for spilling.  Putting the
      // break here allows long classpath arguments to be handled.
      int firstSpillableArgument = cmd.size();
      setupModernJavacCommandlineSwitches(cmd);
      logAndAddFilesToCompile(cmd);
      return executeExternalCompile(cmd.getCommandline(), firstSpillableArgument, true) == 0;
    } else {
      attributes.log(
          "You must set fork=\"yes\" to use the external Error Prone compiler", Project.MSG_ERR);
      return false;
    }
  }

  private void addResourceSource(Path classpath, String resource) {
    final File f =
        LoaderUtils.getResourceSource(
            ErrorProneExternalCompilerAdapter.class.getClassLoader(), resource);
    if (f != null) {
      attributes.log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
      classpath.createPath().setLocation(f);
    } else {
      attributes.log("Couldn't find " + resource, Project.MSG_DEBUG);
    }
  }
}
