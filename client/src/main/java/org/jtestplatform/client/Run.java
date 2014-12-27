/**
 * JTestPlatform is a client/server framework for testing any JVM
 * implementation.
 *
 * Copyright (C) 2008-2015  Fabien DUMINY (fduminy at jnode dot org)
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
package org.jtestplatform.client;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Fabien DUMINY (fduminy@jnode.org)
 *
 */
public final class Run {
    private static final String ROOT_WORK_DIR_PREFIX = "run_";

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    public static Run getLatest(File workDir) {
        workDir.mkdirs();
        String[] runs = workDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(ROOT_WORK_DIR_PREFIX);
            }
        });

        Date latest = null;
        String latestRun = null;
        for (String run : runs) {
            if (latest == null) {
                latest = getTimestamp(run);
                latestRun = run;
            } else {
                Date d = getTimestamp(run);
                if (d.after(latest)) {
                    latest = d;
                    latestRun = run;
                }
            }
        }

        Run result = null;
        if (latest != null) {
            result = new Run(new File(workDir, latestRun), latest);
        }
        return result;
    }

    public static Run create(File workDir) {
        Date timestamp = Calendar.getInstance().getTime();
        String run = ROOT_WORK_DIR_PREFIX + TIMESTAMP_FORMAT.format(timestamp);
        File rootWorkDir = new File(workDir, run);
        rootWorkDir.mkdirs();
        return new Run(rootWorkDir, timestamp);
    }

    private static Date getTimestamp(String run) {
        try {
            return TIMESTAMP_FORMAT.parse(run.substring(
                    ROOT_WORK_DIR_PREFIX.length()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final File rootWorkDir;
    private final Date timestamp;
    private final String timestampString;

    private Run(File rootWorkDir, Date timestamp) {
        this.rootWorkDir = rootWorkDir;
        this.timestamp = timestamp;
        this.timestampString = TIMESTAMP_FORMAT.format(timestamp);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getTimestampString() {
        return timestampString;
    }

    public File getWorkingTests() {
        return new File(rootWorkDir, "working-tests.txt");
    }

    public File getCrashingTests() {
        return new File(rootWorkDir, "crashing-tests.txt");
    }

    /**
     * @return
     */
    public File getReportXml() {
        return new File(rootWorkDir, "report.xml");
    }
}
