The Java `Locale` API is broken in a few ways that should be avoided, with some
examples of error prone issues below:

#### Constructors

The constructors don't validate the parameters at all, they just "trust" it
100%. \
This is also true for the static method `Locale.of`, introduced in JDK 19.

For example:

```java
Locale locale = new Locale("en_AU"); // or Locale.of("en_AU")
locale.toString();    // "en_au"
locale.getLanguage(); // "en_au"
locale.getCountry();  // ""

locale = new Locale("somethingBad#!34, long, and clearly not a locale ID");
// or Locale.of("somethingBad#!34, long, and clearly not a locale ID")
locale.toString();    // "somethingbad#!34, long, and clearly not a locale id"
locale.getLanguage(); // "somethingbad#!34, long, and clearly not a locale id"
locale.getCountry();  // ""
```

As you can see, the full string is interpreted as language, and the country is
empty.

For `new Locale("zh", "tw", "#Hant")` and `Locale.of("zh", "tw", "#Hant")` you
get:

```
toString()    : zh_TW_#Hant
getLanguage() : zh
getCountry()  : TW
getScript()   :
getVariant()  : #Hant
```

And for `Locale.forLanguageTag("zh-hant-tw")` you get a different result:

```
toString()    : zh_TW_#Hant
getLanguage() : zh
getCountry()  : TW
getScript()   : Hant
getVariant()  :
```

We can see that while the `toString()` value for both locales are equivalent,
the individual parts are different. More specifically, the first locale is
incorrect since `#Hant` is supposed to be the script for the locale rather than
the variant. \
There's no reliable way of getting a correct result through a `Locale`
constructor, so we should prefer using `Locale.forLanguageTag()` (and the IETF
BCP 47 format) for correctness.

**Note:** You might see a `.replace('_', '-')` appended to a suggested fix for
the error prone checker for this bug pattern. This is sanitization measure to
handle the fact that `Locale.forLanguageTag()` accepts the "minus form" of a tag
(`en-US`) but not the "underscore form" (`en_US`). It will silently default to
`Locale.ROOT` if the latter form is passed in.

**Note:** This error-prone rule cannot reliably fix constructors and static
method `Locale.of` with two or three parameters, because a proper fix requires
more context.

If the initial code started with a `String` that was split at `'_'` or `'-'`,
just to be used for locale, the right fix is to use `toLanguageTag()`.

```java
// Initial code
void someMethod(String localeId) {
  String[] parts = localeId.split("_");
  Locale locale = switch (parts.size) {
    case 1 -> new Locale(part[0]), // or Locale.of
    case 2 -> new Locale(part[0], part[1]), // or Locale.of
    case 3 -> new Locale(part[0], part[1], part[2]), // or Locale.of
  }
  // use the locale
}

// Fixed code
void someMethod(String localeId) {
  Locale locale = Locale.forLanguageTag.replace('_', '-');
  // use the locale
}
```

If the initial code started separate "pieces" (language, region, variant) the
right fix is to use a `Locale.Builder()`.

```java
// Initial code
void someMethod(@NotNull String langId, String regionId) {
  Locale locale (regionId == null)
      ? new Locale(langId) // or Locale.of
      : new Locale(langId, regionId); // or Locale.of
  // use the locale
}

// Fixed code
void someMethod(@NotNull String langId, String regionId) {
  Locale.Builder builder = new Locale.Builder();
  builder.setLanguage(langId);
  if (regionId == null) {
    builder.setCountry(regionId);
  }
  Locale locale = builder.build();
  // use the locale
}
```

#### toString()

This poses the inverse of the constructor problem.

```java
Locale myLocale = Locale.forLanguageTag("zh-hant-tw")
String myLocaleStr = myLocale.toString() // zh_TW_#Hant
Locale derivedLocale = ??? // Not clean way to get a correct locale from myLocaleStr
```

The `toString()` implementation for `Locale` isn't necessarily incorrect in
itself. \
It is intended to be _"concise but informative representation that is easy for a
person to read"_ (see documentation at
[Object.toString()](https://docs.oracle.com/javase/6/docs/api/java/lang/Object.html#toString\(\))).

So it is not intended to produce a value that can be turned back into a
`Locale`. It is not a serialization format. \
It often produces a value that _looks_ like a locale identifier, but it is not.
