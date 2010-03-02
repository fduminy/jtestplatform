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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.dom4j.DocumentException;
import org.jtestplatform.client.domain.ConfigurationException;
import org.jtestplatform.client.domain.DomainConfig;
import org.jtestplatform.client.domain.DomainFactory;
import org.jtestplatform.client.domain.libvirt.LibVirtDomainFactory;
import org.jtestplatform.client.utils.ConfigurationUtils;
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
        
//    private Map<String, DomainFactory<?>> typeToFactory;
    
    public ConfigReader() {
//        typeToFactory = new HashMap<String, DomainFactory<?>>();
        //addFactory(new LibVirtDomainFactory());
    }
    
//    private void addFactory(DomainFactory<?> factory) {
//        typeToFactory.put(factory.getType(), factory);
//    }

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
            
//            Properties properties = ConfigurationUtils.readProperties(configFile);
//            properties.put("config.dir", configFile.getParentFile().getAbsolutePath());
//            // read the vm configuration
//            String useVM = ConfigUtils.getString(properties, "use.vm");
//            
//            int index = useVM.indexOf(':');
//            String type = useVM.substring(0, index);
//            String vmConfig = useVM.substring(index + 1);
//            
//            File vmConfigFile = new File(configFile.getParentFile(), vmConfig);
//            List<DomainConfig> vmConfigs = createVMConfig(properties, type, vmConfigFile);
//            Config config = createConfig(homeDirectory, configurationDirectory, properties, vmConfigs);
//                        
//            return config;
            ConfigurationDom4jReader reader = new ConfigurationDom4jReader();
            return reader.read(new FileReader(configFile));
        } catch (IOException e) {
            throw new ConfigurationException("can't read config", e);
        } catch (DocumentException e) {
            throw new ConfigurationException("can't read config", e);
        }
    }
    
//    protected Config createConfig(File homeDirectory, File configurationDirectory, Properties properties, List<DomainConfig> vmConfigs) {
//        Config config = new Config();
//        config.setServerName(properties.getProperty("server.name", "localhost"));
//        config.setServerPort(ConfigUtils.getInt(properties, "server.port", 10000));
//        config.setWorkDir(new File(homeDirectory, "workdir"));
//        config.setExcludingFilters(ConfigUtils.getStringArray(properties, "excluding.filters"));
//        config.setWatchDogPollInterval(ConfigUtils.getInt(properties, "watchdog.poll.interval", 10000));
//        
//        return config;
//    }

//    /**
//     * Read the configuration of the VM.
//     * @param type type of VM
//     * @param vmConfigFile file containing vm configuration
//     * @return
//     * @throws IOException
//     */
//    protected List<DomainConfig> createVMConfig(Properties properties, String type, File vmConfigFile) throws ConfigurationException {
//        List<DomainConfig> configs = new ArrayList<DomainConfig>();
//        
//        try {
//            Reader reader = new FileReader(vmConfigFile);
//            addConfig(properties, configs, type, reader);
//        } catch (FileNotFoundException e) {
//            throw new ConfigurationException("can't read file " + vmConfigFile.getAbsolutePath(), e);
//        } 
//        
//        return configs;
//    }
//    
//    protected final void addConfig(Properties properties, List<DomainConfig> configs, String type, Reader reader) throws ConfigurationException {
//        DomainFactory<?> factory = typeToFactory.get(type);        
//        if (factory == null) {
//            throw new ConfigurationException("unsupported type : " + type);
//        }
//        
//        configs.add(factory.readConfig(properties, reader));        
//    }
    
    /**
     * Search for home directory.
     * @throw RuntimeException if something is wrong (typically the home directory can't be found).
     */
    private File findHome() {
        final File home;
        
        // search home directory 
        //HOME = searchHomeDirectory();
        String homeProperty = System.getProperty(HOME_DIRECTORY_PROPERTY);
        if ((homeProperty != null) && !homeProperty.trim().isEmpty()) {
            home = new File(homeProperty.trim());
        } else {
            home = null;
            throw new RuntimeException(HOME_DIRECTORY_PROPERTY + " system property not set");
        }
        
        return home;
    }
}
