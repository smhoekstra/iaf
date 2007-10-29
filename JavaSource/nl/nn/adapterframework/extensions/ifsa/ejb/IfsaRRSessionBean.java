/*
 * $Log: IfsaRRSessionBean.java,v $
 * Revision 1.1.2.1  2007-10-29 12:25:34  europe\M00035F
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.BusinessMessage;
import com.ing.ifsa.api.RequestReplyService;
import com.ing.ifsa.api.ServiceReply;
import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.exceptions.ServiceException;
import java.rmi.RemoteException;
import javax.ejb.SessionBean;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public class IfsaRRSessionBean extends IfsaEjbBeanBase implements SessionBean, RequestReplyService
{

    public ServiceReply onServiceRequest(ServiceRequest request) throws RemoteException, ServiceException {
        String replyText = processRequest(request);

        ServiceReply reply = new ServiceReply(request, new BusinessMessage(replyText));
        return reply;
    }

}
