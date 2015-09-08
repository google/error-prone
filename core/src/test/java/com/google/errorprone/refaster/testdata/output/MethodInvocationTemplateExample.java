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

package com.google.errorprone.refaster.testdata;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Example input for {@code MethodInvocationTemplate}.
 * 
 * @author lowasser@google.com (Louis Wasserman)
 */
public class MethodInvocationTemplateExample {
  public void example(MessageDigest digest, String string) throws NoSuchAlgorithmException {
    // positive examples
    MessageDigest.getInstance("MD5").digest("foo".getBytes(Charset.defaultCharset()));
    digest.digest("foo".getBytes(Charset.defaultCharset()));
    MessageDigest.getInstance("SHA1").digest(string.getBytes(Charset.defaultCharset()));
    digest.digest((string + 90).getBytes(Charset.defaultCharset()));
    // negative examples
    System.out.println("foo".getBytes());
  }
}
