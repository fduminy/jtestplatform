/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 * <p>
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
 * <p>
 * JTestPlatform is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * JTestPlatform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package org.jtestplatform.server;

import org.jtestplatform.common.TestName;
import org.jtestplatform.common.message.TestResult;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class ServerUtils {
    public static String printStackTrace(Throwable throwable, TestResult testResult) {
        final String test = testResult.getTest();
        int index = TestName.indexOfSeparator(test);
        if (index < 0) {
            return printStackTrace(throwable, test);
        } else {
            // TODO try to avoid allocating 2 sub-strings (using CharSequence instead of String instances)
            return printStackTrace(throwable, test.substring(0, index), test.substring(index + 1));
        }
    }

    public static String printStackTrace(Throwable throwable, String className) {
        return printStackTrace(throwable, className, null);
    }

    public static String printStackTrace(Throwable throwable, String className, String methodName) {
        // estimate result size
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int nbLines;
        for (nbLines = stackTrace.length - 1; nbLines >= 0; nbLines--) {
            final StackTraceElement element = stackTrace[nbLines];
            if (element.getClassName().equals(className) &&
                ((methodName == null) || element.getMethodName().equals(methodName))) {
                break;
            }
        }
        nbLines = (nbLines < 0) ? 0 : nbLines;
        //        int nbLines = stackTrace.length - 1;
        int size = 90 * (nbLines + 2);

        StringBuilder buffer = new StringBuilder(size);
        buffer.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append('\n');
        boolean first = true;
        for (int i = 0; i <= nbLines; i++) {
            StackTraceElement ste = stackTrace[i];
            if (!first) {
                buffer.append('\n');
            }
            buffer.append('\t').append("at ").append(ste.toString());
            first = false;
        }
        return buffer.toString();
    }
}
