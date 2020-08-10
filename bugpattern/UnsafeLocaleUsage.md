---
title: UnsafeLocaleUsage
summary: Possible unsafe operation related to the java.util.Locale library.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The Java `Locale` API is broken in a few ways that should be avoided, with some
examples of error prone issues below:

#### Constructors

The constructors don't validate the parameters at all, they just "trust" it
100%.

For example:

```
Locale locale = new Locale("en_AU");
toString()        : "en_au"
getLanguage()     : "en_au"
locale.getCountry : ""

locale = new Locale("somethingBad#!34, too long, and clearly not a locale ID");
toString()    : "somethingbad#!34, too long, and clearly not a locale id"
getLanguage() : "somethingbad#!34, too long, and clearly not a locale id"
getCountry()  : ""
```

As you can see, the full string is interpreted as language, and the country is
empty.

For `new Locale("zh", "tw", "#Hant")` you get:

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

**Note:** You might see a `.replace("_", "-")` appended to a suggested fix for
the error prone checker for this bug pattern. This is sanitization measure to
handle the fact that `Locale.forLanguageTag()` accepts the "minus form" of a tag
(`en-US`) but not the "underscore form" (`en_US`). It will silently default to
`Locale.ROOT` if the latter form is passed in.

#### toString()

This poses the inverse of the constructor problem

```
Locale myLocale = Locale.forLanguageTag("zh-hant-tw")
String myLocaleStr = myLocale.toString() // zh_TW_#Hant
Locale derivedLocale = ??? // Not clean way to get a correct locale from this string
```

The `toString()` implementation for `Locale` isn't necessarily incorrect in
itself. \
It is intended to be _"concise but informative representation that is easy for a
person to read"_ (see documentation at
[Object.toString()](https://docs.oracle.com/javase/6/docs/api/java/lang/Object.html#toString\(\))).

So it is not intended to produce a value that can be turned back into a
`Locale`. It is not a serialization format. \
It often produces a value that _looks_ like a locale identifier, but it is not.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnsafeLocaleUsage")` to the enclosing element.
