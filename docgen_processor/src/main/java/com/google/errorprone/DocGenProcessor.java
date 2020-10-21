/*
 * Copyright 2011 The Error Prone Authors.
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

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor which visits all classes that have a {@code BugPattern} annotation, and
 * writes a tab-delimited text file dumping the data found.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author alexeagle@google.com (Alex Eagle)
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.google.errorprone.BugPattern")
public class DocGenProcessor extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  private final Gson gson = new Gson();

  private PrintWriter pw;

  /** {@inheritDoc} */
  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    try {
      FileObject manifest =
          processingEnv
              .getFiler()
              .createResource(StandardLocation.SOURCE_OUTPUT, "", "bugPatterns.txt");
      pw = new PrintWriter(new OutputStreamWriter(manifest.openOutputStream(), UTF_8), true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(BugPattern.class)) {
      gson.toJson(BugPatternInstance.fromElement(element), pw);
      pw.println();
    }

    if (roundEnv.processingOver()) {
      // this was the last round, do cleanup
      cleanup();
    }
    return false;
  }

  /** Perform cleanup after last round of annotation processing. */
  private void cleanup() {
    pw.close();
  }
}
