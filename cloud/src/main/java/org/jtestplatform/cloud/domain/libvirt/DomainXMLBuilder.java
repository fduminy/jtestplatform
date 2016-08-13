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
