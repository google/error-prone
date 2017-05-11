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

package com.google.errorprone.bugpatterns.android.testdata;

/** @author avenet@google.com (Arnaud J. Venet) */
public class HardCodedSdCardPathNegativeCases {
  static final String PATH1 = "/home/sdcard";

  static final String PATH2 = "/data/file1";

  static final String FRAGMENT1 = "/root";

  static final String FRAGMENT2 = "sdcard";

  static final String PATH3 = FRAGMENT1 + "/" + FRAGMENT2;

  static final String PATH4 = "/data/dir/file2";

  static final String FRAGMENT3 = "/data";

  static final String FRAGMENT4 = "1user";

  static final String PATH5 = FRAGMENT3 + "/" + FRAGMENT4;
}
