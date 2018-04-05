Comparing strings with == is almost always an error, but it is an error 100% of
the time when one of the strings is a protobuf field. Additionally, protobuf
fields cannot be null, so Object.equals(Object) is always more correct.
