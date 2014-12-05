package com.google.errorprone;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.LoaderUtils;

/**
 * Ant component to launch an external javac with error-prone enabled.
 */
public class ErrorProneExternalCompilerAdapter extends DefaultCompilerAdapter {
  private Path classpath;
  private boolean suggestFixes;

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

  public void setSuggestFixes(boolean suggestFixes) {
    this.suggestFixes = suggestFixes;
  }

  @Override
  public boolean execute() throws BuildException {
    if (getJavac().isForkedJavac()) {
      attributes.log("Using external error-prone compiler", Project.MSG_VERBOSE);
      Commandline cmd = new Commandline();
      cmd.setExecutable(JavaEnvUtils.getJdkExecutable("java"));

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

      cmd.createArgument().setValue("-classpath");
      if (classpath == null) {
        classpath = new Path(getProject());
      }
      // Usually redundant, but check two resources in case Ant stuff is in a different jar
      addResourceSource(classpath, "com/google/errorprone/ErrorProneExternalCompilerAdapter.class");
      addResourceSource(classpath, "com/google/errorprone/ErrorProneCompiler.class");
      addResourceSource(classpath, "com/sun/tools/javac/Main.class");
      cmd.createArgument().setPath(classpath);
      cmd.createArgument().setValue(ErrorProneCompiler.class.getName());
      if (suggestFixes) {
        cmd.createArgument().setValue("-Xjcov");
      }
      setupModernJavacCommandlineSwitches(cmd);
      logAndAddFilesToCompile(cmd);
      return executeExternalCompile(cmd.getCommandline(), cmd.size(), true) == 0;
    } else {
      attributes.log("You must set fork=\"yes\" to use the external error-prone compiler", Project.MSG_ERR);
      return false;
    }
  }

  private void addResourceSource(Path classpath, String resource) {
    final File f = LoaderUtils.getResourceSource(ErrorProneExternalCompilerAdapter.class.getClassLoader(), resource);
    if (f != null) {
      attributes.log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
      classpath.createPath().setLocation(f);
    } else {
      attributes.log("Couldn't find " + resource, Project.MSG_DEBUG);
    }
  }
}
