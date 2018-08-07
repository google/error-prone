/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation.inference;

import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Proper inference variables are thin wrappers around Nullness lattice elements, lifted so that
 * they can be compared other inference variables.
 */
enum ProperInferenceVar implements InferenceVariable {
  BOTTOM {
    @Override
    Nullness nullness() {
      return Nullness.BOTTOM;
    }
  },
  NONNULL {
    @Override
    Nullness nullness() {
      return Nullness.NONNULL;
    }
  },
  NULL {
    @Override
    Nullness nullness() {
      return Nullness.NULL;
    }
  },
  NULLABLE {
    @Override
    Nullness nullness() {
      return Nullness.NULLABLE;
    }
  };

  abstract Nullness nullness();

  static Optional<InferenceVariable> fromTypeIfAnnotated(Type type) {
    return Nullness.fromAnnotations(
            type.getAnnotationMirrors().stream().map(Object::toString).collect(Collectors.toList()))
        .map(ProperInferenceVar::create);
  }

  static InferenceVariable create(Nullness nullness) {
    switch (nullness) {
      case BOTTOM:
        return ProperInferenceVar.BOTTOM;
      case NONNULL:
        return ProperInferenceVar.NONNULL;
      case NULL:
        return ProperInferenceVar.NULL;
      case NULLABLE:
        return ProperInferenceVar.NULLABLE;
    }
    throw new RuntimeException("Unhandled nullness value: " + nullness);
  }
}
