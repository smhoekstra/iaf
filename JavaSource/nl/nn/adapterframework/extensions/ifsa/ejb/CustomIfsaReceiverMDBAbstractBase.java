/*
 * $Log: CustomIfsaReceiverMDBAbstractBase.java,v $
 * Revision 1.1.2.2  2007-11-12 12:41:27  europe\M00035F
 * Use LogUtil for obtaining logger
 *
 * Revision 1.1.2.1  2007/11/02 11:47:05  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add custom versions of IFSA MDB Receiver beans, and subclass of IFSA ServiceLocatorEJB
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.exceptions.ConnectionException;
import com.ing.ifsa.provider.Receiver;
import com.ing.ifsa.provider.ServiceLocator;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.jms.Message;
import javax.jms.MessageListener;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;


/**
 * Abstract base class for custom replacement of IFSA FF/RR Receiver MDB.
 * 
 * The reason for the custom subclass is to be able to create an instance
 * of a subclassed ServiceLocatorEJB which looks up a bean of our own
 * naming convention instead of a bean per IFSA Service name.
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public abstract class CustomIfsaReceiverMDBAbstractBase implements MessageDrivenBean, MessageListener {
    private static final Logger log = LogUtil.getLogger(CustomIfsaReceiverMDBAbstractBase.class);
    protected static ServiceLocator serviceLocator = createServiceLocator();
    
    protected MessageDrivenContext ejbContext;
    protected Receiver receiver;
    
    protected static ServiceLocator createServiceLocator() {
        return new CustomIfsaServiceLocatorEJB();
    }
    public void ejbCreate() throws CreateException, EJBException {
        if(log.isInfoEnabled()) {
            log.info(">>> ejbCreate()");
        }
        try {
            receiver = createReceiver();
            receiver.connect();
        }
        catch(ConnectionException e) {
            log.fatal("Connection failed", e);
            throw new CreateException(e.toString());
        }
        if(log.isInfoEnabled()) {
            log.info("<<< ejbCreate");
        }
    }

    public void ejbRemove() throws EJBException {
        if(log.isInfoEnabled()) {
            log.info(">>> ejbRemove()");
        }
    }

    public MessageDrivenContext getMessageDrivenContext() {
        return ejbContext;
    }

    public void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException {
        ejbContext = ctx;
    }

    public abstract void onMessage(Message msg);

    protected abstract Receiver createReceiver();

}
