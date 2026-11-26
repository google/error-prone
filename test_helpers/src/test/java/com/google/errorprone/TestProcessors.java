/*
 * Copyright 2026 The Error Prone Authors.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/// Shared annotation processor fixtures for test helper tests.
public final class TestProcessors {

  /// Processor that fails compilation whenever `@Trigger` is present.
  @SupportedAnnotationTypes("Trigger")
  public static final class FailingProcessor extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      for (TypeElement annotation : annotations) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
          processingEnv
              .getMessager()
              .printMessage(Diagnostic.Kind.ERROR, "FailingProcessor ran", element);
        }
      }
      return false;
    }
  }

  /// Writes a JAR in `directory` containing `processorClass` and a
  /// `META-INF/services/javax.annotation.processing.Processor` entry registering it, and returns
  /// the path to the JAR.
  public static Path createProcessorJar(Path directory, Class<? extends Processor> processorClass)
      throws IOException {
    Path jarPath = directory.resolve("processor.jar");
    try (OutputStream os = Files.newOutputStream(jarPath);
        JarOutputStream jos = new JarOutputStream(os)) {
      String classEntry = processorClass.getName().replace('.', '/') + ".class";
      jos.putNextEntry(new JarEntry(classEntry));
      try (InputStream is =
          requireNonNull(
              processorClass.getClassLoader().getResourceAsStream(classEntry), classEntry)) {
        is.transferTo(jos);
      }
      jos.closeEntry();

      jos.putNextEntry(new JarEntry("META-INF/services/javax.annotation.processing.Processor"));
      jos.write((processorClass.getName() + "\n").getBytes(UTF_8));
      jos.closeEntry();
    }
    return jarPath;
  }

  private TestProcessors() {}
}
