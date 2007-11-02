/*
 * $Log: CustomIfsaServiceLocatorEJB.java,v $
 * Revision 1.1.2.2  2007-11-02 11:48:36  europe\M00035F
 * Add JavaDoc comment
 *
 * Revision 1.1.2.1  2007/11/02 11:47:06  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add custom versions of IFSA MDB Receiver beans, and subclass of IFSA ServiceLocatorEJB
 *
 *
 * $Id: CustomIfsaServiceLocatorEJB.java,v 1.1.2.2 2007-11-02 11:48:36 europe\M00035F Exp $
 *
 */
package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.FireForgetService;
import com.ing.ifsa.api.RequestReplyService;
import com.ing.ifsa.internal.exceptions.InvalidServiceException;
import com.ing.ifsa.internal.exceptions.UnknownServiceException;
import com.ing.ifsa.provider.ServiceLocatorEJB;

/**
 * Override the IFSA SeriveLocatorEJB implementation to return the IBIS
 * Service Dispatcher session bean for all IFSA service URLs.
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public class CustomIfsaServiceLocatorEJB extends ServiceLocatorEJB {
    public final static String SERVICE_DISPATCHER_EJB_NAME = "java:comp/env/ejb/ibis/ServiceDispatcher";
    
    public FireForgetService getFireForgetService(String service) throws UnknownServiceException, InvalidServiceException {
        try {
            return super.getFireForgetService(service);
        } catch (UnknownServiceException e) {
            return super.getFireForgetService(SERVICE_DISPATCHER_EJB_NAME);
        }
    }

    public RequestReplyService getRequestReplyService(String service) throws UnknownServiceException, InvalidServiceException {
        try {
            return super.getRequestReplyService(service);
        } catch (UnknownServiceException e) {
            return super.getRequestReplyService(SERVICE_DISPATCHER_EJB_NAME);
        }
    }

}
