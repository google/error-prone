/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.errorprone.refaster.testdata.template;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.MayOptionallyUse;
import com.google.errorprone.refaster.annotation.Placeholder;
import java.io.UnsupportedEncodingException;

/**
 * Template demonstrating the use of the @MayOptionallyUse annotation.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public abstract class MayOptionallyUseTemplate {
  @Placeholder
  abstract void useString(String str);

  @Placeholder
  abstract void handleException(@MayOptionallyUse Exception e);

  @BeforeTemplate
  public void tryCatch(byte[] bytes) {
    try {
      useString(new String(bytes, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      handleException(e);
    }
  }

  @AfterTemplate
  public void safe(byte[] bytes) {
    useString(new String(bytes, UTF_8));
  }
}
