When classes declare that they have an `@javax.inject.Inject`ed method,
dependency injection tools must call those methods after first calling any
`@javax.inject.Inject` constructor, and performing any field injection. These
methods are part of the initialization contract for the object.

When subclasses override methods annotated with `@javax.inject.Inject` and
*don't* also annotate themselves with `@javax.inject.Inject`, the injector will
not call those methods as part of the subclass's initialization. This may
unexpectedly cause assumptions taken in the superclass (e.g.: this
post-initialization routine is finished, meaning that I can safely use this
field) to no longer hold.

This compile error is intended to prevent this unintentional breaking of
assumptions. Possible resolutions to this error include:

*   `@Inject` the overridden method, calling the `super` method to maintain the
    initialization contract.
*   Make the superclass' method `final` to avoid subclasses unintentionally
    masking the injected method.
*   Move the initialization work performed by the superclass method into the
    constructor.
*   Suppress this error, and very carefully inspect the initialization routine
    performed by the superclass, making sure that any work that needs to be done
    there is done in an @Inject method in the subclass. You may want to refactor
    portions of the body of the superclass method into a `protected` method for
    this subclass to use.
