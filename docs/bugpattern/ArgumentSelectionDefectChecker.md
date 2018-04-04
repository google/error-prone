If permuting the arguments of a method call means that the argument names are a
better match for the parameter names than the original ordering then this might
indicate that they have been accidentally swapped. There are also legitimate
reasons for the names not to match such as when rotating an image (swap width
and height). In this case we suggest annotating the names with a comment to make
the deliberate swap clear to future readers of the code. Argument names
annotated with a comment containing the parameter name will not generate a
warning.
