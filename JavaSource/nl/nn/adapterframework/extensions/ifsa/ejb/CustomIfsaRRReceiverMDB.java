/*
 * $Log: CustomIfsaRRReceiverMDB.java,v $
 * Revision 1.1.2.1  2007-11-02 11:47:06  europe\M00035F
 * Add custom versions of IFSA MDB Receiver beans, and subclass of IFSA ServiceLocatorEJB
 *
 *
 * $Id: CustomIfsaRRReceiverMDB.java,v 1.1.2.1 2007-11-02 11:47:06 europe\M00035F Exp $
 *
 */
package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.provider.RRReceiver;
import com.ing.ifsa.provider.Receiver;
import javax.jms.Message;
import org.apache.log4j.Logger;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public class CustomIfsaRRReceiverMDB extends CustomIfsaReceiverMDBAbstractBase {
    private static final Logger log = Logger.getLogger(CustomIfsaRRReceiverMDB.class);

    public void onMessage(Message msg) {
        if (log.isInfoEnabled()) {
            log.info(">>> onMessage()");
        }
        if (!((RRReceiver) receiver).handleMessage(msg)) {
            log.warn("message was not handled succesfully, rollback transaction");
            getMessageDrivenContext().setRollbackOnly();
        }
        if (log.isInfoEnabled()) {
            log.info("<<< onMessage");
        }
    }

    protected Receiver createReceiver() {
        return new RRReceiver(serviceLocator);
    }

}