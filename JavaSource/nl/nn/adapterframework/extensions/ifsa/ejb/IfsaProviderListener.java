/*
 * $Log: IfsaProviderListener.java,v $
 * Revision 1.1.2.4  2007-11-05 13:51:37  europe\M00035F
 * Add 'version' string to new IFSA classes
 *
 * Revision 1.1.2.3  2007/10/29 12:25:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * Revision 1.1.2.2  2007/10/29 09:33:00  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor: pullup a number of methods to abstract base class so they can be shared between IFSA parts
 *
 * Revision 1.1.2.1  2007/10/25 15:03:44  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Begin work on implementing IFSA-EJB
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.ServiceRequest;
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
    public static final String version = "$RCSfile: IfsaProviderListener.java,v $ $Revision: 1.1.2.4 $ $Date: 2007-11-05 13:51:37 $";
    
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
        ServiceRequest request = (ServiceRequest) rawMessage;
        return request.getUniqueId();
    }

    public String getStringFromRawMessage(Object rawMessage, Map context) throws ListenerException {
        ServiceRequest request = (ServiceRequest) rawMessage;
        return request.getBusinessMessage().getText();
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
