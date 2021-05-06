---
title: Patching
layout: documentation
---

## Suggested Replacements

In addition to reporting errors as part of your regular compile phase, Error Prone can offer suggested replacements:

```
error: [DeadException] Exception created but not thrown
    new RuntimeException();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new RuntimeException();'?
1 error
```
Here, Error Prone is suggesting to fix this issue by prepending a `throw` keyword to the code.

While you can, of course, manually make these changes to your source code, you can also use Error Prone to modify the source code with the suggested replacements. This is useful when first adding Error Prone enforcement to an existing codebase, or for fixing warning-level issues that don't break the build (like [MissingOverride] or [DefaultCharset]).

## How to use

To apply the suggested fixes for checks built in to the Error Prone compiler, you'll add two compiler flags to your compiler invocation:

```
-XepPatchChecks:MissingOverride,DefaultCharset,DeadException
-XepPatchLocation:/full/path/to/your/source/root
```

The first flag determines which checks to try and create fixes from. If a check doesn't generally emit suggested fixes (e.g.: [InputStreamSlowMultibyteRead]), then it won't do anything here.
The second flag determines where a file named `error-prone.patch` will be emitted. This will be a unified diff patch file relative to that source root. You can inspect the patch file directly, and apply it to your source with:

```shell
cd /full/path/to/your/source/root
patch -p0 -u -i error-prone.patch
```

NOTE: This feature is experimental, and subject to change. If you have any feedback about this process, please let us know via the [Google Group][grp]

[MissingOverride]: ../bugpattern/MissingOverride
[DefaultCharset]: ../bugpattern/DefaultCharset
[InputStreamSlowMultibyteRead]: ../bugpattern/InputStreamSlowMultibyteRead
[grp]: https://groups.google.com/forum/#!forum/error-prone-discuss
