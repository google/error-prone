/*
 * Copyright 2013 Google Inc. All rights reserved.
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

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * A simplistic representation of a template match; can be used for expecting certain results from a
 * scan.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class Match {
  public static Match create(TemplateMatch match) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (Map.Entry<Bindings.Key<?>, Object> entry : match.getUnifier().getBindings().entrySet()) {
      builder.put(entry.getKey().getIdentifier(), entry.getValue().toString());
    }
    return create(builder.build());
  }

  public static Match create(Map<String, String> bindings) {
    return new AutoValue_Match(ImmutableMap.copyOf(bindings));
  }

  abstract Map<String, String> bindings();
}
