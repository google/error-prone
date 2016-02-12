Every subclass of `Fragment` must be public and have a public, nullary 
constructor. The Android framework will reflectively instantiate them after
a configuration change, such as screen rotation, and if the class is not
instantiable by `Class#newInstance()`, an `InstantiationException` will be
thrown.

In addition, it is strongly recommended that subclasses not have other
constructors with parameters, since these constructors will not be called 
when the fragment is re-instantiated; instead, arguments should be supplied 
with `setArguments(Bundle)` and retrieved with `getArguments()`.
 

For more information, please see the documentation for [Fragment]
(http://developer.android.com/reference/android/app/Fragment.html#Fragment()).

This check is an adaptation of the `ValidFragment` rule of [Android Lint]
(http://tools.android.com/tips/lint-checks).
