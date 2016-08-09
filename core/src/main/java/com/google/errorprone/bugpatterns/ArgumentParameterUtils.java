/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for argument and parameter name analysis.
 *
 * @author yulissa@google.com (Yulissa Arroyo-Paredes)
 */
public class ArgumentParameterUtils {
  /**
   * Given the name of the argument and parameter this measures the similarity of the two strings.
   * The terms within the argument and parameter names are split, so "keepPath" becomes &lt;"keep",
   * "path"&gt;.
   *
   * @return percentage of how many terms are similar between the argList and paramList. There is a
   *     false positive if given terms like "fooBar" and "barFoo", the strings are not the same but
   *     the similarity is still 1.
   */
  public static double lexicalSimilarity(String arg, String param) {
    Set<String> argSplit = splitStringTermsToSet(arg);
    Set<String> paramSplit = splitStringTermsToSet(param);

    double commonTerms = Sets.intersection(argSplit, paramSplit).size() * 2;
    double totalTerms = argSplit.size() + paramSplit.size();
    return (commonTerms / totalTerms);
  }

  /**
   * @return list of doubles that stand for similarities between the argument and the parameter at
   *     that index.
   */
  public static List<Double> similarityOfArgToParams(String arg, List<String> params) {
    List<Double> simToParams = new ArrayList<>();
    for (String param : params) {
      simToParams.add(lexicalSimilarity(arg, param));
    }
    return simToParams;
  }

  private static HashSet<String> splitStringTermsToSet(String name) {
    // TODO(yulissa): Handle constants in the form of upper underscore
    String nameSplit = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    return Sets.newHashSet(
        Splitter.on('_').trimResults().omitEmptyStrings().splitToList(nameSplit));
  }
}
