/*
 * $Log: IbisManagerEjbLocal.java,v $
 * Revision 1.2.2.1  2007-10-24 09:39:47  europe\M00035F
 * Merge changes from HEAD
 *
 * Revision 1.2  2007/10/09 16:07:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import javax.ejb.EJBLocalObject;

import nl.nn.adapterframework.configuration.IbisManager;

/**
 * Local interface for Enterprise Bean: IbisManagerEjb
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IbisManagerEjbLocal extends IbisManager, EJBLocalObject {
	
}
