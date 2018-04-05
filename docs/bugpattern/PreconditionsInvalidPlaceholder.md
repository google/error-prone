The Guava Preconditions checks take error message template strings that look
similar to format strings but only accept %s as a placeholder. This check points
out places where there is a non-%s placeholder in a Preconditions error message
template string and the number of arguments does not match the number of %s
placeholders.
