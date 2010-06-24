/*
 * $Log: LogSender.java,v $
 * Revision 1.6.2.1  2010-06-24 15:27:11  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.6  2010/03/10 14:30:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.4  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.3  2009/11/18 17:28:03  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.2  2009/09/07 13:32:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use log from ancestor
 *
 * Revision 1.1  2008/08/06 16:36:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.IParameterHandler;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

/**
 * Sender that just logs its message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>level on which messages are logged</td><td>info</td></tr>
 * <tr><td>{@link #setLogCategory(String) logCategory}</td><td>category under which messages are logged</td><td>name of the sender</td></tr>
 * </table>
 * 
 * @author Gerrit van Brakel
 * @since  4.9
 * @version Id
 */
public class LogSender extends SenderWithParametersBase implements IParameterHandler {
	private String logLevel="info";
	private String logCategory=null;

	protected Level level;

	public void configure() throws ConfigurationException {
		super.configure();
		log=LogUtil.getLogger(getLogCategory());
		level=Level.toLevel(getLogLevel());
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		log.log(level,message);
		if (prc != null) {
			try {
				prc.forAllParameters(paramList, this);
			} catch (ParameterException e) {
				throw new SenderException("exception determining value of parameters", e);
			}
		}
		return message;
	}

	public void handleParam(String paramName, Object value) {
		log.log(level,"parameter [" + paramName + "] value [" + value + "]");
	}

	public String getLogCategory() {
		if (StringUtils.isNotEmpty(logCategory)) {
			return logCategory;
		}
		if (StringUtils.isNotEmpty(getName())) {
			return getName();
		}
		return this.getClass().getName();
	}

	public void setLogCategory(String string) {
		logCategory = string;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String string) {
		logLevel = string;
	}

	public String toString() {
		return "LogSender ["+getName()+"] logLevel ["+getLogLevel()+"] logCategory ["+logCategory+"]";
	}

}
