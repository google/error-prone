Querying `AnnotatedElement` (via `getAnnotation`, `isAnnotationPresent`,
`getAnnotationsByType`, `getDeclaredAnnotation`, or
`getDeclaredAnnotationsByType`) for an annotation that does not have its
`@Retention` set to `RetentionPolicy.RUNTIME` will always fail to find the
annotation (returning `null`, `false`, or an empty array).
