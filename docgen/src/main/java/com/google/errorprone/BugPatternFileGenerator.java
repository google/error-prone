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

package com.google.errorprone;

import static com.google.common.base.Predicates.not;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.io.LineProcessor;
import com.google.errorprone.BugPattern.Instance;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.common.collect.ImmutableMap;

import java.io.IOError;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads each line of the bugpatterns.txt tab-delimited data file, and generates a GitHub
 * Jekyll page for each one.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
class BugPatternFileGenerator implements LineProcessor<List<Instance>> {
  private final Path outputDir;
  private final Path exampleDirBase;
  private final Path explanationDir;
  private List<Instance> result;

  /**
   * Enables pygments-style code highlighting blocks instead of github flavoured markdown style
   * code fences, because the latter make jekyll unhappy.
   */
  private final boolean usePygments;

  /**
   * Controls whether yaml front-matter is generated.
   */
  private final boolean generateFrontMatter;

  public BugPatternFileGenerator(
      Path bugpatternDir,
      Path exampleDirBase,
      Path explanationDir,
      boolean generateFrontMatter,
      boolean usePygments) {
    this.outputDir = bugpatternDir;
    this.exampleDirBase = exampleDirBase;
    this.explanationDir = explanationDir;
    this.generateFrontMatter = generateFrontMatter;
    this.usePygments = usePygments;
    result = new ArrayList<>();
  }

  private static class ExampleFilter implements DirectoryStream.Filter<Path> {
    private Pattern matchPattern;

    public ExampleFilter(String checkerName) {
      this.matchPattern = Pattern.compile(checkerName + "(Positive|Negative)Case.*");
    }

    @Override
    public boolean accept(Path entry) throws IOException {
      return Files.isDirectory(entry)
          || matchPattern.matcher(entry.getFileName().toString()).matches();
    }
  }

  /**
   * Construct an appropriate page template for this {@code BugPattern}.  Include altNames if
   * there are any, and explain the correct way to suppress.
   */
  private static MessageFormat constructPageTemplate(
      Instance pattern, boolean generateFrontMatter) {
    StringBuilder result = new StringBuilder();

    result.append(
        "<!--\n"
        + "*** AUTO-GENERATED, DO NOT MODIFY ***\n"
        + "To make changes, edit the @BugPattern annotation or the explanation in"
        + " docs/bugpattern.\n"
        + "-->\n\n");

    if (!generateFrontMatter) {
      result.append("<div style=\"float:right;\"><table id=\"metadata\">\n"
          + "<tr><td>Category</td><td>{3}</td></tr>\n"
          + "<tr><td>Severity</td><td>{4}</td></tr>\n"
          + "<tr><td>Maturity</td><td>{5}</td></tr>\n"
          + "</table></div>\n\n"
          + "# {1}\n"
          + "__{8}__\n\n");
    }

    if (pattern.altNames.length() > 0) {
      result.append("_Alternate names: {2}_\n\n");
    }
    result.append(
        "## The problem\n"
        + "{9}\n"
        + "\n"
        + "## Suppression\n");

    switch (pattern.suppressibility) {
      case SUPPRESS_WARNINGS:
        result.append("Suppress false positives by adding an `@SuppressWarnings(\"{1}\")` "
            + "annotation to the enclosing element.\n");
        break;
      case CUSTOM_ANNOTATION:
        result.append("Suppress false positives by adding the custom suppression annotation "
            + "`@{7}` to the enclosing element.\n");
        break;
      case UNSUPPRESSIBLE:
        result.append("This check may not be suppressed.\n");
        break;
    }
    return new MessageFormat(result.toString(), Locale.ENGLISH);
  }

  /**
   * A function to convert a test case file into an {@link ExampleInfo}.
   */
  private static class PathToExampleInfo implements Function<Path, ExampleInfo> {

    private final String checkerClass;

    public PathToExampleInfo(String checkerClass) {
      this.checkerClass = checkerClass;
    }

    @Override
    public ExampleInfo apply(Path path) {
      ExampleInfo.ExampleKind posOrNeg = null;
      String fileName = path.getFileName().toString();
      if (fileName.contains("Positive")) {
        posOrNeg = ExampleInfo.ExampleKind.POSITIVE;
      } else if (fileName.contains("Negative")) {
        posOrNeg = ExampleInfo.ExampleKind.NEGATIVE;
      } else {
        // ExampleFilter enforces this
        throw new AssertionError(
            "Example filename must contain \"Positive\" or \"Negative\", but was " + fileName);
      }

      String code;
      try {
        code = new String(Files.readAllBytes(path), UTF_8).trim();
      } catch (IOException e) {
        throw new IOError(e);
      }

      return ExampleInfo.create(posOrNeg, checkerClass, fileName, code);
    }
  }

