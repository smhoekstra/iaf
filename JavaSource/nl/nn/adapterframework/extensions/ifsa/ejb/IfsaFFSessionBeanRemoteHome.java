/*
 * $Log: IfsaFFSessionBeanRemoteHome.java,v $
 * Revision 1.1.2.1  2007-11-01 10:35:24  europe\M00035F
 * Add remote interfaces for IFSA Session beans, since that is what's expected by the IFSA libraries
 *
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import javax.ejb.EJBHome;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaFFSessionBeanRemoteHome extends EJBHome {
    public IfsaFFSessionBeanLocal create()
        throws javax.ejb.CreateException;

}
