The presence of an unused method may indicate a bug. This check highlights
_private_ methods which are unused and can be safely removed without considering
the impact on other source files.

## Suppression

All false positives can be suppressed by annotating the method with
`@SuppressWarnings("unused")` or prefixing its name with `unused`.
