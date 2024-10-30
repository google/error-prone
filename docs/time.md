---
title: How to Think About Time
layout: documentation
---


*for programmers, and people who like to know things*

Author: [Kevin Bourrillion](https://kevinb9n.github.io/)

> Time is what keeps everything from happening at once. —Ray Cummings

It's hard to write much code without encountering the need for *when*.

We might need to record when something happened. Or dictate when something
should happen. Or give a range during which the thing can happen. Or in some
other way, our code needs to deal with **dates and times**.

Fortunately, we learned date and time concepts when we were very young. It
wasn't that long after we learned our animals and our colors. And we've been
speaking about these things all our lives, so... this should be easy, right?

(Narrator: it was not.)

Suddenly, bugs and confusion. You might blame it on your libraries -- and
honestly, some deserve it. But the fact is, the subject is just extremely
complicated. Now that we're talking to a computer rather than a person, we need
to be far more precise, and far more "correct" about things, and this changes
the whole game. We thought we understood this stuff, yet somehow now we are
writing bug after bug into our programs.

Eventually we might say to ourselves, "Wait, I need to pause and go learn how
dates and times *really* work." But then we discover to our surprise that there
is no end to the subject, that the exceptions go on and on, and it is in fact
more confusing and complicated than just about anything we've ever tried to
learn.

Uh oh.

## What is this document? {#this_document}

The Java And Kotlin Ecosystem team at Google has worked for several years to
eliminate the sources of date/time bugs in Google's codebase. We've learned a
lot from this.

But my goal here is not to provide a laundry list of programming practices. It's
more basic than that. I hope to provide a solid "conceptual model" for how we
think about date and time concepts in the most helpful way. I'm shooting for it
to be understandable, usable, and to avoid saying things that are egregiously
wrong.

**So!**

Just [forget everything you know](https://www.youtube.com/watch?v=kO_x6rJJdsY)
about dates and times, and we'll build it up again together from the ground up.

Let's begin!

## Instants and the timeline {#timeline}

*Record scratch*

But wait. What does it mean to "begin"?[^begin]

[^begin]: Wait. What does it mean to "wait"? FYI, I use footnotes in this doc,
    and you really, *really* don't have to read them. I assure you that
    every one is absolutely nonessential reading. It will *feel* like you
    have to follow those little high up numbers where they lead, every
    time, but I'm telling you. As an example, consider how useless this
    one is. *Sometimes* you'll be reading and you'll be distracted by that
    "hey, what about..." feeling, and those are the kinds of places where
    I try to leave a footnote. But if you weren't distracted then don't
    let the little number be the thing that distracts you.

The way we picture time is as a single line stretching out in both directions;
an endless ordered stream of fleeting moments, moving ever forward in what
physicists call the
"[direction of increasing entropy](https://www.youtube.com/watch?v=i6rVHr6OwjI)".

This is the [timeline](https://i.imgur.com/WnvNLTa.jpeg).

![A wide arrow points rightward; a tick mark in the middle is labeled "now"; the
span to the left and right are respectively labeled "the past" and "the
future"](timeline.svg "The timeline") The timeline. We envision the past on the
left and the future on the right because we have cultural bias.
{@paragraph .figure}

Every event that has happened or will happen in history is located at a
particular point on this line — or had a *start* and *end* that can be so
located. For example, when you were born, or when you first opened this guide,
or when you will close it again in disappointment.[^never] These points on the
timeline are called **instants**.

[^never]: Events such as when I might be expected to give you up are nowhere to
    be seen.

In this simplified view[^relativity], an instant is a universal thing, the same
for everyone everywhere. And in practical terms it is, nearly enough, at least
for everyone on the surface of the Earth. For example, if the entire Earth
suddenly vanished in a puff of space dust, that is something we would all
experience together at the same *instant.*[^puff] (Different people will
*describe* that instant differently — for example, "it was a dark and stormy
night when..." — but later for that.)

[^relativity]: In 1905 a Swiss patent clerk took what had been a beautifully
    simple Newtonian model of time and really trashed it to hell.
    "What if time is just more space," he said, and when no one at
    the party would listen, he wrote it down. Time has been weird
    ever since. But! To reason well about time for our software, it
    helps *a lot* if we can ignore relativity — and in this guide
    we're going to do just that. IF YOU ARE PROGRAMMING SPACECRAFT,
    or maybe anything that communicates with spacecraft, please set
    this guide down carefully. Please go learn about time somewhere
    else such as a graduate school. Or if you decide to stay and
    trust this guide anyway, please send me an email and let me know
    whose spaceships never to travel on.

The timeline is the basis for everything that follows in this guide, but it does
very little on its own. It does lets us ponder a few useful concepts, like
"now", "after", "start", and "early". And it gives tense to our languages:
present, past, future, and somehow "pluperfect".

<!--
  n.b. GitHub pages does not render a
  sequence of interleaved footnotes correctly; it instead drops the last footnote
  We break them up. This one is referenced two paragraphs hence.
-->

[^puff]: I would like to think that this shared experience would at last bring
    us closer together as a global community in our final moments of life.

But how do we even *talk about* one particular instant, as something distinct
from all the other instants? Identifying an instant turns out to be sort of
impossible! We can't very well say "the instant 37", or "the instant Craig", or
"that instant right there, no not *that* one, look where I'm pointing".

Let's move on to *durations* and come back to that.

## Durations {#durations}

A duration is the *difference* between any two instants on the timeline. A
duration *always* has a value measured in **seconds**. At least, that is the
official unit that humans can agree on. (Units like the "hour", "nanosecond",
and "microfortnight" do exist as well, but these are always defined in *terms*
of the second.)

The second has a precise definition, established by the 1967 General Conference
on Weights and Measures (CGPM), which you don't really need to worry about right
now.[^second]

[^second]: But if your hunch is that it's the duration in which the radiation
    corresponding to transiting the two hyperfine levels of the ground
    state of the caesium-133 atom oscillates 9,192,631,760 times, you are
    sadly mistaken. It's the duration in which the radiation
    corresponding to transiting the two hyperfine levels of the ground
    state of the caesium-133 atom oscillates 9,192,631,770 times.

![A wide arrow points rightward; a tick mark on the left is labeled "play 'Free
Bird'"; one on the right is labeled "'Free Bird' ends". The span between them is
labeled "549 seconds".](duration.svg "A duration") An example of a duration.
Durations are objective and finite, however it might seem at the time.
{@paragraph .figure}

**Warning:** in this context, units like "minute" are fine, but larger units
like "month" or "year" *have no precise meaning*. You should avoid them, but
it's also reasonable to use one for *approximate* descriptions only. Someone has
to choose an arbitrary value for it, in terms of one of the precise units (for
example, for a year, it might use 525,600 minutes). These units from "day" on up
will be far more relevant to "civil" time, which we'll get to shortly.

## Negative durations? {#negative_duration}

Can a duration be negative? We would rather you had not asked.

Yeah, technically it can. In fact, in a variety of ways time behaves completely
[symmetrically](https://en.wikipedia.org/wiki/T-symmetry#Even).[^invertible] For
example, the duration from when I arrived at work *until* I left home this
morning was *negative* 25 minutes.

[^invertible]: also electrical current, but not length, mass, temperature, or
    luminosity. With those we might still use a negative value to
    express a *reduction* in some other measurement, but we can only
    decrease that value so far until it hits zero. Negative mass or
    temperature is the subject of science fiction and clickbait
    headlines. (Incidentally, for electrical current we chose
    positive vs. negative arbitrarily, and managed to
    [get it wrong](https://xkcd.com/567/).)

However, this is basically playing word games. We are never going to *actually*
spend a negative duration of time doing anything in our lives if we are not a
character in the movie [*Primer*](https://reelgood.com/movie/primer-2004).

I feel a warning coming on.

**Warning:** the vast majority of code that works with a duration will
implicitly *expect* it to be nonnegative. By permitting negative durations in
your code you'll run the risk of violating those expectations and triggering
puzzling bugs. I recommend avoiding negative durations as much as possible. It's
not that there are *no* valid reasons to use one; it's just that the primary
reason is if you don't like your teammates.

## Math operations {#physical_math}

Instants and durations have some things in common; for example, they both have a
strong ordering, with "less-than" meaning "before" or "shorter than",
respectively.

But they are otherwise quite different. In particular, an instant is not a
*quantity*. A duration is.

More concretely: the set of mathematical operations instants and durations can
participate in looks like this:[^vector]

*   instant − instant = duration
*   instant ± duration = instant
*   duration ± duration = duration
*   duration ×÷ real number = duration

Note in particular that you cannot add two instants.

[^vector]: Mathematically, we say that a durations are *vectors*, and instants
    form an "affine space" over these vectors, and this is what produces
    the relationships seen here. Then the person we say this to backs
    away slowly.

(You might notice that these rules work perfectly fine for negative durations.
Well, we did say negative durations are perfectly well-defined. The fact remains
that they're unexpected and bug-prone.)

## Identifying a particular instant {#identifying_instant}

The only way to refer to any particular instant involves using durations.

Instants, as mentioned above, are hard to talk about, but durations are easy:
it's just "so many" seconds. So there's an easy solution: as long as we can
agree on some *reference* instant, we can identify any instant by saying at what
duration before or after the reference instant it happens. For example, we might
describe the instant a runner crossed the finish line by saying it was "9.58
seconds after the instant the starting pistol was fired" — as long as the firing
of that pistol is a well-understood point in time for everyone communicating.

It's not necessary for *everyone* to agree on a single universal reference
instant, only that the parties communicating at any particular time agree on the
same one.

In software, in today's global world, it's very useful to have one standard
reference instant defined for use throughout an entire codebase. We call that
instant (strangely) the "epoch", but more on that later.

## Physical time {#physical_time}

Now it's time to talk about *physical time*.

Actually, physical time is what we have been talking about all along!

Physical time is concerned *only* with durations (as measured in seconds), and
instants (as described by the duration since a reference instant). And nothing
else.

This is time "as it really exists" in our (still fictionally Newtonian)
universe. All events happen at some particular instant on our timeline, or begin
and end at distinct instants. And this is true whether anyone was ever around to
observe it or not: nothing about physical time actually depends on humans at
all.[^humans]

[^humans]: Which is good, because have you seen us lately? And sure, humans did
    define the unit of the "second". But that's just arbitrary, to serve
    our own need to communicate. We could have chosen *anything*
    convenient, like the Planck time or the half-life of tellurium-128.

But notice that very few of the time concepts you know even exist in the realm
of physical time. We have the concepts "now", "before", and "after", yet
essential terms like "tomorrow", "noon", and "Taco Tuesday" have no meaning. I
don't want to live in a world without Taco Tuesday, so let's read on.

## Civil time {#civil_time}

With the use of a reference instant, physical time became *possible* to talk
about... yet it's still way too *hard* to talk about.

And so we invented civil time!

We did an interesting job of it.

Civil time is the patchwork of made-up human concepts such as "months", "common
era", "leap days", "weekend", "cold November rain", and so on.

Civil time is very important. It makes communication easier, and enables us to
organize our daily lives in a reasonable way. And no one is going to bake you
any special cake or sing you any excruciatingly bland song off-key while you
exhale rapidly onto flaming wax if we don't have civil time.

Civil time always requires a calendar system.

## Calendar systems {#calendars}

There are numerous competing schemes for how to express civil time. Each is
called a **calendar system**. Each of them accomplishes the worthy goal of
simplifying our lives, by being itself stupendously complicated.[^complicated]

[^complicated]: They're not all complicated. There are some wonderfully logical
    calendar systems like the
    [International Fixed Calendar](https://en.wikipedia.org/wiki/International_Fixed_Calendar),
    some of which I think have a really good chance of catching on
    among many of the world's Esperanto speakers.

Of all these, we are going to talk only about the **Gregorian calendar**.

## The Gregorian calendar {#gregorian}

We're focusing only on the Gregorian calendar in this guide because (a) it is
the [official](https://en.wikipedia.org/wiki/Civil_calendar) civil calendar for
countries encompassing approximately 94%[^six] of the world population, and (b)
it's already complicated enough. For most readers, it's the calendar system you
already know and loathe!

[^six]: Yes, the other 6% are important too, but we have to start somewhere. By
    all means, a guide to the other calendar systems would be interesting
    and sometimes useful, but this isn't it.

In this system:

*   A **date** is a triplet consisting of:

    *   **year**: any integer[^year]
    *   **month**: an integer from 1 to 12, inclusive (may be *represented* by
        local names, such as "enero" to "diciembre")
    *   **day of month**: an integer 1–31 (with not all values being used in
        every month)

*   A **time of day** is a triplet consisting of:

    *   **hour**: an integer 0–23
    *   **minute**: an integer 0–59
    *   **second**: a decimal value 0–59.999… (*usually*; you can read about
        [leap seconds](#leap_seconds) if interested)

[^year]: What we (and astronomers) call the years "0" and "-1" are called "1
    BCE" and "2 BCE" (respectively) by historians and middle school
    teachers. They're not wrong, but our way makes math easier: we can
    subtract by subtracting.

<!--
  n.b. GitHub pages does not render a
  sequence of interleaved footnotes correctly; it instead drops the last footnote
  We break them up.
-->

We'll define a **datetime**[^datetime] to be the sextuplet of all the preceding
values: the combination of a date with a time of day.

[^datetime]: Not an officially recognized term, but it should be. Warning: if
    you've used Joda-Time you've seen that it defines a `DateTime`
    class. That is not a datetime, but a
    "[zoned instant](#zoned_datetime)". Several libraries have done
    similar things, but we are reclaiming the term!

These six fields are enough to let people communicate properly with each other
about *most* things — provided that those people share enough context to agree
on what the values mean.

The Gregorian Calendar defines the legal values these fields can take on. Since
this calendar system was adopted piecemeal over a period of hundreds of years,
the calendar system you're probably actually using when you write code is the
"[Proleptic Gregorian Calendar](https://en.wikipedia.org/wiki/Proleptic_Gregorian_calendar)",
which is a convenient fiction that retroactively applies today's Gregorian
Calendar throughout all of history. This is the calendar system you want to use
as long as you never need precise handling of dates more than a century in the
past.

## What about the time zone? {#time_zone_is_uncivil}

(Hey, didn't we agree at the top to "forget everything we know"?)

I've intentionally not brought time zones into the picture yet. **A time zone is
not the seventh field of a civil datetime.** It is not a concept in civil time
at all (and it *certainly* isn't in physical time either).

Civil time is still highly useful without it, and you probably use it constantly
in this way. For example, my children and I are perfectly capable of discussing
when I should pick them up from school even if they had never *heard* of time
zones. The language that discussion happens in is civil time.

Unfortunately, we'll discuss time zones soon.

## Periods {#periods}

A period expresses the difference between two datetimes. Like a datetime,
therefore, it's measured in separate units of years, months, days, hours,
minutes, and seconds. Example periods: "100 years", "127 hours", or "5 months, 2
weeks, 2 days".

This completes an analogy: As duration is to instant (in the realm of physical
time), so period is to datetime (in the realm of civil time).

![Two cloud shapes. One, labeled "physical time", contains "instant" and
"duration" with arrows going from one to the other and back. The other, labeled
"civil time", contains "datetime" and "period" in the same positions, with the
same arrows.](analogy.svg "An analogy")
{@paragraph .figure}

The key difference to remember between durations and periods is that durations
are great, while periods are awful. Rephrased: durations are simple and precise;
periods are complex and vague. Just look at how red this document gets now.

**Warning:** People tend to use similar words when talking about durations as
when talking about periods. This is dangerous! The two are fundamentally
different in subtle but important ways (as this document will continue to try to
explain).

## Civil math {#civil_math}

Of course, mathematical operations on civil time work in a corresponding way to
their analogues in physical time, but with an important difference.

*   datetime ± period = datetime
*   period ± period = period
*   period × integer = period
*   datetime − datetime = period (but this produces a complex period)

The important difference is that these operations are not well-defined!

For example, what is "January 30, 2019" plus the period "one month"? (No,
really: think about it and decide what you think the answer should be!) You
might reasonably have said it's February 28, or March 1, or March 2, or perhaps
something else. There is simply no "one best" formula to follow.

**Warning:** This kind of math may violate expectations in many ways. For
example, if you remember your "associative property" from your school days,
guess what? Give it back. You can't have it. The calculation "(today + 1 month)
+ 1 month" might not give the same value as "today + (1 month + 1 month)". And
there are many other examples.

**Warning:** Civil math can also produce technically-correct results that are
still wrong for your purposes (the worst kind of correct). What is the period
between the times of "1:30 am" and "3:30 am" on the date "2019-03-10"? In the
realm of civil time, this is a period of two hours. That's a clear and
unambiguous and correct answer, which is more than we can usually expect from
period math. Nevertheless it is probably not the question you meant to ask! The
answer of two hours in no way implies that two hours' *duration* (physical time)
will transpire between those times for anyone; they might have to change their
clocks in the interim. In short, even though civil time does have its uses,
beware of confusing it with physical time.

This is the major point of difference between physical and civil time. Physical
time is well-defined mathematically, which makes it easy to manipulate. Civil
time obeys no known mathematical laws.

## Bad Periods {#bad_periods}

Like durations, periods *can* be negative, but please don't. It's especially
bad, here. But also...

**Warning:** Avoid **multi-unit periods** if at all possible. They compound the
confusion of several periods into one. You can reasonably claim there is a
period representing "+2 months, -9 weeks, +1 day", but what *is* that? Is it
even positive?

## Time zones {#time_zones}

By now we have covered a basic understanding of both
[physical time](#physical_time) and [civil time](#civil_time), and we can see
that they are quite distinct concepts. Civil time has conversational meaning to
humans; physical time more directly models objective reality.

But if they're separate, then we've certainly got to have some way to get back
and forth between the two. *Now* enter the time zone.

A **time zone** is a set of rules for how to convert *in both directions*
between an instant (physical time) and a corresponding datetime (civil time).

![Two cloud shapes. One, labeled "physical time", contains "instant" and
"duration" with arrows going from one to the other and back. The other, labeled
"civil time", contains "datetime" and "period" in the same positions, with the
same arrows. A double-headed arrow between "instant" and "datetime", labeled
"time zone".](timezone.svg)
{@paragraph .figure}

These rule sets are set and modified by governments of every nation, and
therefore *definitely always make lots and lots of sense*.

**Remember:** as [previously covered](#gregorian), time zones have no purpose in
*either* realm, physical *or* civil; they're just how we move back and forth
*between* those realms.

## Good time zones {#good_bad_time_zones}

**Not all time zones are created equal.** Plenty of the things that you think of
as time zones are actually *really bad at* being time zones.

A well-defined time zone should support *all* conversions, from any possible
instant to a datetime, and from *almost* any possible datetime back to an
instant.[^civiltophysical]

[^civiltophysical]: The reason for this imperfection is, typically, Daylight
    Saving Time: whenever a locale moves their clocks *forward*,
    there will be a range of civil times that are "skipped over"
    and thus map to no real physical instant; whenever it moves
    them *backward*, there will be a range of civil times that
    can't be *uniquely* mapped to a single physical instant.

The **best, most useful, most correct time zones** are the canonical time zones
defined by the [IANA time zone database](https://www.iana.org/time-zones) (or
Olsen database). You can recognize them by their slashed, mixed-case,
region-based identifiers, like `America/New_York` or `Asia/Kolkata`.

For each such identifier, the database attempts to maintain a complete
historical record of all legislative changes to its ruleset, and the complete
rules governing Daylight Saving Time shifts for both the past and the
foreseeable future. This is what distinguishes these canonical time zones like
`Antarctica/DumontDUrville` from others; it is able to handle any conversion you
throw at it.

As a practical matter, it's sometimes easier to think about the time zone
instead as a function mapping an instant or datetime to an **offset** (in hours,
possibly fractional). At some instant when my offset is `-07:00` and yours is
`+02:00`, then we say (informally) that you are "nine hours ahead" of me. We
mean that your current datetime is my current datetime plus nine hours. But it's
just a different way of expressing the same thing we said above. We'll say a
little more about offsets in the [UTC](#utc) section below.

There is much more to learn about time zones than we'll cover in this document.
But if all you take away is that **Real Time Zones Look Like
`Africa/Kinshasa`**, I've done my job for now.

## Bad time zones

**Warning:** Codes like "PST" are very very bad at being time zones. Avoid them
like you avoid temporal paradoxes while time-traveling.

Here's a fun fact. General knowledge says that the contiguous 48 United States
use four "time zones", which we call "PST", "MST", "CST", and "EST".
Astonishingly, as these codes were brought into the computing world, "PST" and
"CST" got defined as *changing* with the local Daylight Saving Time, while "MST"
and "EST" are static!

And for compatibility's sake, this situation has persisted. So, a given civil
time when translated into each of these four so-called "time zones" might yield
four, three, or even just two distinct instants.

If this absurdity doesn't scare you away, I'm unclear on why you're even reading
this document.

Avoid these sorry excuses for "time zones" that use three uppercase letters.
Except one...

## UTC {#utc}

UTC is a very special time zone that is useful despite *not* being in the
above-mentioned `Asia/Kolkata` style. What is it, and why do we use it?

UTC is defined to be the *simplest* time zone possible. It has a permanent,
never-changing offset, which is defined to be *zero*. This makes it a reference
point we can use to describe all other offsets: our earlier example of an offset
of `-07:00` means *UTC* minus 7 hours. UTC provides a perfect one-to-one mapping
between (physical) instants and (civil) datetimes. That's pretty neat!

On the other hand, it's fake, so there's that. That is, it's not the "real" time
zone of any human being or business on the planet.[^iss] That's okay, though.

[^iss]: I apologize for marginalizing the 0.00000013% of readers who live in
    space, who do actually use UTC as their home time zone. You matter too.
    I'm sorry. Please don't drop anything on me.

## UTC for zone-independent code? {#zone_independent}

Pop quiz: how do you write time-zone-independent code? You use the UTC time
zone, right? This is generally on the right track, but not quite all the way
right.

The only safe way to write zone-independent code is to stay completely and
neatly in the realm of physical time, where there *is* no such thing as a time
zone in the first place. Use library types that model instants and durations,
only, and avoid the rest.

The problem with UTC is that, since it *is a time zone*, it will let you freely
cross that boundary between physical and civil time without complaint. Nothing
will stop your code from being able to ask questions like "did this instant
happen on a Thursday?" and you will get some answer to that question!

Sounds kinda... *zone-dependent*, wouldn't you say? If our intention is to be
zone-neutral then the question is bogus and unanswerable--and anything
unanswerable ought to be unaskable too. And that's what you'll get by sticking
to types like Java's `Instant`, that are designed to model physical time only.

## <a id="time_zones_change"></a>Persistence hazards {#persistence}

Suppose you have a known, fixed, IANA canonical time zone, like
`America/Chicago`. Since that provides a mapping between instants and datetimes
throughout both past and future, this means that you can think of instants and
datetimes as basically equivalent forms of the same thing, right?

And *that* means it doesn't matter which of those two values you store — the
instant (stored as seconds since the agreed-upon reference instant), or the
datetime (stored as the six field values Y/M/D/H/M/S). They're the same! Right?
*RIGHT?*

Oh dear. It would be nice if this were the case. The first problem is that many
time zones have problematic datetimes that they map to either zero or two
possible instants. We could address that by always preferring the instant... but
it's not quite as simple as that.

An even worse problem is that users sometimes *change* their time zone. And the
absolutely nasty problem is that **time zone rules themselves sometimes
change**. In fact, they often change with extremely little notice, because the
chaos these last-minute decisions wreak on computing systems and their
maintainers is generally invisible to the policymakers who make the changes.

Because of this, it matters how you choose to store these values. Suppose a user
books an event hall for their wedding, starting at 2:00 pm on a particular day,
and you store those endpoints as instants. Now if the local government decides
that Daylight Saving Time should start earlier this year, that booking might
suddenly show up as starting at 3:00 pm instead. You can see the trouble this
could cause.

But on the other hand, consider for example an online invitation to your friends
to go watch the sunrise on
[Manhattanhenge](https://www.amnh.org/research/hayden-planetarium/resources/manhattanhenge).
You'd want that to go the other way, but there would be no way for the computer
to know that.

Often there is no correct answer that guarantees the right thing will happen for
every user. But if you proactively think through what you want to happen in
these scenarios (user changes time zones, time zone rules change, etc.), you'll
be okay.

## Avoid the "zoned datetime" {#zoned_datetime}

Many libraries roll a datetime or instant, *plus* a time zone, into a single
type you can pass around. In fact, Java's first two attempts at date/time
support offered *only* this sort of combined type. With Java 8 it offered
properly separated `Instant` and `LocalDateTime` classes to address physical and
civil time (respectively) — but even as it did that, it *also* offered the
combined `ZonedDateTime` as well.

Too bad. The concept of a zoned datetime or zoned instant is **fundamentally
flawed**, and you should avoid it for these reasons:

1.  Since it represents both physical time and civil time *at the same time*, it
    creates ambiguities that lead to bugs. The API will offer you *two different
    ways* to express the question "give me this value plus one day", with one of
    them using the physical interpretation (+ 24 * 60 * 60 seconds) and the
    other using the civil interpretation (same time tomorrow, which might be a
    difference of 23 hours, or 24.5 hours, etc.). And these two ways will tend
    to look very similar to each other. Instead, by avoiding this type you'll be
    forced to choose cleanly which realm you mean, and each choice will support
    only *one* way of adding a day.

2.  As just covered, how to serialize or store such a thing is ambiguous. With a
    zoned datetime type, most developers won't even *know* which of the two
    approaches will be used, let alone have the chance to decide whether it's
    the right one for them.

3.  And besides all that, it's **simply unnecessary**! A instant/datetime and a
    time zone are two *independent* pieces of information. They can be handled
    separately instead, and it'll make your code clearer. In fact, it's unusual
    to need a distinct time zone alongside each and every individual
    instant/datetime anyway. Typically the time zone comes from some business
    object at a broader scope, like a user, session, client, etc.

**Warning:** don't use "zoned datetime" or "zoned instant" types. I already said
that, but now I've said it in a red box.

## Now what? {#now_what}

With that admonition, we've concluded our basic discussion of how to think about
time. What next?

First, you might want to learn which APIs model which of these concepts in your
particular [language environment](#languages).

You may also wish to consult the appendix which discusses additional topics:

*   [Wall time](#wall_time) vs. [elapsed time](#elapsed)
*   [Leap seconds](#leap_seconds)
*   [Recurrences](#recurrence)
*   [The "epoch"](#the-unix-epoch)
*   etc.

## Thanks {#thanks}

Major help on this document was received from
[Kurt Kluever](https://github.com/kluever),
[Greg Miller](https://github.com/devjgm),
[Chris Povirk](https://github.com/cpovirk),
[Jon Skeet](https://github.com/jskeet/),
[Bradley White](https://github.com/devbww), and many others. I'm grateful to
them, and you should be too; you should have seen the bad jokes I *started*
with.

## Language-Specific Date/Time APIs {#languages}

### C++

[Abseil Common Libraries](https://abseil.io/) are strongly recommended.

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://github.com/abseil/abseil-cpp/blob/master/absl/time/time.h#L591"><code>absl::Time</code></a></td>
      <td>resolution of at least one nanosecond</td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://github.com/abseil/abseil-cpp/blob/master/absl/time/time.h#L157"><code>absl::Duration</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://github.com/abseil/abseil-cpp/blob/master/absl/time/civil_time.h#L328"><code>absl::CivilDay</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td>none</td>
      <td></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://github.com/abseil/abseil-cpp/blob/master/absl/time/civil_time.h#L322"><code>absl::CivilSecond</code></a></td>
      <td>second resolution</td>
    </tr>
    <tr>
      <td>day of week</td>
      <td><a href="https://github.com/abseil/abseil-cpp/blob/master/absl/time/civil_time.h#L369"><code>absl::Weekday</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>period</td>
      <td>none</td>
      <td></td>
    </tr>
    <tr>
      <td>time zone</td>
      <td><a href="https://github.com/abseil/abseil-cpp/blob/master/absl/time/time.h#L846"><code>absl::TimeZone</code></a></td>
      <td></td>
    </tr>
  </tbody>
</table>

### C# / .NET

[Noda Time](https://nodatime.org/) is strongly recommended.

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.Instant.html"><code>Instant</code></a></td>
      <td>nanosecond resolution</td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.Duration.html"><code>Duration</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.LocalDate.html"><code>LocalDate</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.LocalTime.html"><code>LocalTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.LocalDateTime.html"><code>LocalDateTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>day of week</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.IsoDayOfWeek.html"><code>IsoDayOfWeek</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>period</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.Period.html"><code>Period</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time zone</td>
      <td><a href="https://nodatime.org/unstable/api/NodaTime.DateTimeZone.html"><code>DateTimeZone</code></a></td>
      <td>can use Windows or IANA time zones</td>
    </tr>
  </tbody>
</table>

### Go

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://pkg.go.dev/time#Time"><code>time.Time</code></a></td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://pkg.go.dev/time#Duration"><code>time.Duration</code></a></td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://pkg.go.dev/cloud.google.com/go/civil#Date"><code>civil.Date</code></a></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td><a href="https://pkg.go.dev/cloud.google.com/go/civil#Time"><code>civil.Time</code></a></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://pkg.go.dev/cloud.google.com/go/civil#DateTime"><code>civil.DateTime</code></a></td>
    </tr>
    <tr>
      <td>day of week</td>
      <td><a href="https://pkg.go.dev/time#Weekday"><code>time.Weekday</code></a></td>
    </tr>
    <tr>
      <td>period</td>
      <td>none</td>
    </tr>
    <tr>
      <td>time zone</td>
      <td><a href="https://pkg.go.dev/time#Location"><code>time.Location</code></a></td>
    </tr>
  </tbody>
</table>

### Java and Kotlin

Use the `java.time` library (Java 8+) if at all possible; otherwise use
[JodaTime](https://www.joda.org/joda-time/). Absolutely avoid the legacy classes
`Date`, `Calendar`, and `Timestamp` at all costs.

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html"><code>java.time.Instant</code></a></td>
      <td>nanosecond resolution</td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html"><code>java.time.Duration</code></a></td>
      <td>nanosecond resolution</td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html"><code>java.time.LocalDate</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/LocalTime.html"><code>java.time.LocalTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html"><code>java.time.LocalDateTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>day of week</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/DayOfWeek.html"><code>java.time.DayOfWeek</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>period</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/Period.html"><code>java.time.Period</code></a></td>
      <td>only has day resolution; The classes like <code>Minutes</code> and <code>Months</code> from <code>org.threeten.extra</code> provide second resolution and are preferred when possible.</td>
    </tr>
    <tr>
      <td>time zone</td>
      <td><a href="https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html"><code>java.time.ZoneId</code></a></td>
      <td></td>
    </tr>
  </tbody>
</table>

### JavaScript and TypeScript

#### Temporal

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/instant.html"><code>Temporal.Instant</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/duration.html"><code>Temporal.Duration</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/plaindate.html"><code>Temporal.PlainDate</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/plaintime.html"><code>Temporal.PlainTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/plaindatetime.html"><code>Temporal.PlainDateTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>day of week</td>
      <td>none</td>
      <td>many <code>Temporal</code> objects have a <code>dayOfWeek</code> getter</td>
    </tr>
    <tr>
      <td>period</td>
      <td>none</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/duration.html"><code>Temporal.Duration</code></a> can be added to or subtracted from civil time object types (<code>PlainDate</code>, <code>PlainDateTime</code>, etc).</td>
    </tr>
    <tr>
      <td>time zone</td>
      <td><a href="https://tc39.es/proposal-temporal/docs/timezone.html"><code>Temporal.TimeZone</code></a></td>
      <td></td>
    </tr>
  </tbody>
</table>

#### Closure

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://google.github.io/closure-library/api/goog.date.UtcDateTime.html"><code>goog.date.UtcDateTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://google.github.io/closure-library/api/goog.date.Interval.html"><code>goog.date.Interval</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://google.github.io/closure-library/api/goog.date.Date.html"><code>goog.date.Date</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td>none</td>
      <td></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://google.github.io/closure-library/api/goog.date.DateTime.html"><code>goog.date.DateTime</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>day of week</td>
      <td>none</td>
      <td>Closure's <code>Date</code>s have a <code>getWeekday</code> method</td>
    </tr>
    <tr>
      <td>period</td>
      <td>none</td>
      <td>Closure's <code>Interval</code>s can be added and subtracted from <code>DateTime</code> instances.</td>
    </tr>
    <tr>
      <td>time zone</td>
      <td>none</td>
      <td></td>
    </tr>
  </tbody>
</table>

### Python

<table>
  <thead>
    <tr>
      <th>To&nbsp;represent&nbsp;a</th>
      <th>Use</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>instant</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#datetime.datetime"><code>datetime.datetime</code></a></td>
      <td>When using <code>datetime.datetime.now(tz)</code>, <b>always</b> <a href="https://dev.to/anasmk/be-careful-when-using-python-datetime-module-1dcf">specify the <code>tz</code> parameter</a></td>
    </tr>
    <tr>
      <td>duration</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#timedelta-objects"><code>datetime.timedelta</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>date</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#date-objects"><code>datetime.date</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>time of day</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#time-objects"><code>datetime.time</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>datetime</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#datetime.datetime"><code>datetime.datetime</code></a></td>
      <td>When using <code>datetime.datetime.now(tz)</code>, <b>always</b> <a href="https://dev.to/anasmk/be-careful-when-using-python-datetime-module-1dcf">specify the <code>tz</code> parameter</a></td>
    </tr>
    <tr>
      <td>day of week</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#datetime.date.weekday"><code>datetime.weekday</code></a></td>
      <td></td>
    </tr>
    <tr>
      <td>period</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#timedelta-objects"><code>datetime.timedelta</code></a></td>
      <td>Duration is preferred over period, though timedelta can express both. See the section on periods above.</td>
    </tr>
    <tr>
      <td>time zone</td>
      <td><a href="https://docs.python.org/3/library/datetime.html#timezone-objects"><code>datetime.timezone</code></a></td>
      <td></td>
    </tr>
  </tbody>
</table>

## Appendix: Miscellaneous Time Topics {#appendix}

### Wall time {#wall_time}

Hey, what time is it?

When we ask a computer this question:

*   We are asking for the current value of "now".
*   The thing we are asking is called a "clock".
*   The clock reads the "wall time" (because clocks go on the wall).
*   And returns the answer as an instant called a "timestamp".

That's a lot of terms, but they all go together, and don't really have any more
elaborate meaning than what's illustrated here.

Note that all of this takes place firmly in the realm of *physical time.*

#### Timestamps {#timestamps}

As soon as a timestamp is read, it is already in the past, but it doesn't stop
being a "timestamp". A timestamp is just an instant that was probably originally
read as the wall time. (In practice, you are likely to see *any* old instant
called a timestamp, but what can you do.)

Timestamps are instants and all the normal physical time math operations apply.
But note that subtracting one timestamp *from another timestamp* can be sketchy!
(Read on.)

#### The most important thing about wall time {#approximate}

Wall time is inherently approximate. It drifts and is periodically corrected by
a network time service.

Yes, this means it can even appear to run backward.

Don't try to use wall time for measuring elapsed time.

### Elapsed time {#elapsed}

Elapsed time is a *measured* duration -- the duration that transpired between
two operations.

Subtracting two timestamps is a poor method for measuring elapsed time! Because
both values are *approximate*, the resulting duration has the error of both of
them summed together, and could even be *negative*. It is best to think of wall
time and elapsed time as two entirely different concepts.

Use a library such as Guava's [`Stopwatch`](https://guava.dev/Stopwatch), which
is designed for measuring elapsed time and is impervious to wall clock
corrections. See the [language-specific guide](#languages) above for more info.

### The Unix epoch

Earlier we explained that computing systems tend to choose a reference instant
that all other instants can be expressed relative to, and this is known as an
"epoch". A very common choice of epoch is the "Unix epoch". You probably already
know that the Unix epoch is start-of-day on the civil date "1970-01-01";
specifically it's the instant that this civil datetime maps to using the UTC
time zone. (That's why if you ask programmers what bogus year seems to pop up in
bugs more than any other, you'll hear "1970" if they live in the Eastern
Hemisphere, but "1969" if Western.)

But my real point here is something quite different.

My real point is that *most* code you write, by far, should neither know nor
care how that epoch is defined! It should use libraries that treat the epoch as
an implementation detail. No, it's not that anyone is ever going to suddenly
change its value. But *if they did* and your code kept working anyway, that's a
good sign that you've been following more bug-proof practices.

### AM/PM {#am-pm}

Can anything in the topic of dates and times ever be simple? Maybe AM/PM is it.
It's just a different way of writing the hour integer, that's all.

<table>
  <thead>
    <tr>
      <th>Hour as integer</th>
      <th>Hour in AM/PM form</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>0</td>
      <td>12 AM</td>
    </tr>
    <tr>
      <td>1–11</td>
      <td>1–11 AM</td>
    </tr>
    <tr>
      <td>12</td>
      <td>12 PM</td>
    </tr>
    <tr>
      <td>13–23</td>
      <td>1–11 PM</td>
    </tr>
  </tbody>
</table>

Well, it was *almost* simple.

### Day of week {#day_of_week}

Maybe this one is simple though.

A secondary feature of Gregorian civil time is that each date is
deterministically assigned a **day of week**. These have names in most
languages; in English they are "Monday", "Tuesday", etc. They will *always*
cycle in that order and never skip.

But be careful! That is true of civil time, abstractly, but civil time *as
experienced* in a particular locale is another story. And once upon a time in
two Pacific island nations,
[Thursday was followed by Saturday](https://www.timeanddate.com/on-this-day/december/30).

(Well, I'm sure *something* in this whole guide will be simple.)

#### Base logic on it?

It's tempting to base business logic on the day of week (for example, deciding
when the week begins, or whether it is a "business day", or to what degree it is
"manic"), but inferences like these are extremely locale-sensitive. Be careful.

### Leap seconds {#leap_seconds}

Uh oh. Leap seconds. These threaten to be a big confusing thing that makes
everything insane and complicated. Just look at the size of the
[Wikipedia page](https://en.wikipedia.org/wiki/Leap_second) alone! Actually no,
please don't go there. It will make you worry. And don't worry! We have ways of
dealing with leap seconds so they won't be that bad.

Here's the deal. The overwhelming majority of minutes that will ever happen last
exactly 60 seconds in length. But, because nothing can ever be simple, every now
and then there will be one that lasts 61 seconds. The additional inserted second
is called a leap second, and it happens very irregularly; the only cases in the
last decade were in 2012, 2015, and 2016. Making matters worse, in 2022 there is
now talk of a possible *negative* leap second!

**Question one:** for the love of God and chocolate *why.* Why not define "one
second" as "the time it takes a point on Earth's equator to complete 1/86,400th
of a full rotation relative to the sun"? We wanted to, but the problem is that
Earth's rotation isn't constant; it speeds up slightly when it's having fun, and
slows down again when it's waiting in line to renew its driving permit. We had
to pick a stable definition instead, and we chose one to match our preferred
definition as best we could. Since we couldn't get it perfect, now the Earth's
rotation sometimes starts to lag a bit behind. When it does, astronomers get
together and declare a leap second, to give the Earth time to catch up. By the
time that leap second ends it's right back where it's supposed to be.

**Question two:** why did I say this isn't bad? It sounds bad. How is it not
bad?

The *only* reason this isn't so bad is that for *most* intents and purposes we
can pretend they don't exist and get away with it.

Remember: wall time is always inherently approximate anyway! Your system clock
is constantly receiving small adjustments to bring it back on track; the only
thing different about a leap second is that it's a somewhat larger adjustment.
In fact, Google's way of dealing with that larger adjustment is to just turn it
into a series of smaller adjustments: a
"[leap smear](https://googleblog.blogspot.com/2011/09/time-technology-and-leaping-seconds.html)".

The effect of all this is that when you subtract two timestamps, the duration
you get might actually be incorrect. But this is *already* the case anyway due
to clock drift and periodic clock corrections.

### Midnight vs. start of day {#start_of_day}

"Midnight" is another word for the time-of-day "0:00:00.000". It's a dangerous
concept because it sounds like something that every day has, but this does not
hold true. In some time zones, certain days start later than that and have *no*
"midnight" (for example, in `Asia/Gaza` the date 2007-04-01 began at 01:00). The
concept you're probably looking for instead is called "start of day" — but often
you don't need even that, and can stick to representing the civil date itself.

### Recurrence

In civil time, a recurrence is a rule for selecting successive datetime (or
date) values. Many examples can probably be found in your calendar app of
choice: daily, daily on specified days of the week, biweekly on Fridays, on the
5th of every month, bimonthly on the second Tuesday, etc.

Every period can be used as a recurrence, but because period math is
ill-defined, this can sometimes get you into trouble. Recurrences are defined
slightly differently, so as to be, er, well-defined.

### Communicating about dates {#iso}

The second of July in the year 2019 should always be written in the following
way: "2019-07-02". This is an [international standard](https://xkcd.com/1179/)
that avoids cultural bias, avoids confusing people (does he mean February 7?),
and also makes textual dates sort correctly. Many years ago I switched to
writing *all* of my dates in this style, in all circumstances, and if everyone
reading this would start doing this too, then eventually I might get fewer
strange looks.
