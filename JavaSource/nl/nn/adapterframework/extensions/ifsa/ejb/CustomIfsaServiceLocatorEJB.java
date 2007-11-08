/*
 * $Log: CustomIfsaServiceLocatorEJB.java,v $
 * Revision 1.1.2.3  2007-11-08 09:47:54  europe\M00035F
 * Look up bean from JNDI instead of via service-lookup method (since our JNDI name is not an IFSA service id!)
 *
 * Revision 1.1.2.2  2007/11/02 11:48:36  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add JavaDoc comment
 *
 * Revision 1.1.2.1  2007/11/02 11:47:06  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add custom versions of IFSA MDB Receiver beans, and subclass of IFSA ServiceLocatorEJB
 *
 *
 * $Id: CustomIfsaServiceLocatorEJB.java,v 1.1.2.3 2007-11-08 09:47:54 europe\M00035F Exp $
 *
 */
package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.FireForgetService;
import com.ing.ifsa.api.RequestReplyService;
import com.ing.ifsa.internal.exceptions.InvalidServiceException;
import com.ing.ifsa.internal.exceptions.UnknownServiceException;
import com.ing.ifsa.provider.ServiceLocatorEJB;
import com.ing.ifsa.utils.NamingHelper;
import java.lang.reflect.Method;
import javax.ejb.EJBHome;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import org.apache.log4j.Logger;

/**
 * Override the IFSA SeriveLocatorEJB implementation to return the IBIS
 * Service Dispatcher session bean for all IFSA service URLs.
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public class CustomIfsaServiceLocatorEJB extends ServiceLocatorEJB {
    private final static Logger log = Logger.getLogger(CustomIfsaServiceLocatorEJB.class);
    
    public final static String SERVICE_DISPATCHER_EJB_NAME = "java:comp/env/ejb/ibis/ServiceDispatcher";
    
    public FireForgetService getFireForgetService(String service) throws UnknownServiceException, InvalidServiceException {
        try {
            return super.getFireForgetService(service);
        } catch (UnknownServiceException e) {
            log.warn("Can not find EJB Bean for FF service [" + service + "], will look up generic FF service EJB", e);
            return (FireForgetService) getBeanFromJNDI(SERVICE_DISPATCHER_EJB_NAME);
        }
    }

    public RequestReplyService getRequestReplyService(String service) throws UnknownServiceException, InvalidServiceException {
        try {
            return super.getRequestReplyService(service);
        } catch (UnknownServiceException e) {
            log.warn("Can not find EJB Bean for RR service [" + service + "], will look up generic RR service EJB", e);
            return (RequestReplyService) getBeanFromJNDI(SERVICE_DISPATCHER_EJB_NAME);
        }
    }
    
    protected Object getBeanFromJNDI(String beanHomeJNDIName) throws UnknownServiceException, InvalidServiceException {
        try {
            Object obj = NamingHelper.getInstance().lookup(beanHomeJNDIName);
            EJBHome svcHome = (EJBHome) PortableRemoteObject.narrow(obj, javax.ejb.EJBHome.class);
            
            Class homeClass = svcHome.getClass();
            Method createMethod = homeClass.getMethod("create", null);
            Object remoteSvc = createMethod.invoke(svcHome, new Object[0]);
            return remoteSvc;
        } catch(ClassCastException e) {
            throw new InvalidServiceException("Can not find bean home interface ["
                    + beanHomeJNDIName + "]", e);
        } catch(NameNotFoundException e) {
            throw new UnknownServiceException("Can not find bean home interface ["
                    + beanHomeJNDIName + "]", e);
        } catch (NamingException e) {
            throw new UnknownServiceException("JNDI error looking up bean home interface ["
                    + beanHomeJNDIName + "]", e);
        } catch (Exception e) {
            throw new InvalidServiceException("Can not create bean ["
                    + beanHomeJNDIName + "]", e);
        }
    }
}
