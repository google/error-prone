/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.LineProcessor;
import com.google.common.io.MoreFiles;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Optional;

/** External source of information about @CanIgnoreReturnValue-equivalent API's. */
public class ExternalCanIgnoreReturnValue {
  private ExternalCanIgnoreReturnValue() {}

  private static final String EXTERNAL_API_EXCLUSION_LIST = "CheckReturnValue:ApiExclusionList";

  private static final Supplier<Optional<FromConfig>> EXTERNAL_RESOURCE =
      VisitorState.memoize(
          state ->
              state
                  .errorProneOptions()
                  .getFlags()
                  .get(EXTERNAL_API_EXCLUSION_LIST)
                  .map(ExternalCanIgnoreReturnValue::tryLoadingConfigFile));

  public static boolean externallyConfiguredCirvAnnotation(MethodSymbol m, VisitorState s) {
    return EXTERNAL_RESOURCE.get(s).map(protoList -> protoList.methodMatches(m, s)).orElse(false);
  }

  private static FromConfig tryLoadingConfigFile(String filename) {
    try {
      ImmutableSetMultimap<String, Api> apis =
          MoreFiles.asCharSource(Paths.get(filename), UTF_8)
              .readLines(
                  new LineProcessor<>() {
                    private final ImmutableSetMultimap.Builder<String, Api> collectedApis =
                        ImmutableSetMultimap.builder();

                    @Override
                    public boolean processLine(String line) {
                      Api parsed = Api.parse(line);
                      collectedApis.put(parsed.className(), parsed);
                      return true;
                    }

                    @Override
                    public ImmutableSetMultimap<String, Api> getResult() {
                      return collectedApis.build();
                    }
                  });
      return new FromConfig(apis);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not load external resource for CanIgnoreReturnValue", e);
    }
  }

  private static final class FromConfig {
    // TODO(glorioso): Lots of different ways to think about this one here.
    //   * Do we make Api Comparable and use SortedSet?
    //   * Index by outer class?
    //   * Just Set<Api>?
    private final ImmutableSetMultimap<String, Api> apis;

    private FromConfig(ImmutableSetMultimap<String, Api> apis) {
      this.apis = apis;
    }

    boolean methodMatches(MethodSymbol methodSymbol, VisitorState state) {
      return apis.get(methodSymbol.enclClass().getQualifiedName().toString()).stream()
          .anyMatch(api -> apiMatchesMethodSymbol(methodSymbol, api, state));
    }

    private static boolean apiMatchesMethodSymbol(
        MethodSymbol methodSymbol, Api api, VisitorState state) {
      if (!methodSymbol.getSimpleName().contentEquals(api.methodName())) {
        return false;
      }

      // Check for compatibility of these params.
      return Iterables.elementsEqual(
          api.parameterTypes(),
          Iterables.transform(
              methodSymbol.params(), p -> state.getTypes().erasure(p.type).toString()));
    }
  }
}
