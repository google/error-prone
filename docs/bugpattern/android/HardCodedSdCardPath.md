Your code should not reference the `/sdcard` path directly, which is
platform-dependent. You should use
`Environment.getExternalStorageDirectory().getPath()` instead.

Similarly, do not reference the `/data/data/` path directly, as it can vary
in multi-user scenarios. You should use `Context.getFilesDir().getPath()`
instead.

For more information, please see the documentation for [android.os.Environment]
(http://developer.android.com/reference/android/os/Environment.html)
and [android.content.Context]
(http://developer.android.com/reference/android/content/Context.html).

This check is an adaptation of the `SdCardPath` rule of [Android Lint]
(http://tools.android.com/tips/lint-checks).
