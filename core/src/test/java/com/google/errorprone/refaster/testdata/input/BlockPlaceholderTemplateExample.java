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

import com.google.common.io.ByteStreams;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test data for {@code BlockPlaceholderTemplate}.
 */
public class BlockPlaceholderTemplateExample {
  public void positiveExample1() throws IOException {
    InputStream stream = new FileInputStream("foo.bar");
    try {
      System.out.println(ByteStreams.toByteArray(stream).length);
    } finally {
      stream.close();
    }
  }
  
  public void positiveExample2() throws IOException {
    InputStream stream = new FileInputStream("foo.bar");
    try {
      int count = 0;
      while (true) {
        int b = stream.read();
        if (b == -1) {
          break;
        }
        count++;
      }
      System.out.println(count);
    } finally {
      stream.close();
    }
  }
  
  public void negativeExample1() throws IOException { // modifies placeholder parameter
    InputStream stream = null;
    try {
      stream = new FileInputStream("foo.bar");
      System.out.println(ByteStreams.toByteArray(stream).length);
    } finally {
      stream.close();
    }
  }
  
  public void negativeExample2() throws IOException { // changes control flow
    for (int i = 0; i < 10; i++) {
      InputStream stream = new FileInputStream("foo.bar");
      try {
        System.out.println(ByteStreams.toByteArray(stream).length);
        break;
      } finally {
        stream.close();
      }
    }
  }
}
