According to the Javadoc of `java.util.TimeZone`:

> For compatibility with JDK 1.1.x, some other three-letter time zone IDs (such
> as "PST", "CTT", "AST") are also supported. However, their use is deprecated
> because the same abbreviation is often used for multiple time zones (for
> example, "CST" could be U.S. "Central Standard Time" and "China Standard
> Time"), and the Java platform can then only recognize one of them.

Aside from the ambiguity between timezones, there is inconsistency in the
observance of Daylight Savings Time for the returned time zone, meaning the
`TimeZone` obtained may not be what you expect. Examples include:

*   `DateTime.getTimeZone("PST")` does observe daylight savings time; however,
    the identifier implies that it is Pacific *Standard* Time, i.e. daylight
    savings time is not observed.
*   `DateTime.getTimeZone("EST")` (and `"MST"` and `"HST"`) do not observe
    daylight savings time. However, this is inconsistent with PST (and others),
    so you may believe that daylight savings time will be observed.

This check will only suggest replacements which yield the same rules as the
existing three-letter ID for at least part of the year (e.g. it will suggest
"America/Chicago" and "Etc/GMT+6" but not "Asia/Shanghai" as a replacement for
"CST").

Certain 3-letter time zone IDs are not flagged by this check, specifically if
the ID appears in `ZoneId.getAvailableZoneIds()`, e.g. "UTC", "GMT", "PRC".
