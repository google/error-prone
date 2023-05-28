`String.toLowerCase()` (and `toUpperCase`) without specifying a `Locale` can
have surprising results.

For example, if this is used on a device and the user's `Locale` is Türkiye,
then `"I".toLowerCase()` will yield a lowercase dotless I ("ı"). This could be
extremely dangerous if you were expecting to operate on ASCII text to generate
machine-readable identifiers.

If this kind of regionalisation is desired, use
`.toLowerCase(Locale.getDefault())` to make that explicit. If not,
`.toLowerCase(Locale.ROOT)` or `.toLowerCase(Locale.ENGLISH)` will give you
casing independent of the user's current `Locale`. If you know that you're
operating on ASCII, prefer `Ascii.toLower/UpperCase` to make that explicit.
