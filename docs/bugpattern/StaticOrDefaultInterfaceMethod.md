Static and default interface methods are not natively supported on Android
versions earlier than 7.0. Enable this check for compatibility with older
devices. See [Android Java 8
Documentation](https://developer.android.com/guide/platform/j8-jack.html).


## Suppression

To declare default or static methods in interfaces, add a
`@SuppressWarnings("StaticOrDefaultInterfaceMethod")` annotation to the
enclosing element.
