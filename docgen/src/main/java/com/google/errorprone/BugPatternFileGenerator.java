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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;
import com.google.gson.Gson;
import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Reads each line of the bugpatterns.txt tab-delimited data file, and generates a GitHub Jekyll
 * page for each one.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
class BugPatternFileGenerator implements LineProcessor<List<BugPatternInstance>> {

  private static final Joiner COMMA_JOINER = Joiner.on(", ");
  private static final Function<String, String> ANNOTATE_AND_CODIFY =
      new Function<String, String>() {
        @Override
        public String apply(String annotationName) {
          Preconditions.checkState(annotationName.endsWith(".class"));
          return "`@"
              + annotationName.substring(0, annotationName.length() - ".class".length())
              + "`";
        }
      };

  private final Path outputDir;
  private final Path exampleDirBase;
  private final Path explanationDir;
  private List<BugPatternInstance> result;

  /**
   * Enables pygments-style code highlighting blocks instead of github flavoured markdown style code
   * fences, because the latter make jekyll unhappy.
   */
  private final boolean usePygments;

  /** Controls whether yaml front-matter is generated. */
  private final boolean generateFrontMatter;

  /** The base url for links to bugpatterns. */
  @Nullable private final String baseUrl;

  public BugPatternFileGenerator(
      Path bugpatternDir,
      Path exampleDirBase,
      Path explanationDir,
      boolean generateFrontMatter,
      boolean usePygments,
      String baseUrl) {
    this.outputDir = bugpatternDir;
    this.exampleDirBase = exampleDirBase;
    this.explanationDir = explanationDir;
    this.generateFrontMatter = generateFrontMatter;
    this.usePygments = usePygments;
    this.baseUrl = baseUrl;
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

  /** A function to convert a test case file into an {@link ExampleInfo}. */
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
    BugPatternInstance pattern = new Gson().fromJson(line, BugPatternInstance.class);
    result.add(pattern);

    // replace spaces in filename with underscores
    Path checkPath = Paths.get(pattern.name.replace(' ', '_') + ".md");

    try (Writer writer = Files.newBufferedWriter(outputDir.resolve(checkPath), UTF_8)) {

      // load side-car explanation file, if it exists
      Path sidecarExplanation = explanationDir.resolve(checkPath);
      if (Files.exists(sidecarExplanation)) {
        if (!pattern.explanation.isEmpty()) {
          throw new AssertionError(
              String.format(
                  "%s specifies an explanation via @BugPattern and side-car", pattern.name));
        }
        pattern.explanation = new String(Files.readAllBytes(sidecarExplanation), UTF_8).trim();
      }

      // Construct an appropriate page for this {@code BugPattern}. Include altNames if
      // there are any, and explain the correct way to suppress.

      ImmutableMap.Builder<String, Object> templateData =
          ImmutableMap.<String, Object>builder()
              .put("tags", Joiner.on(", ").join(pattern.tags))
              .put("severity", pattern.severity)
              .put("providesFix", pattern.providesFix.displayInfo())
              .put("name", pattern.name)
              .put("summary", pattern.summary.trim())
              .put("altNames", Joiner.on(", ").join(pattern.altNames))
              .put("explanation", pattern.explanation.trim());

      if (baseUrl != null) {
        templateData.put("baseUrl", baseUrl);
      }

      if (generateFrontMatter) {
        Map<String, String> frontmatterData =
            ImmutableMap.<String, String>builder()
                .put("title", pattern.name)
                .put("summary", pattern.summary)
                .put("layout", "bugpattern")
                .put("tags", Joiner.on(", ").join(pattern.tags))
                .put("severity", pattern.severity.toString())
                .put("providesFix", pattern.providesFix.toString())
                .build();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        Writer yamlWriter = new StringWriter();
        yamlWriter.write("---\n");
        yaml.dump(frontmatterData, yamlWriter);
        yamlWriter.write("---\n");
        templateData.put("frontmatter", yamlWriter.toString());
      }

      if (pattern.documentSuppression) {
        String suppression;
        switch (pattern.suppressibility) {
          case SUPPRESS_WARNINGS:
            suppression =
                String.format(
                    "Suppress false positives by adding an `@SuppressWarnings(\"%s\")` "
                        + "annotation to the enclosing element.",
                    pattern.name);
            break;
          case CUSTOM_ANNOTATION:
            if (pattern.customSuppressionAnnotations.length == 1) {
              suppression =
                  String.format(
                      "Suppress false positives by adding the custom suppression annotation "
                          + "`@%s` to the enclosing element.",
                      pattern.customSuppressionAnnotations[0]);
            } else {
              suppression =
                  String.format(
                      "Suppress false positives by adding one of these custom suppression "
                          + "annotations to the enclosing element: %s",
                      COMMA_JOINER.join(
                          Lists.transform(
                              Arrays.asList(pattern.customSuppressionAnnotations),
                              ANNOTATE_AND_CODIFY)));
            }
            break;
          case UNSUPPRESSIBLE:
            suppression = "This check may not be suppressed.";
            break;
          default:
            throw new AssertionError(pattern.suppressibility);
        }
        templateData.put("suppression", suppression);
      }

      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache = mf.compile("com/google/errorprone/resources/bugpattern.mustache");
      mustache.execute(writer, templateData.build());

      if (pattern.generateExamplesFromTestCases) {
        // Example filename must match example pattern.
        List<Path> examplePaths = new ArrayList<>();
        Filter<Path> filter =
            new ExampleFilter(pattern.className.substring(pattern.className.lastIndexOf('.') + 1));
        findExamples(examplePaths, exampleDirBase, filter);

        List<ExampleInfo> exampleInfos =
            FluentIterable.from(examplePaths)
                .transform(new PathToExampleInfo(pattern.className))
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
  public List<BugPatternInstance> getResult() {
    return result;
  }
}
