---
title: TruthGetOrDefault
summary: Asserting on getOrDefault is unclear; prefer containsEntry or doesNotContainKey
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Expectation of `Truth.assertThat(map.getOrDefault(key,
defaultValue)).isEqualTo(expectedValue)` is unclear if the `defaultValue` is
same as `expectedValue`. If the test passes, its hard to say if `map` contained
`key, expectedValue` as an entry. Most likely, developer intended to verify that
`map` doesn't contain `'key` or perhaps map `key` isn't mapped to
`defaultValue`.

Additionally, same assertion can be simplified if `defaultValue` and
`expectedValue` are different constants to
`Truth.assertThat(map.get(key)).isEqualTo(expectedValue)`.

That is, prefer this:

```java
public static void doSomething(Map<String, String> map, String key, String expectedValue) {
  assertThat(map.get(key)).isEqualTo(expectedValue);
  assertThat(map).doesNotContainKey(key);
  assertThat(map).containsEntry(key, expectedValue);
}
```

to this:

```java
public static void doSomething(Map<String, String> map, String key, String defaultValue, String expectedValue) {
  assertThat(map.getOrDefault(key, defaultValue)).isEqualTo(expectedValue);
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TruthGetOrDefault")` to the enclosing element.

