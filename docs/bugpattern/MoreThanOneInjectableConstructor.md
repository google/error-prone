Injection frameworks may use `@Inject` to determine how to construct an object
in the absence of other instructions. Annotating `@Inject` on a constructor
tells the injection framework to use that constructor. However, if multiple
`@Inject` constructors exist, injection frameworks can't reliably choose between
them.
