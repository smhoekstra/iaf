/*
 * $Log: CorePipeProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:11  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class CorePipeProcessor implements PipeProcessor {
	
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		return pipe.doPipe(message, pipeLineSession);
	}

}
