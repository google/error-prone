"YYYY" in a date pattern means "week year". The week year is defined to begin at
the beginning of the week that contains the year's first Thursday. For example,
the week year 2015 began on Monday, December 29, 2014, since January 1, 2015,
was on a Thursday.

"Week year" is intended to be used for week dates, e.g. "2015-W01-1", but is
often mistakenly used for calendar dates, e.g. 2014-12-29, in which case the
year may be incorrect during the last week of the year. If you are formatting
anything other than a week date, you should use the year specifier "yyyy"
instead.

This isn't an idle risk; Twitter had a
[significant outage](https://web.archive.org/web/20150711045621/http://tech.firstpost.com/news-analysis/twitter-suffers-massive-outage-on-all-online-platforms-back-now-247196.html)
in ~~2015~~ 2014
[due to this bug](https://xcancel.com/jmhodges/status/549430032616017921).
