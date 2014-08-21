/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Queue;

/**
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorReportingJavaCompiler extends JavaCompiler {

  private final ErrorProneAnalyzer errorProneAnalyzer;

  /**
   * Registers our message bundle reflectively, so we can compile against both JDK6 and JDK7.
   * OpenJDK6: com.sun.tools.javac.util.Messages.instance(context)
   *             .add("com.google.errorprone.errors");
   * OpenJDK7: com.sun.tools.javac.util.JavacMessages.instance(context)
   *             .add("com.google.errorprone.errors");
   */
  public static void setupMessageBundle(Context context) {
    if (System.getProperty("java.vm.vendor").equals("Apple Inc.")) {
      throw new UnsupportedOperationException("error-prone doesn't work on Apple JDK's yet.\n" +
          "Vote: http://code.google.com/p/error-prone/issues/detail?id=16");
    }

    try {
      Class<?> messagesClass;
      try {
        messagesClass = ErrorReportingJavaCompiler.class.getClassLoader()
            .loadClass("com.sun.tools.javac.util.Messages");
      } catch (ClassNotFoundException e) {
        messagesClass = ErrorReportingJavaCompiler.class.getClassLoader()
            .loadClass("com.sun.tools.javac.util.JavacMessages");
      }
      Object instance = messagesClass.getMethod("instance", Context.class).invoke(null, context);
      messagesClass.getMethod("add", String.class).invoke(instance, "com.google.errorprone.errors");
    } catch (Exception e) {
      throw new RuntimeException(String.format(
          "Unable to register message bundle. java.vm.vendor=[%s],  java.version=[%s]",
          System.getProperty("java.vm.vendor"), System.getProperty("java.version")),
          e);
    }
  }

  public ErrorReportingJavaCompiler(Context context) {
    super(context);
    assert context.get(Scanner.class) != null;

    // Setup message bundle.
    setupMessageBundle(context);

    // Create ErrorProneAnalyzer.
    errorProneAnalyzer = new ErrorProneAnalyzer(log, context);
  }

  /**
   * Adds an initialization hook to the Context, such that each subsequent
   * request for a JavaCompiler (i.e., a lookup for 'compilerKey' of our
   * superclass, JavaCompiler) will actually construct and return our version.
   * It's necessary since many new JavaCompilers may
   * be requested for later stages of the compilation (annotation processing),
   * within the same Context. And it's the preferred way for extending behavior
   * within javac, per the documentation in {@link com.sun.tools.javac.util.Context}.
   */
  public static void preRegister(final Context context) {
    final Scanner scanner = context.get(Scanner.class);
    context.put(compilerKey, new Factory<JavaCompiler>() {
      //@Override for OpenJDK 7 only
      public JavaCompiler make(Context ctx) {
        // Ensure that future processing rounds continue to use the same Scanner.
        ctx.put(Scanner.class, scanner);
        return new ErrorReportingJavaCompiler(ctx);
      }
      //@Override for OpenJDK 6 only
      public JavaCompiler make() {
        return new ErrorReportingJavaCompiler(context);
      }
    });
  }

  @Override
  protected void flow(Env<AttrContext> attrContextEnv, Queue<Env<AttrContext>> envs) {
    super.flow(attrContextEnv, envs);
    try {
      postFlow(attrContextEnv);
    } catch (Throwable e) {
      ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
      PrintWriter writer = new PrintWriter(stackTrace);
      e.printStackTrace(writer);
      writer.flush();
      Properties mavenProperties = new Properties();
      String version;
      try {
        mavenProperties.load(getClass().getResourceAsStream(
            "/META-INF/maven/com.google.errorprone/error_prone_core/pom.properties"));
        version = mavenProperties.getProperty("version");
      } catch (Exception e1) {
        version = "unknown: " + e1.getClass().getSimpleName() + ": " + e1.getMessage();
      }
      log.error("error.prone.crash", stackTrace.toString(), version);
    }
  }

  public void profilePostFlow(Env<AttrContext> attrContextEnv) {
    try {
      // For profiling with YourKit, add to classpath:
      // <Profiler Installation Directory>/lib/yjp-controller-api-redist.jar
//      Controller controller = new Controller();
//      controller.startCPUProfiling(ProfilingModes.CPU_SAMPLING, "");
      postFlow(attrContextEnv);
//      controller.stopCPUProfiling();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Run Error Prone analysis after performing dataflow checks.
   */
  public void postFlow(Env<AttrContext> env) {
    errorProneAnalyzer.reportReadyForAnalysis(env, errorCount() > 0);
  }

}
