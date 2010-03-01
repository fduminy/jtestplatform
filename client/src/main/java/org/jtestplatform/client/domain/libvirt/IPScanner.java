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
/**
 * 
 */
package org.jtestplatform.client.domain.libvirt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public class IPScanner {
    private static final Logger LOGGER = Logger.getLogger(IPScanner.class);
    
    private final List<String> usedIPs = new Vector<String>();
    
    private final String ipBase;
    private final int min;
    private final int max;
    
    public IPScanner(String ipBase, int min, int max) {
        this.ipBase = ipBase;
        this.min = min;
        this.max = max;
    }
    
    public synchronized List<String> computeDelta() throws UnknownHostException, IOException {
        int capacity = max - min + 1;
        final List<String> newUsedIPs = new Vector<String>(capacity);
        ExecutorService executor = new ThreadPoolExecutor(1, 16, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(capacity));
        for (int i = min; i <= max; i++) {
            final String ip = ipBase + i;
            Runnable r = new Runnable () {
                @Override
                public void run() {                                        
                    boolean found = false;
                    try {
                        found = ping(ip);
                    } catch (UnknownHostException e) {
                        LOGGER.error(e);
                    } catch (IOException e) {                        
                        LOGGER.error(e);
                    }
                    if (found) {
                        LOGGER.debug("\n" + ip + " : " + found);
                        newUsedIPs.add(ip);
                    }
                }
            };
            executor.execute(r);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // ignore
        }
        
        List<String> delta = new ArrayList<String>();
        for (String ip : newUsedIPs) {
            if (!usedIPs.contains(ip)) {
                delta.add(ip);
            }
        }
        
        usedIPs.clear();
        usedIPs.addAll(newUsedIPs);
        
        return delta;
    }
    
    private boolean ping(String host) throws UnknownHostException, IOException {
        int timeOut = 1000;
        boolean found = InetAddress.getByName(host).isReachable(timeOut);
        return found;
    }

    /**
     * @param ipAddress
     * @throws IOException 
     * @throws UnknownHostException 
     */
    public synchronized void waitRemoval(String ipAddress) throws UnknownHostException, IOException {
        while (ping(ipAddress)) { //TODO use java.util.concurrent instead
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        usedIPs.remove(ipAddress);
    }    
}
