Using `Optional#map` (or `Optional#transform`) to map to another `Optional`
might indicate an error, or at least quite hard-to-reason-about code that may
turn into an error.

For example,

```java {.bad}
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
