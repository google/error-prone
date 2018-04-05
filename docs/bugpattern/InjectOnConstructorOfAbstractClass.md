When dependency injection frameworks call constructors, they can only do so on
constructors of concrete classes, which can delegate to superclass constructors.
In the case of abstract classes, their constructors are only called by their
concrete subclasses, not directly by injection frameworks, so the `@Inject`
annotation has no effect.
