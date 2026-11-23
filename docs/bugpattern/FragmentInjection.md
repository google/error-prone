Fragment Injection refers to a security vulnerability in classes extending
`android.preference.PreferenceActivity` wherein malicious intents can place
fragments in places they were never meant to be. In Android SDK 19 and higher,
this was patched by adding the method `isValidFragment` to verify that fragments
placed in preference activities are meant to be there before instantiating them.

The vulnerability exists for any exported preference activity targeting API
level < 19 unless you override `isValidFragment` in your activity to check that
fragments are the type that you expect. You'll probably want to do something
like this:

```java
protected boolean isValidFragment(String fragmentName) {
  return MyFragment.class.getName().equals(fragmentName);
}
```

This check emits a warning if isValidFragment is not implemented on classes
extending PreferenceActivity, or if the implementation of isValidFragment
returns true on all code paths[^1].

If you are targeting API level >= 19, or if `exported` is set to `false` in your
`Manifest.xml`, you are probably safe[^2], but it is even better to implement
`isValidFragment` anyway in case of future changes to your manifest, or other
eventualities.

For more info:

*   https://securityintelligence.com/new-vulnerability-android-framework-fragment-injection/
*   https://support.google.com/faqs/answer/7188427

[^1]: Your method implementation may be safe if you throw a runtime exception
    instead of returning false for a fragment, but you will still get a
    warning. You may want to change your implementation to return false
    instead. This way your app will still be safe, and won't crash.
[^2]: For API levels >= 19, the default implementation throws a runtime
    exception, so it is "safe", but probably not the behavior you want. If
    your activity is not exported, intents outside your app should not be able
    to attempt to show any fragments, and therefore isValidFragment should not
    be called.
