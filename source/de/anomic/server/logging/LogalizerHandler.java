//LogalizerHandler.java 
//-------------------------------------
//part of YACY
//(C) by Michael Peter Christen; mc@anomic.de
//first published on http://www.anomic.de
//Frankfurt, Germany, 2004
//
//This file ist contributed by Matthias Soehnholz
//last major change: $LastChangedDate$ by $LastChangedBy$
//Revision: $LastChangedRevision$
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//Using this software in any meaning (reading, learning, copying, compiling,
//running) means that you agree that the Author(s) is (are) not responsible
//for cost, loss of data or any harm that may be caused directly or indirectly
//by usage of this softare or this documentation. The usage of this software
//is on your own risk. The installation and usage (starting/running) of this
//software may allow other people or application to access your computer and
//any attached devices and is highly dependent on the configuration of the
//software which must be done by the user of the software; the author(s) is
//(are) also not responsible for proper configuration and usage of the
//software, even if provoked by documentation provided together with
//the software.
//
//Any changes to this file according to the GPL as documented in the file
//gpl.txt aside this file in the shipment you received can be done to the
//lines that follows this copyright notice here, but changes must not be
//done inside the copyright notive above. A re-distribution must contain
//the intact and unchanged copyright notice.
//Contributions and changes to the program code must be marked as such.

package de.anomic.server.logging;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import de.anomic.plasma.plasmaParser;
import de.anomic.server.logging.logParsers.LogParser;

public class LogalizerHandler extends Handler {

    public static boolean enabled;
    public static boolean debug;
    private String logParserPackage;
    private HashMap parsers;
    
    public LogalizerHandler() {
        super();
        configure();
    }    

    private HashMap loadParsers() {
        HashMap parsers = new HashMap();
        try {
            if (debug) System.out.println("Searching for additional content parsers in package " + logParserPackage);
            // getting an uri to the parser subpackage
            String packageURI = plasmaParser.class.getResource("/"+logParserPackage.replace('.','/')).toString();
            if (debug) System.out.println("LogParser directory is " + packageURI);
            
            File parserDir = new File(new URI(packageURI));
            //System.out.println(parserDir.toString());
            String [] parserDirFiles = parserDir.list(parserNameFilter);
            if(parserDirFiles == null && enabled) {
                System.out.println("Can't find any parsers in "+parserDir.getAbsolutePath());
            }
            //System.out.println(parserDirFiles.length);
            for (int i=0; i<parserDirFiles.length; i++) {
                String tmp = parserDirFiles[i].substring(0,parserDirFiles[i].indexOf(".class"));
                Class tempClass = Class.forName(logParserPackage+"."+tmp);
                if (tempClass.isInterface() && enabled) System.out.println(tempClass.getName() + " is an Interface");
                else {
                    Object theParser = tempClass.newInstance();
                    if (theParser instanceof LogParser) {
                        LogParser theLogParser = (LogParser) theParser;
                        //System.out.println(bla.getName() + " is a logParser");
                        parsers.put(theLogParser.getParserType(), theParser);
                        if (debug) System.out.println("Added " + theLogParser.getClass().getName() + " as " + theLogParser.getParserType() + " Parser.");
                    }
                    else {
                        //System.out.println(bla.getName() + " is not a logParser");
                        if (debug) System.out.println("Rejected " + tempClass.getName() + ". Class does not implement the logParser-Interface");

                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return parsers;
    }
    
    /**
     * Get any configuration properties set
     */
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String className = getClass().getName();

        if(manager.getProperty(className + ".enabled").equalsIgnoreCase("true")) enabled = true;
        if(manager.getProperty(className + ".debug").equalsIgnoreCase("true")) debug = true;

        logParserPackage = manager.getProperty(className + ".parserPackage");

        parsers = loadParsers();
    }
    
    public void publish(LogRecord record) {
        if (enabled) {
            LogParser temp = (LogParser) parsers.get(record.getLoggerName());
            if (temp != null) {
                int returnV = temp.parse(record.getLevel().toString(), record.getMessage());
                //if (debug) System.out.println("Logalizertest: " + returnV + " --- " + record.getLevel() + " --- " + record.getMessage());
                if (debug) System.out.println("Logalizertest: " + returnV + " --- " + record.getLevel());
            }
        }
    }
    
    public void close() throws SecurityException {
        // TODO Auto-generated method stub

    }

    public void flush() {
        // TODO Auto-generated method stub

    }
    
    private static final FilenameFilter parserNameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.matches(".*.class");
        }
    };


}
