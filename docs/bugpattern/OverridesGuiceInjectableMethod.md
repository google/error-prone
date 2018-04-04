Unlike with `@javax.inject.Inject`, if a method overrides a method annotated
with `@com.google.inject.Inject`, Guice will inject it even if it itself is not
annotated. This differs from the behavior of methods that override
`@javax.inject.Inject` methods since according to the JSR-330 spec, a method
that overrides a method annotated with `@javax.inject.Inject` will not be
injected unless it iself is annotated with `@Inject`. Because of this
difference, it is recommended that you annotate this method explicitly.
