/*
 * $Log: IfsaRRSessionBeanLocalHome.java,v $
 * Revision 1.1.2.1  2007-10-29 12:25:34  europe\M00035F
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import javax.ejb.EJBLocalHome;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaRRSessionBeanLocalHome extends EJBLocalHome {
    public IfsaRRSessionBeanLocal create()
        throws javax.ejb.CreateException;

}
