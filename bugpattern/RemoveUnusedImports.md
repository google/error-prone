---
title: RemoveUnusedImports
summary: Unused imports
layout: bugpattern
tags: Style
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This import is unused.

## The check reported an import that was actually used!

The check has no known bugs. If you it reports an unused import that looks like
it was actually used, try removing it and recompiling. If everything still
compiles, it was unused.

Note that the check can detect some unused imports that `google-java-format`
cannot. The formatter looks at a single file at a time, so `RemoveUnusedImports`
is more accurate in examples like the following:

```java
package a;

import b.Baz; // this is unused!

class Foo extends Bar {
  Baz baz; // this is a.Bar.Baz (from the supertype), *not* b.Baz
}
```

```java
package a;
class Bar {
  class Baz {}
}
```

```java
package b;
class Baz {}
```

