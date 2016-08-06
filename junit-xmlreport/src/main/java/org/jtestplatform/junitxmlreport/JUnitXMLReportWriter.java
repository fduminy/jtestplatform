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
package org.jtestplatform.junitxmlreport;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;

/**
 * @author Fabien DUMINY (fduminy at jnode dot org)
 */
public class JUnitXMLReportWriter {
    /**
     * Writes a JUnit XML report.
     *
     * @param output
     * @param suites
     */
    public void write(OutputStream output, Testsuites suites) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        marshaller.marshal(suites, output);
    }
}
