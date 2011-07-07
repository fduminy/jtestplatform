/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2011  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.client.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This is a utility class for configuration.
 *
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public final class ConfigurationUtils {

    private static final String BEGIN_VARIABLE_REF = "${";
    private static final String END_VARIABLE_REF = "}";

    private ConfigurationUtils() {
    }

    /**
     * Read a properties file.
     *
     * @param configFile File where configuration is stored.
     * @return the properties file.
     * @throws IOException
     */
    public static Properties readProperties(File configFile) throws IOException {
        Properties properties = new Properties(System.getProperties());
        properties.load(new FileInputStream(configFile));
        expandVariables(properties);
        return properties;
    }

    private static void expandVariables(Properties properties) {
        boolean replaced;

        do {
            replaced = false;
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                String newValue = expandValue(properties, value);
                if (!newValue.equals(value)) {
                    properties.setProperty(key, newValue);
                    replaced = true;
                }
            }
        } while (replaced);
    }

    public static String expandValue(Properties properties, String value) {
        if (value != null) {
            boolean replaced;
            do {
                replaced = false;
                int begin = value.indexOf(BEGIN_VARIABLE_REF);
                if (begin >= 0) {
                    int beginVariable = begin + BEGIN_VARIABLE_REF.length();
                    int endVariable = value.indexOf(END_VARIABLE_REF,
                            beginVariable + 1);
                    if (endVariable >= 0) {
                        String variable = value.substring(beginVariable,
                                endVariable);
                        String variableValue = properties.getProperty(variable);
                        if (variableValue != null) {
                            value = value.substring(0, begin) + variableValue
                                + value.substring(endVariable
                                        + END_VARIABLE_REF.length());
                            replaced = true;
                        }
                    }
                }
            } while (replaced);
        }

        return value;
    }
}
