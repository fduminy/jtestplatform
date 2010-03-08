/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2010  Fabien DUMINY (fduminy at jnode dot org)
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
 * -
 */
package org.jtestplatform.common;

import java.io.File;
import java.util.Properties;

public class ConfigUtils {
    private ConfigUtils() {        
    }

    public static boolean isBlank(String s) {
        return (s == null) || s.trim().isEmpty();
    }

    public static int getInt(Properties properties, String name, int defaultValue) {
        String valueStr = properties.getProperty(name);
        int value = defaultValue;

        if (!isBlank(valueStr)) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return value;
    }

    public static File getDirectory(Properties properties, String name, File defaultDir) {
        String dirStr = properties.getProperty(name);
        File dir = defaultDir;

        if (!isBlank(dirStr)) {
            dir = new File(dirStr);

            if (!dir.exists()) {
                dir.mkdirs();
            } else if (!dir.isDirectory()) {
                dir = defaultDir;
            }
        }

        return dir;
    }

    public static String[] getStringArray(Properties properties, String name) {
        String str = properties.getProperty(name);
        String[] array = null;

        if (!isBlank(str)) {
            array = str.split(",");
        }

        return (array == null) ? new String[0] : array;
    }

    public static boolean getBoolean(Properties properties, String name, boolean defaultValue) {
        String str = properties.getProperty(name);
        boolean value = defaultValue;

        if (!isBlank(str)) {
            value = "true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str);
        }

        return value;
    }

    public static File getDirectory(Properties properties, String name, boolean mustExist) {
        return getFile(properties, name, mustExist, true); // directory=true
    }

    public static File getFile(Properties properties, String name, boolean mustExist) {
        return getFile(properties, name, mustExist, false); // directory=false (aka we want a file)
    }

    private static File getFile(Properties properties, String name, boolean mustExist, boolean directory) {
        String fileStr = properties.getProperty(name);
        File file = null;

        if (!isBlank(fileStr)) {
            file = new File(fileStr);

            boolean validType = (directory && file.isDirectory()) || (!directory && file.isFile()); 
            if ((!file.exists() && mustExist) || !validType) {
                file = null;
            }
        }

        if (file == null) {
            final String type = directory ? "directory" : "file";
            final String msg;
            if (mustExist) {
                msg = "parameter " + name + " must be an existing " + type;
            } else {
                msg = "parameter " + name + " must be a " + type;
            }
            throw new IllegalArgumentException(msg + " (value: " + fileStr + ")");
        }

        return file;
    }

    public static String getString(Properties properties, String name) {
        String value = properties.getProperty(name, null);
        if (isBlank(value)) {
            throw new IllegalArgumentException("property " + name + " must be specified");
        }

        return value;
    }

    /**
     * @param properties
     * @param string
     * @param b
     * @return
     */
    public static String getClasspath(Properties properties, String name, boolean mustExist) {
        String classpath;

        if (mustExist) {
            classpath = getString(properties, name);
        } else {
            classpath = properties.getProperty(name, null);
        }

        if (classpath != null) {
            classpath = classpath.replace(',', File.pathSeparatorChar);
        }

        return classpath;
    }
}
