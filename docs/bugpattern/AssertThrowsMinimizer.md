Prefer to minimize the amount of logic in a lambda passed to `assertThrows`.

An `assertThrows` assertion will pass if any code in the provided lambda throws
the given exception. If the lambda contains multiple expressions that may throw,
the test may incorrectly pass if an earlier expression unexpectedly throws. For
example, consider:

```java
assertThrows(
    IllegalArgumentException.class,
    () ->
        EncodingUtil.convertAudioFormatToMp3Configuration(
            AudioEncoding.newBuilder()
                .setAudioFormat(AudioFormat.AUDIO_FORMAT_MP3)
                .setAudioQuality(AudioQuality.UNRECOGNIZED)
                .build()));
```

This assertion is intended to check that
`convertAudioFormatToMp3Configuration()` throws `IllegalArgumentException`, but
the assertion will always pass because the setup logic in
`setAudioQuality(AudioQuality.UNRECOGNIZED)` is incorrect and `build()` will
throw `IllegalArgumentException`.

Instead, you should minimize the amount of logic inside of your `assertThrows`:

```java
AudioEncoding audioEncoding =
    AudioEncoding.newBuilder()
        .setAudioFormat(AudioFormat.AUDIO_FORMAT_MP3)
        .setAudioQuality(AudioQuality.UNRECOGNIZED) // BOOM goes the dynamite!
        .build();
assertThrows(
    IllegalArgumentException.class,
    () -> EncodingUtil.convertAudioFormatToMp3Configuration(audioEncoding));
```

The test above now (correctly) fails and exposes the setup issue.

## Variable types and names {#var}

This check tries to pick a reasonable name for the extracted variable, but there
may be a better name. If the extracted variable seems verbose, consider renaming
it to improve readability.

In some cases a judicious use of `var` could improve readability, following the
[guidelines for usage of `var`][lvti].

[lvti]: https://openjdk.java.net/projects/amber/guides/lvti-style-guide
