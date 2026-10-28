---
title: AlmostExhaustiveTestParameter
summary: Prefer an exhaustive @TestParameter with an assumption to filter out specific
  enum values, so newly added enum values are automatically tested
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When using `TestParameterInjector` to test all values of an enum except one (or,
for proto3 enums, all declared values except one alongside the synthetic
`UNRECOGNIZED` value), prefer an implicit `@TestParameter` combined with a
`TruthJUnit.assume()` (or JUnit `Assume.assumeFalse()`) statement in the test
method body over listing every included value explicitly.

With an explicit list of included values, any new value added to the enum in the
future (for example, a new `AVIF` constant added to `ImageFormat`) is silently
omitted from the test. Using an implicit `@TestParameter` with an assumption
makes the exclusion self-documenting and ensures that newly added enum values
are automatically tested. (Small enums with fewer than 4 included values are
ignored.)

```java
@Test
public void decode(
    @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {
  assertThat(canDecode(format)).isTrue();
}
```

```java
@Test
public void decode(@TestParameter ImageFormat format) {
  assume().that(format).isNotEqualTo(ImageFormat.UNKNOWN);
  assertThat(canDecode(format)).isTrue();
}
```

For proto3 enums, excluding a sentinel value such as `IMAGE_FORMAT_UNSPECIFIED`
along with the synthetic `UNRECOGNIZED` constant can use `isNoneOf`:

```java
@Test
public void decode(@TestParameter ImageFormat format) {
  assume()
      .that(format)
      .isNoneOf(ImageFormat.IMAGE_FORMAT_UNSPECIFIED, ImageFormat.UNRECOGNIZED);
  assertThat(canDecode(format)).isTrue();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AlmostExhaustiveTestParameter")` to the enclosing element.
