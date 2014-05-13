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

package com.google.errorprone.intellij;

import com.google.errorprone.matchers.Matcher;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.OutputParser;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.compiler.impl.javaCompiler.ExternalCompiler;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfigurable;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration;
import com.intellij.compiler.impl.javaCompiler.javac.JavacOutputParser;
import com.intellij.compiler.impl.javaCompiler.javac.JavacSettingsBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.MockJdkWrapper;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ErrorProneIdeaCompiler extends ExternalCompiler {

  private final Project myProject;
  private final List<File> myTempFiles = new ArrayList<File>();
  @NonNls
  private boolean myAnnotationProcessorMode = false;

  public ErrorProneIdeaCompiler(Project project) {
    myProject = project;
  }

  public boolean checkCompiler(final CompileScope scope) {
    final Module[] modules = scope.getAffectedModules();
    final Set<Sdk> checkedJdks = new HashSet<Sdk>();
    for (final Module module : modules) {
      final Sdk jdk = ModuleRootManager.getInstance(module).getSdk();
      if (jdk == null || checkedJdks.contains(jdk)) {
        continue;
      }
      checkedJdks.add(jdk);
      final SdkTypeId sdkType = jdk.getSdkType();
      if (!(sdkType instanceof JavaSdkType)) {
        continue;
      }
      final VirtualFile homeDirectory = jdk.getHomeDirectory();
      if (homeDirectory == null) {
        //noinspection DialogTitleCapitalization
        Messages.showMessageDialog(
            myProject,
            ErrorProneIdeaBundle.jdkHomeNotFoundMessage(jdk),
            ErrorProneIdeaBundle.compilerName(),
            Messages.getErrorIcon()
        );
        return false;
      }
      final String vmExecutablePath = ((JavaSdkType)sdkType).getVMExecutablePath(jdk);
      if (vmExecutablePath == null) {
        Messages.showMessageDialog(
            myProject,
            ErrorProneIdeaBundle.message("error-prone.error.vm.executable.missing", jdk.getName()),
            ErrorProneIdeaBundle.compilerName(),
            Messages.getErrorIcon()
        );
        return false;
      }
      final String toolsJarPath = ((JavaSdkType)sdkType).getToolsPath(jdk);
      if (toolsJarPath == null) {
        Messages.showMessageDialog(
            myProject,
            ErrorProneIdeaBundle.message("error-prone.error.tools.jar.missing", jdk.getName()),
            ErrorProneIdeaBundle.compilerName(),
            Messages.getErrorIcon()
        );
        return false;
      }
      final String versionString = jdk.getVersionString();
      if (versionString == null) {
        Messages.showMessageDialog(
            myProject,
            ErrorProneIdeaBundle.message("error-prone.error.unknown.jdk.version", jdk.getName()),
            ErrorProneIdeaBundle.compilerName(),
            Messages.getErrorIcon()
        );
        return false;
      }

      if (CompilerUtil.isOfVersion(versionString, "1.0")) {
        Messages.showMessageDialog(
            myProject,
            ErrorProneIdeaBundle.message("error-prone.error.1_0_compilation.not.supported"),
            ErrorProneIdeaBundle.compilerName(),
            Messages.getErrorIcon()
        );
        return false;
      }
    }

    return true;
  }

  @NotNull
  @NonNls
  public String getId() { // used for externalization
    return "Javac (with error-prone)";
  }

  @NotNull
  public String getPresentableName() {
    return ErrorProneIdeaBundle.compilerName();
  }

  @NotNull
  public Configurable createConfigurable() {
    return new JavacConfigurable(JavacConfiguration.getOptions(myProject, JavacConfiguration.class));
  }

  public OutputParser createErrorParser(@NotNull final String outputDir, Process process) {
    return new JavacOutputParser(myProject);
  }

  public OutputParser createOutputParser(@NotNull final String outputDir) {
    return null;
  }

  private static class MyException extends RuntimeException {
    private MyException(Throwable cause) {
      super(cause);
    }
  }

  @NotNull
  public String[] createStartupCommand(final ModuleChunk chunk, final CompileContext context, final String outputPath)
      throws IOException, IllegalArgumentException {

    try {
      return ApplicationManager.getApplication().runReadAction(new Computable<String[]>() {
        public String[] compute() {
          try {
            final List<String> commandLine = new ArrayList<String>();
            createStartupCommand(chunk, commandLine, outputPath, JavacConfiguration.getOptions(myProject, JavacConfiguration.class), context.isAnnotationProcessorsEnabled());
            System.out.println("Called as \n  " + StringUtil.join(commandLine, " "));
            return ArrayUtil.toStringArray(commandLine);
          }
          catch (IOException e) {
            throw new MyException(e);
          }
        }
      });
    }
    catch (MyException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException)cause;
      }
      throw e;
    }
  }

  private void createStartupCommand(final ModuleChunk chunk, @NonNls final List<String> commandLine, final String outputPath,
                                    JpsJavaCompilerOptions javacOptions, final boolean annotationProcessorsEnabled) throws IOException {
    final Sdk jdk = getJdkForStartupCommand(chunk);
    final String versionString = jdk.getVersionString();
    JavaSdkVersion version = JavaSdk.getInstance().getVersion(jdk);
    if (versionString == null || version == null || !(jdk.getSdkType() instanceof JavaSdkType)) {
      throw new IllegalArgumentException(CompilerBundle.message("javac.error.unknown.jdk.version", jdk.getName()));
    }

    JavaSdkType sdkType = (JavaSdkType)jdk.getSdkType();

    final String toolsJarPath = sdkType.getToolsPath(jdk);
    if (toolsJarPath == null) {
      throw new IllegalArgumentException(CompilerBundle.message("javac.error.tools.jar.missing", jdk.getName()));
    }

    final String vmExePath = sdkType.getVMExecutablePath(jdk);

    commandLine.add(vmExePath);

    commandLine.add("-Xmx" + javacOptions.MAXIMUM_HEAP_SIZE + "m");

    final List<String> additionalOptions =
      addAdditionalSettings(commandLine, javacOptions, myAnnotationProcessorMode, version, chunk, annotationProcessorsEnabled);

    CompilerUtil.addLocaleOptions(commandLine, false);

    commandLine.add("-classpath");

    commandLine.add(sdkType.getToolsPath(jdk) + File.pathSeparator + PathUtil.getJarPathForClass(Matcher.class));

    commandLine.add("com.google.errorprone.ErrorProneCompiler");

    addCommandLineOptions(chunk, commandLine, outputPath, jdk, myAnnotationProcessorMode);

    commandLine.addAll(additionalOptions);

    final List<VirtualFile> files = chunk.getFilesToCompile();

    for (final VirtualFile file : files) {
      commandLine.add(file.getPath());
    }
  }

  public static List<String> addAdditionalSettings(List<String> commandLine, JpsJavaCompilerOptions javacOptions, boolean isAnnotationProcessing,
                                                   JavaSdkVersion version, ModuleChunk chunk, boolean annotationProcessorsEnabled) {
    final List<String> additionalOptions = new ArrayList<String>();
      StringTokenizer tokenizer = new StringTokenizer(new JavacSettingsBuilder(javacOptions).getOptionsString(chunk), " ");
    if (isAnnotationProcessing) {
      final AnnotationProcessingConfiguration config = CompilerConfiguration.getInstance(chunk.getProject()).getAnnotationProcessingConfiguration(chunk.getModules()[0]);
      additionalOptions.add("-Xprefer:source");
      additionalOptions.add("-implicit:none");
      additionalOptions.add("-proc:only");
      if (!config.isObtainProcessorsFromClasspath()) {
        final String processorPath = config.getProcessorPath();
        additionalOptions.add("-processorpath");
        additionalOptions.add(FileUtil.toSystemDependentName(processorPath));
      }
      final Set<String> processors = config.getProcessors();
      if (!processors.isEmpty()) {
        additionalOptions.add("-processor");
        additionalOptions.add(StringUtil.join(processors, ","));
      }
      for (Map.Entry<String, String> entry : config.getProcessorOptions().entrySet()) {
        additionalOptions.add("-A" + entry.getKey() + "=" +entry.getValue());
      }
    }
    else {
      if (annotationProcessorsEnabled) {
        // Unless explicitly specified by user, disable annotation processing by default for 'java compilation' mode
        // This is needed to suppress unwanted side-effects from auto-discovered processors from compilation classpath
        additionalOptions.add("-proc:none");
      }
    }

    while (tokenizer.hasMoreTokens()) {
      @NonNls String token = tokenizer.nextToken();
      if (version == JavaSdkVersion.JDK_1_0 && "-deprecation".equals(token)) {
        continue; // not supported for this version
      }
      if (!version.isAtLeast(JavaSdkVersion.JDK_1_5) && "-Xlint".equals(token)) {
        continue; // not supported in these versions
      }
      if (isAnnotationProcessing) {
        if (token.startsWith("-proc:")) {
          continue;
        }
        if (token.startsWith("-implicit:")) {
          continue;
        }
      }
      else { // compiling java
        if (annotationProcessorsEnabled) {
          // in this mode we have -proc:none already added above, so user's settings should be ignored
          if (token.startsWith("-proc:")) {
            continue;
          }
        }
      }
      if (token.startsWith("-J-")) {
        commandLine.add(token.substring("-J".length()));
      }
      else {
        additionalOptions.add(token);
      }
    }

    return additionalOptions;
  }

  public static void addCommandLineOptions(@NotNull ModuleChunk chunk,
                                           @NonNls List<String> commandLine,
                                           @NotNull String outputPath,
                                           @NotNull Sdk jdk,
                                           boolean isAnnotationProcessingMode) throws IOException {

    LanguageLevel languageLevel = chunk.getLanguageLevel();
    CompilerUtil.addSourceCommandLineSwitch(jdk, languageLevel, commandLine);

    commandLine.add("-verbose");

    final String bootCp = chunk.getCompilationBootClasspath();

    final String classPath = chunk.getCompilationClasspath();
    commandLine.add("-bootclasspath");
    addClassPathValue(commandLine, bootCp);

    commandLine.add("-classpath");
    addClassPathValue(commandLine, classPath);

    if (isAnnotationProcessingMode) {
      commandLine.add("-s");
      commandLine.add(outputPath.replace('/', File.separatorChar));
      final String moduleOutputPath = CompilerPaths.getModuleOutputPath(chunk.getModules()[0], false);
      if (moduleOutputPath != null) {
        commandLine.add("-d");
        commandLine.add(moduleOutputPath.replace('/', File.separatorChar));
      }
    }
    else {
      commandLine.add("-d");
      commandLine.add(outputPath.replace('/', File.separatorChar));
    }
  }

  private static void addClassPathValue(@NotNull List<String> commandLine,
                                        @NotNull String cpString) throws IOException {
    commandLine.add(cpString);
  }

  private Sdk getJdkForStartupCommand(final ModuleChunk chunk) {
    final Sdk jdk = chunk.getJdk();
    if (ApplicationManager.getApplication().isUnitTestMode() && JavacConfiguration.getOptions(myProject, JavacConfiguration.class).isTestsUseExternalCompiler()) {
      final String jdkHomePath = CompilerConfigurationImpl.getTestsExternalCompilerHome();
      if (jdkHomePath == null) {
        throw new IllegalArgumentException("[TEST-MODE] Cannot determine home directory for JDK to use javac from");
      }
      // when running under Mock JDK use VM executable from the JDK on which the tests run
      return new MockJdkWrapper(jdkHomePath, jdk);
    }
    return jdk;
  }

  public void compileFinished() {
    FileUtil.asyncDelete(myTempFiles);
    myTempFiles.clear();
  }
}
