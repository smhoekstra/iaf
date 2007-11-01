/*
 * $Log: IfsaFFSessionBeanRemote.java,v $
 * Revision 1.1.2.1  2007-11-01 10:35:25  europe\M00035F
 * Add remote interfaces for IFSA Session beans, since that is what's expected by the IFSA libraries
 *
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.FireForgetService;
import javax.ejb.EJBObject;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaFFSessionBeanRemote extends FireForgetService, EJBObject {

}
