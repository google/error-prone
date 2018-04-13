When a class exposes multiple constructors, they're generally used as a means of
initializing default parameters. If a chaining constructor ignores a parameter,
it's likely the parameter needed to be plumbed to the chained constructor.

```java
MissileLauncher(Location target) {
  this(target, false);
}
MissileLauncher(boolean askForConfirmation) {
  this(TEST_TARGET, false); // should be askForConfirmation
}
MissileLauncher(Location target, boolean askForConfirmation) {
   ...
}
```