  private static final Predicate<ExampleInfo> IS_POSITIVE =
      new Predicate<ExampleInfo>() {
        @Override
        public boolean apply(ExampleInfo input) {
          return input.type() == ExampleInfo.ExampleKind.POSITIVE;
        }
      };

  @Override
  public boolean processLine(String line) throws IOException {
    ArrayList<String> parts = new ArrayList<>(Splitter.on('\t').trimResults().splitToList(line));
    Instance pattern = new Instance();
    pattern.name = parts.get(1);
    pattern.altNames = parts.get(2);
    pattern.category = BugPattern.Category.valueOf(parts.get(3));
    pattern.maturity = MaturityLevel.valueOf(parts.get(5));
    pattern.summary = parts.get(8);
    pattern.severity = SeverityLevel.valueOf(parts.get(4));
    pattern.suppressibility = Suppressibility.valueOf(parts.get(6));
    pattern.customSuppressionAnnotation = parts.get(7);
    result.add(pattern);

    // replace spaces in filename with underscores
    Path checkPath = Paths.get(pattern.name.replace(' ', '_') + ".md");

    try (Writer writer = Files.newBufferedWriter(
        outputDir.resolve(checkPath), UTF_8)) {
      // replace "\n" with a carriage return for explanation
      parts.set(9, parts.get(9).replace("\\n", "\n"));

      // load side-car explanation file, if it exists
      Path sidecarExplanation = explanationDir.resolve(checkPath);
      if (Files.exists(sidecarExplanation)) {
        if (!parts.get(9).isEmpty()) {
          throw new AssertionError(
              String.format(
                  "%s specifies an explanation via @BugPattern and side-car",
                  pattern.name));
        }
        parts.set(9, new String(Files.readAllBytes(sidecarExplanation), UTF_8).trim());
      }

      MessageFormat wikiPageTemplate = constructPageTemplate(pattern, generateFrontMatter);

      if (generateFrontMatter) {
        writer.write("---\n");
        Map<String, String> data =
            ImmutableMap.<String, String>builder()
                .put("title", pattern.name)
                .put("summary", pattern.summary)
                .put("layout", "bugpattern")
                .put("category", pattern.category.toString())
                .put("severity", pattern.severity.toString())
                .put("maturity", pattern.maturity.toString())
                .build();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        yaml.dump(data, writer);
        writer.write("---\n\n");
      }

      writer.write(wikiPageTemplate.format(parts.toArray()));

      // Example filename must match example pattern.
      List<Path> examplePaths = new ArrayList<>();
      Filter<Path> filter =
          new ExampleFilter(parts.get(0).substring(parts.get(0).lastIndexOf('.') + 1));
      findExamples(examplePaths, exampleDirBase, filter);

      List<ExampleInfo> exampleInfos =
          FluentIterable.from(examplePaths)
              .transform(new PathToExampleInfo(parts.get(0)))
              .toSortedList( // sort by name
                  new Comparator<ExampleInfo>() {
                    @Override
                    public int compare(ExampleInfo first, ExampleInfo second) {
                      return first.name().compareTo(second.name());
                    }
                  });
      Collection<ExampleInfo> positiveExamples = Collections2.filter(exampleInfos, IS_POSITIVE);
      Collection<ExampleInfo> negativeExamples =
          Collections2.filter(exampleInfos, not(IS_POSITIVE));

      if (!exampleInfos.isEmpty()) {
        writer.write("\n----------\n\n");

        if (!positiveExamples.isEmpty()) {
          writer.write("### Positive examples\n");
          for (ExampleInfo positiveExample : positiveExamples) {
            writeExample(positiveExample, writer);
          }
        }

        if (!negativeExamples.isEmpty()) {
          writer.write("### Negative examples\n");
          for (ExampleInfo negativeExample : negativeExamples) {
            writeExample(negativeExample, writer);
          }
        }
      }
    }
    return true;
  }

  private void writeExample(ExampleInfo example, Writer writer) throws IOException {
    writer.write("__" + example.name() + "__\n\n");
    if (usePygments) {
      writer.write("{% highlight java %}\n");
    } else {
      writer.write("```java\n");
    }
    writer.write(example.code() + "\n");
    if (usePygments) {
      writer.write("{% endhighlight %}\n");
    } else {
      writer.write("```\n");
    }
    writer.write("\n");
  }

  private static void findExamples(
      List<Path> examples, Path dir, DirectoryStream.Filter<Path> filter) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          findExamples(examples, entry, filter);
        } else {
          examples.add(entry);
        }
      }
    }
  }

  @Override
  public List<BugPattern.Instance> getResult() {
    return result;
  }
}
