Certain resources in `android.R.string` have names that do not match their
content: `android.R.string.yes` is actually "OK" and `android.R.string.no` is
"Cancel". Avoid these string resources and prefer ones whose names *do* match
their content. If you need "Yes" or "No" you must create your own string
resources.
