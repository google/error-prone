The Guava Preconditions checks take error message template strings that look
similar to format strings, but only accept the %s format (not %d, %f, etc.).
This check points out places where a Preconditions error message template string
has a non-%s format, or where the number of arguments does not match the number
of %s formats in the string.
