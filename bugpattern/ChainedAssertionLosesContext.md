---
title: ChainedAssertionLosesContext
summary: Inside a Subject, use check(...) instead of assert*() to preserve user-supplied messages and other settings.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Assertions made _inside the implementation of another [Truth] assertion_ should
use [`check`], not `assertThat`.

Before:

```
class MyProtoSubject extends Subject {
  ...

  public void hasFoo(Foo expected) {
    assertThat(actual.foo()).isEqualTo(expected);
  }
}
```

After:

```
class MyProtoSubject extends Subject {
  ...

  public void hasFoo(Foo expected) {
    check("foo()").that(actual.foo()).isEqualTo(expected);
  }
}
```

Benefits of `check` include:

-   When the assertion fails, the failure message includes more context: The
    message specifies that the assertion was performed on `myProto.foo()`, and
    it includes the full value of `myProto` for reference.
-   If the user of the assertion called `assertWithMessage`, that message, which
    is lost in the `assertThat` version, is shown by the `check` version.
-   `check` makes it possible to test the assertion with [`ExpectFailure`] and
    to use [`Expect`] or [`assume`]. `assertThat`, by contrast, overrides any
    user-specified failure behavior.

[Truth]: https://github.com/google/truth
[`check`]: https://google.github.io/truth/api/latest/com/google/common/truth/Subject.html#check-java.lang.String-java.lang.Object...-
[`ExpectFailure`]: https://google.github.io/truth/api/latest/com/google/common/truth/ExpectFailure.html
[`Expect`]: https://google.github.io/truth/api/latest/com/google/common/truth/Expect.html
[`assume`]: https://google.github.io/truth/api/latest/com/google/common/truth/TruthJUnit.html#assume--

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ChainedAssertionLosesContext")` to the enclosing element.
