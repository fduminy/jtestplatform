/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 *
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.server;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Collection;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
final class Utils {
    private Utils() {
    }

    public static <T> Matcher<Collection<T>> contains(final T item) {
        return new BaseMatcher<Collection<T>>() {
            @Override
            public boolean matches(Object container) {
                boolean result;
                if (container instanceof Collection<?>) {
                    result = ((Collection<?>) container).contains(item);
                } else if ((container instanceof String) && (item instanceof String)) {
                    result = ((String) container).contains((String) item);
                } else {
                    throw new IllegalArgumentException(
                            "unsupported container class : " + container.getClass().getName());
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("must contain " + item.toString());
            }
        };
    }

    public static String makeTestName(Class<?> testClass, String methodName) {
        return testClass.getName() + ((methodName == null) ? "" : ("#" + methodName));
    }

    public static Matcher<Collection<String>> contains(Class<?> testClass, String methodName) {
        return contains(makeTestName(testClass, methodName));
    }

    public static Matcher<Collection<String>> contains(Class<?> testClass) {
        return contains(testClass.getName());
    }
}
