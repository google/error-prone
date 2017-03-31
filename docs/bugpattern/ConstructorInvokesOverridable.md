As Effective Java §17 warns: "Constructors must not invoke overridable methods".
The risk is that overrides of these methods in subclasses will observe the new
instance in an incompletely-constructed state. (Subclass state will certainly be
uninitialized, and base class state may be incomplete as well.)

This advice applies not only to constructors per se, but also to instance
variable initializers and instance initializer blocks.

The issue `ConstructorLeaksThis` is closely related.

## Avoiding the warning

If your constructor invokes a class method, and you don't intend it to be
overridden, mark the method private or final. (Its implementation will still
observe the instance in an incomplete state, so take care that all fields are
initialized first.)

If you need to invoke subclass logic as part of initialization, either put it in
the subclass constructor, or invoke it outside the constructor altogether. For
example, wrap the `new` call in a factory method and invoke the overridable
method afterward.
