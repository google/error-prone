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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Multimaps.index;
import static com.google.common.io.Files.readLines;

import com.google.common.base.Joiner;
import org.kohsuke.MetaInfServices;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;

/**
 * This class has two responsibilities:
 *
 * Annotation processor which visits all classes that have a {@code BugPattern} annotation,
 * and writes a tab-delimited text file dumping the data found.
 *
 * Utility main which consumes the same tab-delimited text file and generates GitHub pages for
 * the BugPatterns.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author alexeagle@google.com (Alex Eagle)
 */
@MetaInfServices(Processor.class)
@SupportedAnnotationTypes({"com.google.errorprone.BugPattern"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DocGen extends AbstractProcessor {

  private PrintWriter pw;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    try {
      FileObject manifest = processingEnv.getFiler()
          .createResource(StandardLocation.SOURCE_OUTPUT, "", "bugPatterns.txt");
      pw = new PrintWriter(new OutputStreamWriter(manifest.openOutputStream()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(BugPattern.class)) {
      pw.print(element.toString() + "\t");    //0
      BugPattern annotation = element.getAnnotation(BugPattern.class);
      pw.print(annotation.name() + "\t");     //1
      pw.print(Joiner.on(", ").join(annotation.altNames()) + "\t"); //2
      pw.print(annotation.category() + "\t"); //3
      pw.print(annotation.severity() + "\t"); //4
      pw.print(annotation.maturity() + "\t"); //5
      pw.print(annotation.suppressibility() + "\t"); //6
      // This is ugly, but you always get a MirroredTypeException when you try to access this
      // element. See https://community.oracle.com/thread/1184190.
      try {
        annotation.customSuppressionAnnotation();
        throw new IllegalStateException("Expected a MirroredTypeException");
      } catch (MirroredTypeException e) {
        pw.print(e.getTypeMirror().toString() + "\t"); //7
      }
      pw.print(annotation.summary() + "\t");  //8
      pw.println(annotation.explanation().replace("\n", "\\n")); //9
    }

    if (roundEnv.processingOver()) {
      // this was the last round, do cleanup
      cleanup();
    }
    return true;
  }

  /**
   * Perform cleanup after last round of annotation processing.
   */
  private void cleanup() {
    pw.close();
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.err.println("Usage: java DocGen " +
          "<path to bugPatterns.txt> <path to docs repository> <path to examples>");
      System.exit(1);
    }
    final File bugPatterns = new File(args[0]);
    if (!bugPatterns.exists()) {
      System.err.println("Cannot find bugPatterns file: " + args[0]);
      System.exit(1);
    }
    final File wikiDir = new File(args[1]);
    wikiDir.mkdir();
    final File exampleDirBase = new File(args[2]);
    if (!exampleDirBase.exists()) {
      System.err.println("Cannot find example directory: " + args[2]);
      System.exit(1);
    }

    File bugpatternDir = new File(wikiDir, "bugpattern");
    if (!bugpatternDir.exists()) {
      bugpatternDir.mkdirs();
    }
    new File(wikiDir, "_data").mkdirs();
    BugPatternFileGenerator generator = new BugPatternFileGenerator(bugpatternDir, exampleDirBase);
    try (Writer w = new FileWriter(new File(wikiDir, "_data/bugpatterns.yaml"))) {
      new BugPatternIndexYamlWriter().dump(readLines(bugPatterns, UTF_8, generator), w);
    }
  }

}
