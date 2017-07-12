/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.ForOverride;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * A representation of a choice with zero or more options, which may be evaluated lazily or
 * strictly.
 *
 * <p>This resembles the list monad in Haskell.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public abstract class Choice<T> {
  /*
   * This is currently implemented in terms of Iterators, but this may change in future, e.g. to a
   * Stream-based implementation.
   *
   * Special-casing operations for Choice.none and Choice.single, which are by far the most common
   * cases, avoids overly nested results.
   */

  private static final Choice<Object> NONE =
      new Choice<Object>() {

        @Override
        protected Iterator<Object> iterator() {
          return Collections.emptyIterator();
        }

        @Override
        public <R> Choice<R> thenChoose(Function<? super Object, Choice<R>> function) {
          checkNotNull(function);
          return none();
        }

        @Override
        public <R> Choice<R> thenOption(Function<? super Object, Optional<R>> function) {
          checkNotNull(function);
          return none();
        }

        @Override
        public <R> Choice<R> transform(Function<? super Object, R> function) {
          checkNotNull(function);
          return none();
        }

        @Override
        public Choice<Object> or(Choice<Object> other) {
          return checkNotNull(other);
        }

        @Override
        public Choice<Object> condition(Predicate<? super Object> predicate) {
          checkNotNull(predicate);
          return this;
        }

        @Override
        public String toString() {
          return "Choice.NONE";
        }
      };

  /** The empty {@code Choice}. */
  @SuppressWarnings("unchecked")
  public static <T> Choice<T> none() {
    return (Choice) NONE;
  }

  /** Returns a {@code Choice} with only one option, {@code t}. */
  public static <T> Choice<T> of(final T t) {
    checkNotNull(t);
    return new Choice<T>() {
      @Override
      protected Iterator<T> iterator() {
        return Iterators.singletonIterator(t);
      }

      @Override
      public Optional<T> first() {
        return Optional.of(t);
      }

      @Override
      public Choice<T> condition(Predicate<? super T> predicate) {
        return predicate.apply(t) ? this : Choice.<T>none();
      }

      @Override
      public <R> Choice<R> thenChoose(Function<? super T, Choice<R>> function) {
        return function.apply(t);
      }

      @Override
      public <R> Choice<R> thenOption(Function<? super T, Optional<R>> function) {
        return fromOptional(function.apply(t));
      }

      @Override
      public <R> Choice<R> transform(Function<? super T, R> function) {
        return of(function.apply(t));
      }

      @Override
      public String toString() {
        return String.format("Choice.of(%s)", t);
      }
    };
  }

  /**
   * Returns a {@code Choice} with {@code t} as an option if {@code condition}, and no options
   * otherwise.
   */
  public static <T> Choice<T> condition(boolean condition, T t) {
    return condition ? of(t) : Choice.<T>none();
  }

  /**
   * Returns a choice of the optional value, if it is present, or the empty choice if it is absent.
   */
  public static <T> Choice<T> fromOptional(Optional<T> optional) {
    return optional.isPresent() ? of(optional.get()) : Choice.<T>none();
  }

  public static <T> Choice<T> from(final Collection<T> choices) {
    switch (choices.size()) {
      case 0:
        return none();
      case 1:
        return of(Iterables.getOnlyElement(choices));
      default:
        return new Choice<T>() {
          @Override
          protected Iterator<T> iterator() {
            return choices.iterator();
          }

          @Override
          public String toString() {
            return String.format("Choice.from(%s)", choices);
          }
        };
    }
  }

  /** Returns a choice between any of the options from any of the specified choices. */
  public static <T> Choice<T> any(final Collection<Choice<T>> choices) {
    return from(choices).thenChoose(Functions.<Choice<T>>identity());
  }

  private Choice() {}

  @VisibleForTesting
  Iterable<T> asIterable() {
    return this::iterator;
  }

  // Currently, this is implemented with an Iterator, but that may change in future!
  @ForOverride
  protected abstract Iterator<T> iterator();

  @Override
  public String toString() {
    return Iterables.toString(asIterable());
  }

  /** Returns the first valid option from this {@code Choice}. */
  public Optional<T> first() {
    Iterator<T> itr = iterator();
    return itr.hasNext() ? Optional.of(itr.next()) : Optional.<T>absent();
  }

  /**
   * Returns all the choices obtained by choosing from this {@code Choice} and then choosing from
   * the {@code Choice} yielded by this function on the result.
   *
   * <p>The function may be applied lazily or immediately, at the discretion of the implementation.
   *
   * <p>This is the monadic bind for {@code Choice}.
   */
  public <R> Choice<R> thenChoose(final Function<? super T, Choice<R>> function) {
    checkNotNull(function);
    if (Thread.interrupted()) {
      throw new RuntimeException(new InterruptedException());
    }
    final Choice<T> thisChoice = this;
    return new Choice<R>() {
      @Override
      protected Iterator<R> iterator() {
        if (Thread.interrupted()) {
          throw new RuntimeException(new InterruptedException());
        }
        return Iterators.concat(
            Iterators.transform(thisChoice.iterator(), (T t) -> function.apply(t).iterator()));
      }
    };
  }

  /**
   * Returns all the choices obtained by choosing from this {@code Choice} and yielding a present
   * {@code Optional}.
   *
   * <p>The function may be applied lazily or immediately, at the discretion of the implementation.
   */
  public <R> Choice<R> thenOption(final Function<? super T, Optional<R>> function) {
    checkNotNull(function);
    final Choice<T> thisChoice = this;
    return new Choice<R>() {
      @Override
      protected Iterator<R> iterator() {
        return Optional.presentInstances(Iterables.transform(thisChoice.asIterable(), function))
            .iterator();
      }
    };
  }

  /** Maps the choices with the specified function. */
  public <R> Choice<R> transform(final Function<? super T, R> function) {
    checkNotNull(function);
    final Choice<T> thisChoice = this;
    return new Choice<R>() {
      @Override
      protected Iterator<R> iterator() {
        return Iterators.transform(thisChoice.iterator(), function);
      }
    };
  }

  /** Returns a choice of the options from this {@code Choice} or from {@code other}. */
  public Choice<T> or(final Choice<T> other) {
    checkNotNull(other);
    if (other == none()) {
      return this;
    } else {
      final Choice<T> thisChoice = this;
      return new Choice<T>() {
        @Override
        protected Iterator<T> iterator() {
          return Iterators.concat(thisChoice.iterator(), other.iterator());
        }

        @Override
        public String toString() {
          return String.format("%s.or(%s)", thisChoice, other);
        }
      };
    }
  }

  /** Returns this choice if {@code condition}, otherwise the empty choice. */
  public Choice<T> condition(boolean condition) {
    return condition ? this : Choice.<T>none();
  }

  /** Filters the choices to those that satisfy the provided {@code Predicate}. */
  public Choice<T> condition(final Predicate<? super T> predicate) {
    checkNotNull(predicate);
    final Choice<T> thisChoice = this;
    return new Choice<T>() {
      @Override
      protected Iterator<T> iterator() {
        return Iterators.filter(thisChoice.iterator(), predicate);
      }

      @Override
      public String toString() {
        return String.format("%s.condition(%s)", thisChoice, predicate);
      }
    };
  }
}
