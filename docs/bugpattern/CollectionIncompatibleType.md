Various Collections APIs have methods that take `Object` rather than the type
parameter you would expect. If an argument is given whose type is incompatible
with the appropriate type argument, it's unlikely that the method call is
going to do what you intended.

To learn why Collections APIs have methods that take `Object`, see Kevin
Bourillion's blog post, "[Why does Set.contains() take an Object, not an E?]
(http://smallwig.blogspot.com/2007/12/why-does-setcontains-take-object-not-e.html)".
