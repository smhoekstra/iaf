/*
 * $Log: IfsaProviderListener.java,v $
 * Revision 1.1.2.2  2007-10-29 09:33:00  europe\M00035F
 * Refactor: pullup a number of methods to abstract base class so they can be shared between IFSA parts
 *
 * Revision 1.1.2.1  2007/10/25 15:03:44  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Begin work on implementing IFSA-EJB
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import java.util.Map;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public class IfsaProviderListener extends IfsaEjbBase implements IPushingListener {
    private IMessageHandler handler;
    private IbisExceptionListener exceptionListener;
    public void setHandler(IMessageHandler handler) {
        this.handler = handler;
    }

    public void setExceptionListener(IbisExceptionListener listener) {
        this.exceptionListener = listener;
    }

    public void configure() throws ConfigurationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void open() throws ListenerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close() throws ListenerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getStringFromRawMessage(Object rawMessage, Map context) throws ListenerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
        // Nothing to do here
        return;
    }

    public IbisExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    public IMessageHandler getHandler() {
        return handler;
    }

}
