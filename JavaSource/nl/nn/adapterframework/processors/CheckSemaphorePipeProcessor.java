/*
 * $Log: CheckSemaphorePipeProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:11  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import java.util.Hashtable;
import java.util.Map;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.Semaphore;

/**
 * @version Id
 */
public class CheckSemaphorePipeProcessor implements PipeProcessor {
	private PipeProcessor pipeProcessor;
	private Map pipeThreadCounts=new Hashtable();

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		PipeRunResult pipeRunResult;
		Semaphore s = getSemaphore(pipe);
		if (s != null) {
			long waitingDuration = 0;
			try {
				// keep waiting statistics for thread-limited pipes
				long startWaiting = System.currentTimeMillis();
				s.acquire();
				waitingDuration = System.currentTimeMillis() - startWaiting;
				StatisticsKeeper sk = (StatisticsKeeper) pipeLine.getPipeWaitingStatistics().get(pipe.getName());
				sk.addValue(waitingDuration);
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
			} catch(InterruptedException e) {
				throw new PipeRunException(pipe, "Interrupted acquiring semaphore", e);
			} finally { 
				s.release();
			}
		} else { //no restrictions on the maximum number of threads (s==null)
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		}
		return pipeRunResult;
	}

	private Semaphore getSemaphore(IPipe pipe) {
		int maxThreads = pipe.getMaxThreads();
		if (maxThreads > 0) {
			Semaphore s;
			synchronized (pipeThreadCounts) {
				if (pipeThreadCounts.containsKey(pipe)) {
					s = (Semaphore) pipeThreadCounts.get(pipe);
				} else {
					s = new Semaphore(maxThreads);
					pipeThreadCounts.put(pipe, s);
				}
			}
			return s;
		}
		return null;
	}

}
