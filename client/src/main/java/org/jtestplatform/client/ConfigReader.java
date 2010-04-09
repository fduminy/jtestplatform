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
package org.jtestplatform.client;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;
import org.dom4j.DocumentException;
import org.jtestplatform.common.ConfigUtils;
import org.jtestplatform.configuration.Configuration;
import org.jtestplatform.configuration.io.dom4j.ConfigurationDom4jReader;

/**
 * Utility class used to read the configuration.
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class ConfigReader {
    private static final String HOME_DIRECTORY_PROPERTY = "jtestplatform.home";

    /**
     * Read the configuration.
     * @return the configuration.
     * @throws IOException
     */
    public Configuration read() throws ConfigurationException {
        try {
            File homeDirectory = findHome();
            File configurationDirectory = new File(homeDirectory, "config");
            File configFile = new File(configurationDirectory, "config.xml");

            // init log4j
            File logConfigFile = new File(configurationDirectory, "log4j.properties");
            PropertyConfigurator.configure(logConfigFile.getAbsolutePath());

            ConfigurationDom4jReader reader = new ConfigurationDom4jReader();

            Configuration config = reader.read(new FileReader(configFile));

            File workDir = new File(config.getWorkDir());
            if (!workDir.isAbsolute()) {
                workDir = new File(homeDirectory, config.getWorkDir());
            }
            config.setWorkDir(workDir.getAbsolutePath());

            return config;
        } catch (IOException e) {
            throw new ConfigurationException("can't read config", e);
        } catch (DocumentException e) {
            throw new ConfigurationException("can't read config", e);
        }
    }

    /**
     * Search for home directory.
     * @throw RuntimeException if something is wrong (typically the home directory can't be found).
     */
    private File findHome() {
        // search home directory
        String homeProperty = System.getProperty(HOME_DIRECTORY_PROPERTY);
        if (ConfigUtils.isBlank(homeProperty)) {
            throw new RuntimeException(HOME_DIRECTORY_PROPERTY + " system property not set");
        }

        return new File(homeProperty.trim());
    }
}
