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
/**
 * 
 */
package org.jtestplatform.cloud.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 * @param <T>
 */
public class LoadBalancer<T> {
    private final List<T> elements;
    
    private AtomicInteger nextElement = new AtomicInteger(0);
    
    public LoadBalancer(List<T> elements) {
        this();
        this.elements.addAll(elements);
    }
    
    /**
     * 
     */
    public LoadBalancer() {
        this.elements = new ArrayList<T>();
    }

    public T getNext() {
        //TODO use pluggable strategy
        // simple round robin        
        T element;
        
        synchronized (elements) {
            // block the thread until at least one element is present 
            while (elements.isEmpty()) {
                try {
                    elements.wait(); // block until something is added
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            
            element = elements.get(nextElement.getAndIncrement());
            nextElement.compareAndSet(elements.size(), 0);
        }
        
        return element;
    }

    /**
     * @param domain
     */
    public void add(T element) {
        synchronized (elements) {
            elements.add(element);            
            elements.notify(); // notify something has been added
        }
    }

    /**
     * @param domain
     * @return
     */
    public boolean remove(T element) {
        synchronized (elements) {
            boolean removed = elements.remove(element);
            
            if (removed) {
                nextElement.compareAndSet(elements.size(), 0);
            }
            
            return removed;
        }
    }
    
    /**
     * @return
     */
    public int size() {
        return elements.size();
    }

    /**
     * @return
     */
    public List<T> clear() {
        ArrayList<T> result;
        synchronized (elements) {
            result = new ArrayList<T>(elements);
            elements.clear();
        }
        return result;
    }
}
