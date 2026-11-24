A `java.time.Period` represents a distance of civil time in years, months, and
days. For example, a period could represent "1 month, 2 days", or another period
could represent "32 days".

The method `Period.getDays()` only extracts the "days" portion of the Period, so
a period representing "1 month, 2 days" would return 2 from its `getDays()`.

In many circumstances, developers use `period.getDays()` when they think it
represents "the total days" in the period, especially when the Period is
computed using `LocalDate.until(LocalDate)`.

In this instance, a developer has used `period.getDays()` without consulting
either the `months` or `years` portion of the Period. In all likelihood, the
developer would be better suited by using
`org.threeten.extra.Days.between(LocalDate, LocalDate)` to compute the number of
days between two dates.

For example:

```java
LocalDate day1, day2; ...
if (day1.until(day2).getDays() > 31) { // No more than 31 days between them
  // Oops, this is always false, even if day1 is 4 "months" behind
}

if (Days.between(day1, day2).compareTo(Days.of(31)) > 0) {
  // Can actually be executed!
}
```
