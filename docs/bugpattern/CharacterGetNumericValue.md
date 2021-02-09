`Character.getNumericValue` has unexpected behaviour: it interprets A-Z as
base-36 digits with values 10-35, but also supports non-arabic numerals and
miscellaneous numeric unicode characters like ㊷. For example:

*   `Character.getNumericValue('V' /* ASCII V */) == 31`
*   `Character.getNumericValue('Ⅴ' /* U+2164, Roman numeral 5 */) == 5`
*   `Character.getNumericValue('௧' /* U+0BF2, Tamil Digit One */) == 1`
*   `Character.getNumericValue('௲' /* U+0BF2, Tamil Number One Thousand */) ==
    1000`
*   `Character.getNumericValue('㊷' /* U+32B7, Circled Number Forty Two */) ==
    42`

[`UCharacter.getNumericValue`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/lang/UCharacter.html#getNumericValue-int-)
has the same behavior.

Consider using:

*   [`UCharacter.getUnicodeNumericValue`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/lang/UCharacter.html#getUnicodeNumericValue-int-):
    Handles all unicode codepoints with numeric values including fractions,
    roman numerals, and other miscellaneous numeric characters. Returns the
    value as a double. Does not assign a value to A-Z.
*   [`Character.digit`](https://docs.oracle.com/javase/8/docs/api/java/lang/Character.html#digit-char-int-):
    Handles unicode codepoints in the "decimal digit" category and the letters
    A-Z which are interpreted as base-36 digits with values 10-35. Does not
    handle characters like roman numerals and ㊷. You can use a `radix` value of
    10 or less to avoid interpreting A-Z as digits.
