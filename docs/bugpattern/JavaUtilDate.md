The `Date` API is full of
[major design flaws and pitfalls](https://codeblog.jonskeet.uk/2017/04/23/all-about-java-util-date/)
and should be avoided at all costs. Prefer the `java.time` APIs, specifically,
`java.time.Instant` (for physical time) and `java.time.LocalDate[Time]` (for
civil time).
