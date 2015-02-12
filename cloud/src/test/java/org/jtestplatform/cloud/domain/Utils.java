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
/**
 * 
 */
package org.jtestplatform.cloud.domain;

import com.google.code.tempusfugit.temporal.Condition;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jtestplatform.cloud.domain.Utils.FixedState.ALWAYS_ALIVE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class Utils {
    private Utils() {
    }

    private static File getResource(String resource) {
        URL url = Utils.class.getResource("/" + resource);
        File f = new File(url.getFile());
        if (!f.exists()) {
            throw new RuntimeException("file not found : " + f.getAbsolutePath());
        }
        return f;
    }
    
    public static String getCDROM() {
        return getResource("microcore_2.7.iso").getAbsolutePath();
    }
    
    public static File getConfigFile() {
        return getResource("cloud.xml");
    }

    public static Domain createFixedStateDomain(final FixedState fixedState) throws DomainException {
        Domain domain = mock(Domain.class);
        when(domain.isAlive()).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return ALWAYS_ALIVE.equals(fixedState);
            }
        });
        return domain;
    }

    public static class MethodAnswer<T> implements Answer<T> {
        private final AtomicBoolean called = new AtomicBoolean(false);

        public MethodAnswer() {
        }

        @Override
        public T answer(InvocationOnMock invocationOnMock) throws Throwable {
            called.set(true);
            return null;
        }

        public Condition called() {
            return new Condition() {
                @Override
                public boolean isSatisfied() {
                    return called.get();
                }
            };
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static enum FixedState {
        ALWAYS_ALIVE,
        ALWAYS_DEAD;

        public static void verifyStateOf(Domain domain, FixedState expectedState) throws DomainException {
            if (ALWAYS_DEAD.equals(expectedState)) {
                assertFalse("must be dead", domain.isAlive());
            } else {
                assertTrue("domain must be alive", domain.isAlive());
            }
        }
    }

    public static List<String> generateStringList(int count) {
        List<String> elements = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            elements.add("ELEMENT" + i);
        }
        return elements;
    }
}
