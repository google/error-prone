package com.google.errorprone.bugpatterns.slf4j;

import java.util.Comparator;

import javax.lang.model.element.Modifier;

/**
 * A comparator to sort {@link Modifier}. The order is based on styleguide from Google and Open JDK community.
 */
final class ModifierComparator implements Comparator<Modifier> {
    /**
     * @see <a href="https://google.github.io/styleguide/javaguide.html#s4.8.7-modifiers">Google Java Style</a>
     * @see <a href="http://cr.openjdk.java.net/~alundblad/styleguide/index-v6.html#toc-modifiers">Open JDK Java Style Guidelines</a>
     */
    private int toOrder(Modifier input) {
        switch (input) {
        case PUBLIC:
        case PROTECTED:
        case PRIVATE:
            return 1;
        case ABSTRACT:
            return 2;
        case STATIC:
            return 3;
        case FINAL:
            return 4;
        case TRANSIENT:
            return 5;
        case VOLATILE:
            return 6;
        case DEFAULT:
            return 7;
        case SYNCHRONIZED:
            return 8;
        case NATIVE:
            return 9;
        case STRICTFP:
            return 10;
        default:
            throw new IllegalArgumentException("Unknown modifier:" + input);
        }
    }

    @Override
    public int compare(Modifier left, Modifier right) {
        return toOrder(left) - toOrder(right);
    }
}