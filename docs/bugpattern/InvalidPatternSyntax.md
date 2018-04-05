This error is triggered by calls to regex-accepting methods with invalid string
literals. These calls would cause a PatternSyntaxException at runtime.

We deliberately do not check java.util.regex.Pattern#compile as many of its
users are deliberately testing the regex compiler or using a vacuously true
regex.
