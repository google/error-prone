// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.size;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.google.errorprone.BugPattern.MaturityLevel;

import org.kohsuke.MetaInfServices;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 *
 */
@MetaInfServices(Processor.class)
@SupportedAnnotationTypes({"com.google.errorprone.BugPattern"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class DocGen extends AbstractProcessor {
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(BugPattern.class)) {
      try {
        FileObject manifest = processingEnv.getFiler()
            .createResource(StandardLocation.SOURCE_OUTPUT, "", "bugPatterns.txt");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(manifest.openOutputStream()));
        pw.print(element.toString() + "\t");    //0
        BugPattern annotation = element.getAnnotation(BugPattern.class);
        pw.print(annotation.name() + "\t");     //1
        pw.print(annotation.category() + "\t"); //2
        pw.print(annotation.severity() + "\t"); //3
        pw.print(annotation.maturity() + "\t"); //4
        pw.print(annotation.summary() + "\t");  //5
        pw.println(annotation.explanation().replace("\n", "\\n"));   //6
        pw.close();
        
      } catch (IOException e) {
        // TODO(eaftan): Auto-generated catch block
        e.printStackTrace();
      }
    }
    return true;
  }

  private static final MessageFormat wikiPageTemplate = new MessageFormat(
      Joiner.on("\n").join(
          "#summary {5}",
          "#labels BugPattern",
          "=Bug pattern: {1}=",
          "  * Category: {2}",
          "  * Severity: {3}",
          "  * Maturity: {4}",
          "==The problem==",
          "{6}",
          ""
      ),
      Locale.ENGLISH);

  public static void main(String[] args) throws IOException {
    if (!new File(args[0]).exists()) {
      throw new IllegalArgumentException("Provide path to the wiki repository as arg0");
    }
    final File wikiDir = new File(args[0]);
    Reader patterns = new FileReader(new File("bugPatterns.txt"));
    String indexPage = 
        Files.readLines(new File("bugPatterns.txt"), UTF_8, new LineProcessor<String>() {
      private Multimap<MaturityLevel, String> index = ArrayListMultimap.create();
      
      @Override
      public String getResult() {
        StringBuilder result = new StringBuilder("#summary Bugs caught by error-prone\n"); 
        for(MaturityLevel level : index.keySet()) {
          result.append("=" + level + "=\n");
          for (String bugPattern : index.get(level)) {
            result.append("  * [" + bugPattern + "]\n");
          }
        }
        return result.toString();
      }

      @Override
      public boolean processLine(String line) throws IOException {
        String[] parts = line.split("\t");
        String checkName = parts[1];
        String maturity = parts[4];
        index.put(MaturityLevel.valueOf(maturity), checkName);
        // replace spaces in filename with underscores
        Writer writer = new FileWriter(new File(wikiDir, checkName.replace(' ', '_') + ".wiki"));
        // replace "\n" with a carriage return for explanation
        parts[6] = parts[6].replace("\\n", "\n");
        writer.write(wikiPageTemplate.format(parts));
        Iterable<String> classNameParts = Splitter.on('.').split(parts[0]);
        String path = Joiner.on('/').join(limit(classNameParts, size(classNameParts) - 1));
        File[] examples = new File("src/test/resources/" + path).listFiles();
        if (examples.length > 0) {
          writer.write("==Examples==\n");
        }
        for (File example: examples) {
          writer.write("===!" + example.getName() + "===\n");
          writer.write("{{{\n" + Files.toString(example, Charsets.UTF_8) + "\n}}}\n");
        }
        writer.close();
        return true;
      }
    });
    
    Writer indexWriter = new FileWriter(new File(wikiDir, "BugPatterns.wiki"));
    indexWriter.write(indexPage);
    indexWriter.close();
  }
}
