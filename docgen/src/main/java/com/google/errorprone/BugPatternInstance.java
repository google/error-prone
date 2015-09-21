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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.Suppressibility;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/** A serialization-friendly POJO of the information in a {@link BugPattern}. */
public final class BugPatternInstance {
  public String className;
  public String name;
  public String summary;
  public String explanation;
  public String[] altNames;
  public Category category;
  public MaturityLevel maturity;
  public SeverityLevel severity;
  public Suppressibility suppressibility;
  public String customSuppressionAnnotation;
  public boolean documentSuppression = true;

  public static BugPatternInstance fromElement(Element element) {
    BugPatternInstance instance = new BugPatternInstance();
    instance.className = element.toString();

    BugPattern annotation = element.getAnnotation(BugPattern.class);
    instance.name = annotation.name();
    instance.altNames = annotation.altNames();
    instance.category = annotation.category();
    instance.maturity = annotation.maturity();
    instance.severity = annotation.severity();
    instance.suppressibility = annotation.suppressibility();
    instance.summary = annotation.summary();
    instance.explanation = annotation.explanation();

    Map<String, Object> keyValues = getAnnotation(element, BugPattern.class.getName());
    // Avoid MirroredTypeException hacks:
    instance.customSuppressionAnnotation =
        firstNonNull(keyValues.get("customSuppressionAnnotation"), "").toString();
    // TODO(cushon): access directly once documentSuppression is in the released BugPattern
    if (keyValues.containsKey("documentSuppression")) {
      instance.documentSuppression =
          Boolean.parseBoolean(keyValues.get("documentSuppression").toString());
    }

    return instance;
  }

  private static Map<String, Object> getAnnotation(Element element, String name) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(name)) {
        return annotationKeyValues(mirror);
      }
    }
    throw new IllegalArgumentException(String.format("%s has no annotation %s", element, name));
  }

  private static Map<String, Object> annotationKeyValues(AnnotationMirror mirror) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (ExecutableElement key : mirror.getElementValues().keySet()) {
      result.put(key.getSimpleName().toString(), mirror.getElementValues().get(key).getValue());
    }
    return result;
  }

  public enum Comparators implements Comparator<BugPatternInstance> {
    BY_SEVERITY {
      @Override
      public int compare(BugPatternInstance o1, BugPatternInstance o2) {
        return o1.severity.compareTo(o2.severity);
      }
    },
    BY_NAME {
      @Override
      public int compare(BugPatternInstance o1, BugPatternInstance o2) {
        return o1.name.compareTo(o2.name);
      }
    }
  }
}
