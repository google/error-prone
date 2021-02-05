---
title: CheckedExceptionNotThrown
summary: This method cannot throw a checked exception that it claims to. This may
  cause consumers of the API to incorrectly attempt to handle, or propagate, this
  exception.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Java allows methods to declare that they throw checked exceptions even when they
don't. This can lead to call sites beng forced to explicitly handle or propagate
exceptions which provably can never occur. It may also lead readers of the code
to search for possibly throwing paths where none exist.

```java
private static void validateRequest(Request request) throws IOException {
  checkArgument(request.hasFoo(), "foo must be specified");
}

Response handle(Request request) {
  try {
    validateRequest(request);
  } catch (IOException e) { // Required, but unreachable.
    return failedResponse();
  }
  // ...
}
```

Including unthrown exceptions can be reasonable where the method is overridable,
as overriding methods will not be able to declare that they throw any exceptions
not included.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CheckedExceptionNotThrown")` to the enclosing element.
