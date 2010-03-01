/**
 * JTestPlatform is a client/server framework for testing any JVM implementation.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jtestplatform.client;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.DomainConfig;

public class CustomConfigReader extends ConfigReader {    
    private final String type;
    private final String vmConfigFile;
    
    public CustomConfigReader(String type, String vmConfigFile) {
        this.type = type;
        this.vmConfigFile = vmConfigFile;
    }
    
    @Override
    protected List<DomainConfig> createVMConfig(Properties properties, String type, File vmConfigFile)
        throws ConfigurationException {
        List<DomainConfig> configs = new ArrayList<DomainConfig>();
        
        try {
            Reader reader = new FileReader(new File(vmConfigFile.getParentFile(), this.vmConfigFile));
            addConfig(properties, configs, this.type, reader);
        } catch (IOException ioe) {
            throw new ConfigurationException("failed to read file " + vmConfigFile.getAbsolutePath(), ioe);
        }
        
        return configs;
    }
}
