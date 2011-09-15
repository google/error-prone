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

package com.google.errorprone;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Messages;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_6;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * Top-level class for inspecting Java source code for problems.
 * @author Alex Eagle (alexeagle@google.com)
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(RELEASE_6)
public class ErrorProneProcessor extends AbstractProcessor {

  private Trees trees;
  private Context context;

  @Override
  public void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    trees = Trees.instance(processingEnv);
    context = ((JavacProcessingEnvironment)processingEnv).getContext();

  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      String bundlePath = "/com/google/errorprone/errors.properties";
      InputStream bundleResource = getClass().getResourceAsStream(bundlePath);
      if (bundleResource == null) {
        throw new IllegalStateException("Resource bundle not found at " + bundlePath);
      }
      Messages.instance(context).add(new PropertyResourceBundle(bundleResource));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (!roundEnv.processingOver()) {
      for (Element element : roundEnv.getRootElements()) {
        Tree tree = trees.getTree(element);
        if (tree == null) {
          processingEnv.getMessager().printMessage(WARNING, "No tree found for element " + element);
        } else {
          tree.accept(new ASTVisitor(element, processingEnv, context), new VisitorState());
        }
      }
    }
    return true;
  }

}
