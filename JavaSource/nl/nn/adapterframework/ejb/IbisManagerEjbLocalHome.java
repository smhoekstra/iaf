/*
 * $Log: IbisManagerEjbLocalHome.java,v $
 * Revision 1.2.2.1  2007-10-24 09:39:47  europe\M00035F
 * Merge changes from HEAD
 *
 * Revision 1.2  2007/10/09 16:07:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

/**
 * Local Home interface for Enterprise Bean: IbisManagerEjb
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IbisManagerEjbLocalHome extends javax.ejb.EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: IbisManagerEjb
     */
    public IbisManagerEjbLocal create()
        throws javax.ejb.CreateException;
}
