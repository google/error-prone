/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static org.junit.Assert.assertThrows;

import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.Suppressibility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class BugPatternValidatorTest {

  private @interface CustomSuppressionAnnotation {}

  private @interface CustomSuppressionAnnotation2 {}

  @Test
  public void basicBugPattern() throws Exception {
    @BugPattern(
      name = "BasicBugPattern",
      summary = "Simplest possible BugPattern",
      explanation = "Simplest possible BugPattern ",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    BugPatternValidator.validate(annotation);
  }

  @Test
  public void linkTypeNoneAndNoLink() throws Exception {
    @BugPattern(
      name = "LinkTypeNoneAndNoLink",
      summary = "linkType none and no link",
      explanation = "linkType none and no link",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      linkType = LinkType.NONE
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    BugPatternValidator.validate(annotation);
  }

  @Test
  public void linkTypeNoneButIncludesLink() throws Exception {
    @BugPattern(
      name = "LinkTypeNoneButIncludesLink",
      summary = "linkType none but includes link",
      explanation = "linkType none but includes link",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      linkType = LinkType.NONE,
      link = "http://foo"
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }

  @Test
  public void linkTypeCustomAndIncludesLink() throws Exception {
    @BugPattern(
      name = "LinkTypeCustomAndIncludesLink",
      summary = "linkType custom and includes link",
      explanation = "linkType custom and includes link",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      linkType = LinkType.CUSTOM,
      link = "http://foo"
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    BugPatternValidator.validate(annotation);
  }

  @Test
  public void linkTypeCustomButNoLink() throws Exception {
    @BugPattern(
      name = "LinkTypeCustomButNoLink",
      summary = "linkType custom but no link",
      explanation = "linkType custom but no link",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      linkType = LinkType.CUSTOM
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }

  @Test
  public void suppressWarningsButIncludesCustomAnnotation() throws Exception {
    @BugPattern(
      name = "SuppressWarningsButIncludesCustomAnnotation",
      summary = "Uses SuppressWarnings but includes custom suppression annotation",
      explanation = "Uses SuppressWarnings but includes custom suppression annotation",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.SUPPRESS_WARNINGS,
      customSuppressionAnnotations = CustomSuppressionAnnotation.class
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }

  @Test
  public void unsuppressible() throws Exception {
    @BugPattern(
      name = "Unsuppressible",
      summary = "An unsuppressible BugPattern",
      explanation = "An unsuppressible BugPattern",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.UNSUPPRESSIBLE
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    BugPatternValidator.validate(annotation);
  }

  @Test
  public void unsuppressibleButIncludesCustomAnnotation() throws Exception {
    @BugPattern(
      name = "unsuppressibleButIncludesCustomAnnotation",
      summary = "Unsuppressible but includes custom suppression annotation",
      explanation = "Unsuppressible but includes custom suppression annotation",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.UNSUPPRESSIBLE,
      customSuppressionAnnotations = CustomSuppressionAnnotation.class
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }

  @Test
  public void customSuppressionAnnotation() throws Exception {
    @BugPattern(
      name = "customSuppressionAnnotation",
      summary = "Uses a custom suppression annotation",
      explanation = "Uses a custom suppression annotation",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.CUSTOM_ANNOTATION,
      customSuppressionAnnotations = CustomSuppressionAnnotation.class
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    BugPatternValidator.validate(annotation);
  }

  @Test
  public void multipleCustomSuppressionAnnotations() throws Exception {
    @BugPattern(
      name = "customSuppressionAnnotation",
      summary = "Uses multiple custom suppression annotations",
      explanation = "Uses multiple custom suppression annotations",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.CUSTOM_ANNOTATION,
      customSuppressionAnnotations = {
        CustomSuppressionAnnotation.class,
        CustomSuppressionAnnotation2.class
      }
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    BugPatternValidator.validate(annotation);
  }

  @Test
  public void customSuppressionAnnotationButSuppressWarnings() throws Exception {
    @BugPattern(
      name = "customSuppressionAnnotationButSuppressWarnings",
      summary = "Specifies a custom suppression annotation of @SuppressWarnings",
      explanation = "Specifies a custom suppression annotation of @SuppressWarnings",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.CUSTOM_ANNOTATION,
      customSuppressionAnnotations = SuppressWarnings.class
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }

  @Test
  public void customSuppressionAnnotationsIncludesSuppressWarnings() throws Exception {
    @BugPattern(
      name = "customSuppressionAnnotationButSuppressWarnings",
      summary = "Specifies multiple custom suppression annotations including @SuppressWarnings",
      explanation = "Specifies multiple custom suppression annotations including @SuppressWarnings",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.CUSTOM_ANNOTATION,
      customSuppressionAnnotations = {CustomSuppressionAnnotation.class, SuppressWarnings.class}
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }

  @Test
  public void customSuppressionAnnotationButNoneSpecified() throws Exception {
    @BugPattern(
      name = "customSuppressionAnnotationButNoneSpecified",
      summary = "Sets suppressibility to custom but doesn't provide a custom annotation",
      explanation = "Sets suppressibility to custom but doesn't provide a custom annotation",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      suppressibility = Suppressibility.CUSTOM_ANNOTATION
    )
    final class BugPatternTestClass {}

    BugPattern annotation = BugPatternTestClass.class.getAnnotation(BugPattern.class);
    assertThrows(ValidationException.class, () -> BugPatternValidator.validate(annotation));
  }
}
