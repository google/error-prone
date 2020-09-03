Code that needs to be compatible with Android cannot use types or members that
only the latest or unreleased devices can handle

## Suppression

WARNING: We _strongly_ recommend checking your code with Android Lint if
suppressing or disabling this check.

The check can be suppressed in code that deliberately only targets newer Android
SDK versions.

To suppress for a particular statement, method, or class, use
`@SuppressWarnings`:

```
@SuppressWarnings("AndroidJdkLibsChecker") // TODO(user): document suppression
```
