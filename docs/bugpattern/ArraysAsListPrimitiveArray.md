Arrays.asList does not autobox primitive arrays, as one might expect. If you
intended to autobox the primitive array, use an asList method from Guava that
does autobox. If you intended to create a singleton list containing the
primitive array, use Collections.singletonList to make your intent clearer.
