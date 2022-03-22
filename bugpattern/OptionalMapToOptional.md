---
title: OptionalMapToOptional
summary: Mapping to another Optional will yield a nested Optional. Did you mean flatMap?
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using `Optional#map` (or `Optional#transform`) to map to another `Optional`
might indicate an error, or at least quite hard-to-reason-about code that may
turn into an error.

For example,

```java
class AccountManager {
  /** Retrieves the current user, or absent if not logged in. */
  Optional<Account> getUser() {
    ...
  }

  /**
   * Returns an administrative token for the user, or absent if they do not have
   * such privileges.
   */
  Optional<Token> getAdminToken() {
  }

  void doScaryAdminThings() {
    if (getUser().map(AccountManager::getAdminToken).isPresent()) {
      // do privileged things.
    }
  }
}
```

In this case, assuming `getAdminToken` does not throw, the conditional is
equivalent to `getUser().isPresent()`, which is not what was intended.
`getUser().flatMap(AccountManager::getAdminToken).isPresent()` would be correct.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OptionalMapToOptional")` to the enclosing element.
