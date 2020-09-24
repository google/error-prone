/*
 * Copyright 2014 The Error Prone Authors.
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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.LineProcessor;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Reads each line of the bugpatterns.txt tab-delimited data file, and generates a GitHub Jekyll
 * page for each one.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
class BugPatternFileGenerator implements LineProcessor<List<BugPatternInstance>> {

  private final Path outputDir;
  private final Path explanationDir;
  private final List<BugPatternInstance> result;

  private final Function<BugPatternInstance, SeverityLevel> severityRemapper;

  /** Controls whether yaml front-matter is generated. */
  private final boolean generateFrontMatter;

  /** The base url for links to bugpatterns. */
  @Nullable private final String baseUrl;

  public BugPatternFileGenerator(
      Path bugpatternDir,
      Path explanationDir,
      boolean generateFrontMatter,
      String baseUrl,
      Function<BugPatternInstance, SeverityLevel> severityRemapper) {
    this.outputDir = bugpatternDir;
    this.explanationDir = explanationDir;
    this.severityRemapper = severityRemapper;
    this.generateFrontMatter = generateFrontMatter;
    this.baseUrl = baseUrl;
    result = new ArrayList<>();
  }

  @Override
  public boolean processLine(String line) throws IOException {
    BugPatternInstance pattern = new Gson().fromJson(line, BugPatternInstance.class);
    pattern.severity = severityRemapper.apply(pattern);
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
              .put("name", pattern.name)
              .put("className", pattern.className)
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
                .build();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new SafeConstructor(), new Representer(), options);
        Writer yamlWriter = new StringWriter();
        yamlWriter.write("---\n");
        yaml.dump(frontmatterData, yamlWriter);
        yamlWriter.write("---\n");
        templateData.put("frontmatter", yamlWriter.toString());
      }

      if (pattern.documentSuppression) {
        String suppressionString;
        if (pattern.suppressionAnnotations.length == 0) {
          suppressionString = "This check may not be suppressed.";
        } else {
          suppressionString =
              pattern.suppressionAnnotations.length == 1
                  ? "Suppress false positives by adding the suppression annotation %s to the "
                      + "enclosing element."
                  : "Suppress false positives by adding one of these suppression annotations to "
                      + "the enclosing element: %s";
          suppressionString =
              String.format(
                  suppressionString,
                  Arrays.stream(pattern.suppressionAnnotations)
                      .map((String anno) -> standardizeAnnotation(anno, pattern.name))
                      .collect(Collectors.joining(", ")));
        }
        templateData.put("suppression", suppressionString);
      }

      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache = mf.compile("com/google/errorprone/resources/bugpattern.mustache");
      mustache.execute(writer, templateData.build());
    }
    return true;
  }

  private String standardizeAnnotation(String fullAnnotationName, String patternName) {
    String annotationName =
        fullAnnotationName.endsWith(".class")
            ? fullAnnotationName.substring(0, fullAnnotationName.length() - ".class".length())
            : fullAnnotationName;
    if (annotationName.equals(SuppressWarnings.class.getName())) {
      annotationName = SuppressWarnings.class.getSimpleName() + "(\"" + patternName + "\")";
    }
    return "`@" + annotationName + "`";
  }

  @Override
  public List<BugPatternInstance> getResult() {
    return result;
  }
}
