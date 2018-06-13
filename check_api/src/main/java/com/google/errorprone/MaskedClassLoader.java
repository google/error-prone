/*
 * Copyright 2016 The Error Prone Authors.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.tools.javac.api.ClientCodeWrapper.Trusted;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.net.URL;
import java.net.URLClassLoader;
import javax.tools.JavaFileManager;

/**
 * A classloader that allows plugins to access the Error Prone classes from the compiler classpath.
 */
// TODO(cushon): consolidate with Bazel's ClassloaderMaskingFileManager
public class MaskedClassLoader extends ClassLoader {

  /**
   * An alternative to {@link JavacFileManager#preRegister(Context)} that installs a {@link
   * MaskedClassLoader}.
   */
  public static void preRegisterFileManager(Context context) {
    context.put(
        JavaFileManager.class,
        new Context.Factory<JavaFileManager>() {
          @Override
          public JavaFileManager make(Context c) {
            return new MaskedFileManager(c);
          }
        });
  }

  public MaskedClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if (name.startsWith("com.google.errorprone.")
        || name.startsWith("org.checkerframework.dataflow.")) {
      return Class.forName(name);
    } else {
      throw new ClassNotFoundException(name);
    }
  }

  @Trusted
  static class MaskedFileManager extends JavacFileManager {

    public MaskedFileManager(Context context) {
      super(context, /* register= */ true, UTF_8);
    }

    public MaskedFileManager() {
      this(new Context());
    }

    @Override
    protected ClassLoader getClassLoader(URL[] urls) {
      return new URLClassLoader(
          urls, new MaskedClassLoader(JavacFileManager.class.getClassLoader()));
    }
  }
}
