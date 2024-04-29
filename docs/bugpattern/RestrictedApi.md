Calls to APIs marked `@RestrictedApi` are prohibited without a corresponding
allowlist annotation.

The intended use-case for `@RestrictedApi` is to restrict calls to annotated
methods so that each usage of those APIs must be reviewed separately. For
example, an API might lead to security bugs unless the programmer uses it
correctly.

See the
[javadoc for `@RestrictedApi`](https://errorprone.info/api/latest/com/google/errorprone/annotations/RestrictedApi.html)
for more details.
