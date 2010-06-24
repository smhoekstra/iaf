/*
 * $Log: SenderProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:11  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.SenderWrapperBase;

/**
 * @author Jaco de Groot
 * @version Id
 */
public interface SenderProcessor {

	public String sendMessage(ISender sender, String correlationID,
			Object message, PipeLineSession pipeLineSession,
			boolean namespaceAware) throws SenderException, TimeOutException;

}
