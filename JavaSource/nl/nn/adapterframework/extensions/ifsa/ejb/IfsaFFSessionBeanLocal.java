/*
 * $Log: IfsaFFSessionBeanLocal.java,v $
 * Revision 1.1.2.1  2007-10-29 12:25:34  europe\M00035F
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.FireForgetService;
import javax.ejb.EJBLocalObject;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaFFSessionBeanLocal extends FireForgetService, EJBLocalObject {

}
