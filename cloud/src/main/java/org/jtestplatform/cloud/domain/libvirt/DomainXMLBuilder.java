/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2016  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.cloud.domain.libvirt;

import org.jtestplatform.cloud.domain.DomainConfig;
import org.jtestplatform.cloud.domain.DomainException;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
class DomainXMLBuilder {
    static final char BEGIN_TAG = '$';
    static final char END_TAG = BEGIN_TAG;

    String build(DomainConfig config, String macAddress, String networkName) {
        ST template;
        try {
            template = new ST(readResource("domain_template.xml"), BEGIN_TAG, END_TAG);
        } catch (DomainException e) {
            throw new RuntimeException(e);
        }

        template.add("config", config);
        template.add("macAddress", macAddress);
        template.add("networkName", networkName);
        //TODO use cpu and wordSize properties

        return template.render();
    }

    private static String readResource(String template) throws DomainException {
        InputStream resourceAsStream = DomainXMLBuilder.class.getResourceAsStream(template);
        Reader reader = new InputStreamReader(resourceAsStream);
        CharBuffer xmlTemplate;
        try {
            xmlTemplate = CharBuffer.allocate(resourceAsStream.available());
            int read;
            do {
                read = reader.read(xmlTemplate);
            } while (read > 0);
        } catch (IOException e) {
            throw new DomainException(e);
        } finally {
            try {
                resourceAsStream.close();
            } catch (IOException e) {
                throw new DomainException(e);
            }
        }

        return xmlTemplate.rewind().toString();
    }
}
