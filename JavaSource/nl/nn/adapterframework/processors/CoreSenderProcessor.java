/*
 * $Log: CoreSenderProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:10  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderWrapperBase;

/**
 * @version Id
 */
public class CoreSenderProcessor implements SenderProcessor {

	public String sendMessage(ISender sender, String correlationID,
			Object message, PipeLineSession pipeLineSession,
			boolean namespaceAware) throws SenderException, TimeOutException {
		if (sender instanceof SenderWrapperBase) {
			SenderWrapperBase senderWrapperBase = (SenderWrapperBase)sender;
			return senderWrapperBase.sendMessage(correlationID, message, pipeLineSession, namespaceAware);
		} else if (sender instanceof ISenderWithParameters) { // do not only check own parameters, sender may have them by itself
			ISenderWithParameters psender = (ISenderWithParameters) sender;
			ParameterResolutionContext prc = new ParameterResolutionContext((String)message, pipeLineSession, namespaceAware);
			return psender.sendMessage(correlationID, (String) message, prc);
		} 
		return sender.sendMessage(correlationID, (String) message);
	}

}
