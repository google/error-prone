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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;
import static com.google.common.io.MoreFiles.asCharSource;
import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ApiFactory.fullyErasedAndUnannotatedType;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.Rules.ErrorProneMethodRule;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/** External source of information about API return value ignorability. */
public final class ExternalApiList extends ErrorProneMethodRule {

  /** Returns a rule using an external list of APIs to treat as @CanIgnoreReturnValue. */
  public static ResultUseRule<VisitorState, Symbol> externalIgnorableList() {
    return new ExternalApiList(EXTERNAL_API_IGNORABLE_LIST, ResultUsePolicy.OPTIONAL);
  }

  /** Returns a rule using an external list of APIs to treat as unspecified. */
  public static ResultUseRule<VisitorState, Symbol> externalUnspecifiedList() {
    return new ExternalApiList(EXTERNAL_API_UNSPECIFIED_LIST, ResultUsePolicy.UNSPECIFIED);
  }

  private static final String EXTERNAL_API_IGNORABLE_LIST = "CheckReturnValue:ApiIgnorableList";
  private static final String EXTERNAL_API_UNSPECIFIED_LIST = "CheckReturnValue:ApiUnspecifiedList";

  private static final String API_LIST_PARSER = "CheckReturnValue:ApiListParser";

  private static final Supplier<ConfigParser> CONFIG_PARSER =
      VisitorState.memoize(
          state ->
              state
                  .errorProneOptions()
                  .getFlags()
                  .getEnum(API_LIST_PARSER, ConfigParser.class)
                  .orElse(ConfigParser.AS_STRINGS));

  private final ResultUsePolicy policy;
  private final Supplier<MethodPredicate> evaluator;

  private ExternalApiList(String flagName, ResultUsePolicy policy) {
    this.policy = policy;
    this.evaluator =
        VisitorState.memoize(
            state ->
                state
                    .errorProneOptions()
                    .getFlags()
                    .get(flagName)
                    .filter(s -> !s.isEmpty())
                    .map(filename -> loadConfigListFromFile(filename, CONFIG_PARSER.get(state)))
                    .orElse((m, s) -> false));
  }

  @Override
  public String id() {
    return "EXTERNAL_API_" + policy.name() + "_LIST";
  }

  @Override
  public Optional<ResultUsePolicy> evaluateMethod(MethodSymbol method, VisitorState state) {
    return evaluator.get(state).methodMatches(method, state)
        ? Optional.of(policy)
        : Optional.empty();
  }

  /** Encapsulates asking "does this API match the list of APIs I care about"? */
  @FunctionalInterface
  interface MethodPredicate {
    boolean methodMatches(MethodSymbol methodSymbol, VisitorState state);
  }

  enum ConfigParser {
    AS_STRINGS {
      @Override
      MethodPredicate load(String file) throws IOException {
        return configByInterpretingMethodsAsStrings(asCharSource(Paths.get(file), UTF_8));
      }
    },
    AS_STRINGS_FROM_RESOURCE {
      @Override
      MethodPredicate load(String file) throws IOException {
        return configByInterpretingMethodsAsStrings(asCharSource(getResource(file), UTF_8));
      }
    },
    PARSE_TOKENS {
      @Override
      MethodPredicate load(String file) throws IOException {
        return configByParsingApiObjects(asCharSource(Paths.get(file), UTF_8));
      }
    };

    abstract MethodPredicate load(String file) throws IOException;
  }

  static MethodPredicate loadConfigListFromFile(String filename, ConfigParser configParser) {
    try {
      return configParser.load(filename);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not load external resource for CanIgnoreReturnValue", e);
    }
  }

  private static MethodPredicate configByInterpretingMethodsAsStrings(CharSource file)
      throws IOException {
    ImmutableSet<String> apis;
    // NB: No whitespace stripping here
    try (Stream<String> lines = file.lines()) {
      apis = lines.collect(toImmutableSet());
    }
    return new MethodPredicate() {
      @Override
      public boolean methodMatches(MethodSymbol methodSymbol, VisitorState state) {
        // Construct an API identifier for this method, which involves erasing parameter types
        return apis.contains(apiSignature(methodSymbol, state.getTypes()));
      }

      private String apiSignature(MethodSymbol methodSymbol, Types types) {
        return methodSymbol.owner.getQualifiedName()
            + "#"
            + methodNameAndParams(methodSymbol, types);
      }
    };
  }

  private static MethodPredicate configByParsingApiObjects(CharSource file) throws IOException {
    ImmutableSetMultimap<String, Api> apis;
    try (Stream<String> lines = file.lines()) {
      apis = lines.map(Api::parse).collect(toImmutableSetMultimap(Api::className, api -> api));
    }
    return (methodSymbol, state) ->
        apis.get(surroundingClass(methodSymbol)).stream()
            .anyMatch(
                api ->
                    methodSymbol.getSimpleName().contentEquals(api.methodName())
                        && methodParametersMatch(
                            api.parameterTypes(), methodSymbol.params(), state.getTypes()));
  }

  public static String surroundingClass(MethodSymbol methodSymbol) {
    return methodSymbol.enclClass().getQualifiedName().toString();
  }

  public static String methodNameAndParams(MethodSymbol methodSymbol, Types types) {
    return methodSymbol.name + "(" + paramsString(types, methodSymbol.params()) + ")";
  }

  private static boolean methodParametersMatch(
      ImmutableList<String> parameters, List<VarSymbol> methodParams, Types types) {
    return Iterables.elementsEqual(
        parameters,
        Iterables.transform(methodParams, p -> fullyErasedAndUnannotatedType(p.type, types)));
  }

  private static String paramsString(Types types, List<VarSymbol> params) {
    if (params.isEmpty()) {
      return "";
    }
    return String.join(
        ",", Iterables.transform(params, p -> fullyErasedAndUnannotatedType(p.type, types)));
  }
}
