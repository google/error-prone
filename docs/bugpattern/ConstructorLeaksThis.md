During the execution of a constructor, it's dangerous to make the new instance
accessible to other code. Fields of the instance, including `final` fields, may
not yet be initialized, and executing instance methods may yield unexpected
results.

This advice applies not only to constructors per se, but also to instance
variable initializers and instance initializer blocks.

The issue `ConstructorInvokesOverridable` is closely related.

## Avoiding the warning

One common reason for constructors to pass `this` to other code is to register
the new instance as a listener on some other object. (This pattern is especially
common in UI code.) This runs the risk that the listener method will be invoked
before construction is complete. Further, it means that the constructor has side
effects -- it modifies the object that's being listened to.

To avoid these risks, it's best to factor the listener registration out into
another method, for example a factory method taking the same parameters as the
constructor. The factory method can instantiate the new object and then perform
the registration safely.
