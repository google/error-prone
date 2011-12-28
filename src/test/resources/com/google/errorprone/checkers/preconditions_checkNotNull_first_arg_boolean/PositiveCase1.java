/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package preconditions_checkNotNull_first_arg_boolean;

import com.google.common.base.Preconditions;

public class PositiveCase1 {
  Object a = new Object();
  Object b = new Object();
  public void error() {
    Preconditions.checkNotNull(a != null);
    Preconditions.checkNotNull(a != null && b != null);
    Preconditions.checkNotNull(a != null, "Shouldn't be null");
    Preconditions.checkNotNull(a.toString().toString() != null, "Shouldn't be null");
    Preconditions.checkNotNull(null != a, "Shouldn't be null");
    Preconditions.checkNotNull(null != a, "Shouldn't be null %s %s", "str", "str2");
    Preconditions.checkNotNull(a != null && b != null, "Shouldn't be null");
    Preconditions.checkNotNull(a != null && b != null, "Shouldn't be null %s %s", "str", "str2");

  }
}