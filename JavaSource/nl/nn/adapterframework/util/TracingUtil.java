/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import nl.nn.adapterframework.util.LogUtil;

//do not use these imports, they make inclusion of mett-server.jar required
//import com.ing.coins.mett.application.MonitorAccessor;
//import com.ing.coins.mett.application.exceptions.MonitorStartFailedException;


/**
 * Single point of entry for METT tracing utility
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.5
 * @version $Id$
 */
public class TracingUtil {
	private static Logger log = LogUtil.getLogger(TracingUtil.class);
	private static String properties = "<mett-server>\n\t<start-on-boot>true</start-on-boot>\n\t<sleep-time-thread>50</sleep-time-thread>\n\t<log-events>true</log-events>\n\t<listener-pumps>\n\t\t<listener-pump>\n\t\t\t<className>com.ing.coins.mett.application.SocketPump</className>\n\t\t\t<unique-id>1</unique-id>\n\t\t\t<enabled>true</enabled>\n\t\t\t<attributes>\n\t\t\t\t<attribute>\n\t\t\t\t\t<name>port</name>\n\t\t\t\t\t<value>55555</value>\n\t\t\t\t</attribute>\n\t\t\t</attributes>\n\t\t</listener-pump>\n\t\t<listener-pump>\n\t\t\t<className>com.ing.coins.mett.application.FilePump</className>\n\t\t\t<unique-id>2</unique-id>\n\t\t\t<enabled>true</enabled>\n\t\t\t<attributes>\n\t\t\t\t<attribute>\n\t\t\t\t\t<name>loggerCategory</name>\n\t\t\t\t\t<value>MettLogger</value>\n\t\t\t\t</attribute>\n\t\t\t\t<attribute>\n\t\t\t\t\t<name>closeLogManagerOnDeregister</name>\n\t\t\t\t\t<value>false</value>\n\t\t\t\t</attribute>\n\t\t\t</attributes>\n\t\t</listener-pump>\n\t</listener-pumps>\n</mett-server>";	
	private static File file = null;
	private static boolean isStarted = false;

	public static void startTracing(String serverConfigFile) throws TracingException {
		if (isStarted) {
			throw new TracingException("Monitor is already started");
		}
		try {
			// do not move package name to imports, that makes inclusion of mett-server.jar required
			com.ing.coins.mett.application.MonitorAccessor.start(serverConfigFile);
		} catch (Throwable t) {
			throw new TracingException("Could not start tracing from config file ["+serverConfigFile+"]", t);
		}
		isStarted = true;
	}

	public static void startTracing() throws TracingException {
		if (isStarted) {
			throw new TracingException("Monitor is already started");
		}
		try {
			file = File.createTempFile("mett-server", ".xml", new File(AppConstants.getInstance().getResolvedProperty("logging.path")));
			FileOutputStream fos = new FileOutputStream(file.getPath());
			fos.write(properties.getBytes());
			fos.close();
		} catch (IOException e) {
			throw new TracingException("Error creating tracing configuration file", e);
		}
		file.deleteOnExit();
		startTracing(file.getPath());
	}

	public static void stopTracing() throws TracingException {
		if (!isStarted) {
			throw new TracingException("Monitor is already stopped");
		}
		com.ing.coins.mett.application.MonitorAccessor.stop();
		isStarted = false;
		if (file != null) {
			file.delete();
		}
	}
	
	public static void beforeEvent(Object o) {
		if (o instanceof TracingEventNumbers) {
			eventOccurred(((TracingEventNumbers)o).getBeforeEvent());
		}
	}

	public static void afterEvent(Object o) {
		if (o instanceof TracingEventNumbers) {
			eventOccurred(((TracingEventNumbers)o).getAfterEvent());
		}
	}

	public static void exceptionEvent(Object o) {
		if (o instanceof TracingEventNumbers) {
			eventOccurred(((TracingEventNumbers)o).getExceptionEvent());
		}
	}

	
	protected static void eventOccurred(int eventNr) {
		if (eventNr>=0) {
			postEventToMett(eventNr);
		}
	}

	private static void postEventToMett(int eventNr) {
		try {
			// do not move package name to imports, that makes inclusion of mett-server.jar required
			com.ing.coins.mett.application.MonitorAccessor.eventOccurred(eventNr);
		} catch (Throwable t) {
			log.warn("Exception occured posting METT event",t);
		}
	}

	public static void setProperties(String props) throws TracingException {
		if (isStarted) {
			throw new TracingException("Altering properties only allowed when monitor is stopped");
		}
		properties = props;
	}

	public static String getProperties() throws IOException {
		return properties;
	}

	public static boolean isStarted() {
		return isStarted;
	}
}
