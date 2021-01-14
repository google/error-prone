/*
 * Copyright 2014 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;
import java.lang.annotation.Annotation;

/**
 * Interface for a transformation over Java source.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public interface CodeTransformer {
  /** Apply recursively from the leaf node in the given {@link TreePath}. */
  void apply(TreePath path, Context context, DescriptionListener listener);

  /**
   * Returns a map of annotation data logically applied to this code transformer.
   *
   * <p>As an example, if a {@code CodeTransformer} expressed as a Refaster rule had an annotation
   * applied to it:
   *
   * <pre>{@code
   * {@literal @}MyCustomAnnotation("value")
   * public class AnnotatedRefasterRule {
   *    {@literal @}BeforeTemplate void before(String x) {...}
   *    {@literal @}AfterTemplate void after(String x) {...}
   * }
   * }</pre>
   *
   * You could retrieve the value of {@code @MyCustomAnnotation} from this map.
   */
  ImmutableClassToInstanceMap<Annotation> annotations();
}
