Providing parameter comments for boolean literals has some advantages:

*   Readability is generally improved, as the parameter name will likely provide
    some context on what the boolean literal means
*   [https://errorprone.info/bugpattern/ParameterName](ParameterName) checks at compile-time that the
    comments match the formal argument names to avoid accidentally transposing
    parameters
