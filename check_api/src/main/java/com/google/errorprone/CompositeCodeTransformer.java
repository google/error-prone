/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/** Combines multiple {@code CodeTransformer}s into one. */
@AutoValue
public abstract class CompositeCodeTransformer implements CodeTransformer, Serializable {
  public static CodeTransformer compose(CodeTransformer... transformers) {
    return compose(ImmutableList.copyOf(transformers));
  }

  public static CodeTransformer compose(Iterable<? extends CodeTransformer> transformers) {
    return new AutoValue_CompositeCodeTransformer(ImmutableList.copyOf(transformers));
  }

  CompositeCodeTransformer() {}

  public abstract ImmutableList<CodeTransformer> transformers();

  @Override
  public void apply(TreePath path, Context context, DescriptionListener listener) {
    for (CodeTransformer transformer : transformers()) {
      transformer.apply(path, context, listener);
    }
  }

  @Override
  public ImmutableClassToInstanceMap<Annotation> annotations() {
    return ImmutableClassToInstanceMap.<Annotation>builder().build();
  }
}
