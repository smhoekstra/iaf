/*
 * $Log: JtaUtil.java,v $
 * Revision 1.13.8.1  2007-10-16 14:18:09  europe\M00035F
 * Apply changes required to use Spring based JmsListener, Receiver and to disable JtaUtil for commits, tx status checking
 *
 * Revision 1.13.2.1  2007/09/26 14:59:02  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Updates for more robust and correct transaction handling
 *
 * Revision 1.13  2007/08/10 11:22:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added non-argument inTransaction()
 *
 * Revision 1.12  2007/06/08 12:18:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do not rollback after exception on commit if status is already final
 *
 * Revision 1.11  2007/05/08 16:01:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed stacktrace from debug-logging while obtaining user-transaction
 *
 * Revision 1.10  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.9  2006/09/18 11:46:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * lookup UserTransaction only when necessary
 *
 * Revision 1.8  2006/09/14 11:47:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optimized transactionStateCompatible()
 *
 * Revision 1.7  2006/08/21 15:14:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of transaction attribute handling
 * configuration of user transaction url in appconstants.properties
 *
 * Revision 1.6  2005/09/08 15:58:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added logging
 *
 * Revision 1.5  2004/10/05 09:57:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made version public
 *
 * Revision 1.4  2004/03/31 15:03:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2004/03/26 10:42:38  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.1  2004/03/23 17:14:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import nl.nn.adapterframework.core.TransactionException;

import org.apache.log4j.Logger;

/**
 * Utility functions for JTA 
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JtaUtil {
	public static final String version="$RCSfile: JtaUtil.java,v $ $Revision: 1.13.8.1 $ $Date: 2007-10-16 14:18:09 $";
	private static Logger log = LogUtil.getLogger(JtaUtil.class);
	
	private static final String USERTRANSACTION_URL1_KEY="jta.userTransactionUrl1";
	private static final String USERTRANSACTION_URL2_KEY="jta.userTransactionUrl2";
	
	public static final int TRANSACTION_ATTRIBUTE_REQUIRED=0;
	public static final int TRANSACTION_ATTRIBUTE_REQUIRES_NEW=1;
	public static final int TRANSACTION_ATTRIBUTE_MANDATORY=2;
	public static final int TRANSACTION_ATTRIBUTE_NOT_SUPPORTED=3;
	public static final int TRANSACTION_ATTRIBUTE_SUPPORTS=4;
	public static final int TRANSACTION_ATTRIBUTE_NEVER=5;

	public static final int TRANSACTION_ATTRIBUTE_DEFAULT=TRANSACTION_ATTRIBUTE_SUPPORTS;

	public static final String TRANSACTION_ATTRIBUTE_REQUIRED_STR="Required";
	public static final String TRANSACTION_ATTRIBUTE_REQUIRES_NEW_STR="RequiresNew";
	public static final String TRANSACTION_ATTRIBUTE_MANDATORY_STR="Mandatory";
	public static final String TRANSACTION_ATTRIBUTE_NOT_SUPPORTED_STR="NotSupported";
	public static final String TRANSACTION_ATTRIBUTE_SUPPORTS_STR="Supports";
	public static final String TRANSACTION_ATTRIBUTE_NEVER_STR="Never";

	public static final String transactionAttributes[]=
		{ 
			TRANSACTION_ATTRIBUTE_REQUIRED_STR,
			TRANSACTION_ATTRIBUTE_REQUIRES_NEW_STR,
			TRANSACTION_ATTRIBUTE_MANDATORY_STR,
			TRANSACTION_ATTRIBUTE_NOT_SUPPORTED_STR, 
			TRANSACTION_ATTRIBUTE_SUPPORTS_STR,
			TRANSACTION_ATTRIBUTE_NEVER_STR
		};

    private static UserTransaction utx;
    
    private static class UtxWrapper implements UserTransaction {
        UserTransaction realUtx;

        public UtxWrapper(UserTransaction realUtx) {
            log.debug("Allocated wrapper around real JTA UserTransaction");
            this.realUtx = realUtx;
        }
        
        public void begin() throws NotSupportedException, SystemException {
            throw new UnsupportedOperationException("Not supported anymore; use Spring Transaction Manager instead.");
        }

        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            throw new UnsupportedOperationException("Not supported anymore; use Spring Transaction Manager instead.");
        }

        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            throw new UnsupportedOperationException("Not supported anymore; use Spring Transaction Manager instead.");
        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
            realUtx.setRollbackOnly();
        }

        public int getStatus() throws SystemException {
            return realUtx.getStatus();
        }

        public void setTransactionTimeout(int timeout) throws SystemException {
            realUtx.setTransactionTimeout(timeout);
        }
        
    }
	/**
	 * returns a meaningful string describing the transaction status.
	 */
	public static String displayTransactionStatus(int status) {
		switch (status) {
			case 	Status.STATUS_ACTIVE 			 : return status+"=STATUS_ACTIVE:"+ 	    " A transaction is associated with the target object and it is in the active state."; 
			case 	Status.STATUS_COMMITTED 		 : return status+"=STATUS_COMMITTED:"+ 	    " A transaction is associated with the target object and it has been committed."; 
			case 	Status.STATUS_COMMITTING 		 : return status+"=STATUS_COMMITTING:"+ 	" A transaction is associated with the target object and it is in the process of committing."; 
			case 	Status.STATUS_MARKED_ROLLBACK 	 : return status+"=STATUS_MARKED_ROLLBACK:"+" A transaction is associated with the target object and it has been marked for rollback, perhaps as a result of a setRollbackOnly operation."; 
			case 	Status.STATUS_NO_TRANSACTION 	 : return status+"=STATUS_NO_TRANSACTION:"+ " No transaction is currently associated with the target object.";
			case 	Status.STATUS_PREPARED 			 : return status+"=STATUS_PREPARED:"+ 	    " A transaction is associated with the target object and it has been prepared.";
			case 	Status.STATUS_PREPARING 		 : return status+"=STATUS_PREPARING:"+ 	    " A transaction is associated with the target object and it is in the process of preparing.";
			case 	Status.STATUS_ROLLEDBACK 		 : return status+"=STATUS_ROLLEDBACK:"+ 	" A transaction is associated with the target object and the outcome has been determined to be rollback.";
			case 	Status.STATUS_ROLLING_BACK 		 : return status+"=STATUS_ROLLING_BACK:"+ 	" A transaction is associated with the target object and it is in the process of rolling back.";
			case 	Status.STATUS_UNKNOWN 	 		 : return status+"=STATUS_UNKNOWN:"+ 	    " A transaction is associated with the target object but its current status cannot be determined.";
			default : return "unknown transaction status";
		}   
	}

	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(Transaction tx) {
		try {
			return displayTransactionStatus(tx.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transaction ["+tx+"]: "+e.getMessage();
		}
	}
	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(UserTransaction utx) {
		try {
			return displayTransactionStatus(utx.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transaction ["+utx+"]: "+e.getMessage();
		}
	}
	
	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(TransactionManager tm) {
		try {
			return displayTransactionStatus(tm.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transactionmanager ["+tm+"]: "+e.getMessage();
		}
	}

	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus() {
		UserTransaction utx;
		try {
			utx = getUserTransaction();
		} catch (Exception e) {
			return "exception obtaining user transaction: "+e.getMessage();
		}
		return displayTransactionStatus(utx);
	}


	/** 
	 * returns true if the current thread is associated with a transaction
	 */
	public static boolean inTransaction(UserTransaction utx) throws SystemException {
		return utx != null && utx.getStatus() != Status.STATUS_NO_TRANSACTION;
	}
	public static boolean inTransaction() throws SystemException, NamingException {
		return inTransaction(getUserTransaction());
	}

	/**
	 * Returns a UserTransaction object, that is used by Receivers and PipeLines to demarcate transactions. 
     * 
     * The UserTransaction is wrapped in a private class to prevent begin, commit and rollback operations
	 */
	public static UserTransaction getUserTransaction(Context ctx, String userTransactionUrl) throws NamingException {
	
		if (utx == null) {
			log.debug("looking up UserTransaction ["+userTransactionUrl+"] in context ["+ctx.toString()+"]");
			utx = new UtxWrapper((UserTransaction)ctx.lookup(userTransactionUrl));
		}
		return utx;
	}

	/**
	 * Returns a UserTransaction object, that is used by Receivers and PipeLines to demarcate transactions. 
     * 
     * The UserTransaction is wrapped in a private class to prevent begin, commit and rollback operations
	 */
	public static UserTransaction getUserTransaction() throws NamingException {
		if (utx == null) {
			Context ctx= (Context) new InitialContext();
			String url = AppConstants.getInstance().getProperty(USERTRANSACTION_URL1_KEY,null);
			log.debug("looking up UserTransaction ["+url+"] in context ["+ctx.toString()+"]");
			try {
				utx = new UtxWrapper((UserTransaction)ctx.lookup(url));
			} catch (Exception e) {
				log.debug("Could not lookup UserTransaction from url ["+url+"], will try alternative uri: "+e.getMessage());
				url = AppConstants.getInstance().getProperty(USERTRANSACTION_URL2_KEY,null);
				log.debug("looking up UserTransaction ["+url+"] in context ["+ctx.toString()+"]");
				utx = new UtxWrapper((UserTransaction)ctx.lookup(url));
			}
		}
		return utx;
	}

	
	public static int getTransactionAttributeNum(String transactionAttribute) {
		int i=transactionAttributes.length-1;
		while (i>=0 && !transactionAttributes[i].equalsIgnoreCase(transactionAttribute))
			i--; // try next
		return i; 
	}

	public static String getTransactionAttributeString(int transactionAttribute) {
		if (transactionAttribute<0 || transactionAttribute>=transactionAttributes.length) {
			return "UnknownTransactionAttribute:"+transactionAttribute;
		}
		return transactionAttributes[transactionAttribute];
	}
	
	public static boolean transactionStateCompatible(int transactionAttribute) throws SystemException, NamingException {
		if (transactionAttribute==TRANSACTION_ATTRIBUTE_NEVER) {
			return !inTransaction(getUserTransaction());
		} else if (transactionAttribute==TRANSACTION_ATTRIBUTE_MANDATORY) {
			return inTransaction(getUserTransaction());
		}
		return true;
	}

	public static boolean isolationRequired(int transactionAttribute) throws SystemException, TransactionException, NamingException {
		if (transactionAttribute!=TRANSACTION_ATTRIBUTE_REQUIRES_NEW &&
		    transactionAttribute!=TRANSACTION_ATTRIBUTE_NOT_SUPPORTED) {
		    	return false;
		}
		if (!transactionStateCompatible(transactionAttribute)) {
			throw new TransactionException("transaction attribute ["+getTransactionAttributeString(transactionAttribute)+"] not compatible with state ["+displayTransactionStatus(utx)+"]");
		}
		UserTransaction utx = getUserTransaction();
		return inTransaction(utx) &&
				(transactionAttribute==TRANSACTION_ATTRIBUTE_REQUIRES_NEW ||
				 transactionAttribute==TRANSACTION_ATTRIBUTE_NOT_SUPPORTED);
	}

	public static boolean newTransactionRequired(int transactionAttribute) throws SystemException, TransactionException, NamingException {
		if (!transactionStateCompatible(transactionAttribute)) {
			throw new TransactionException("transaction attribute ["+getTransactionAttributeString(transactionAttribute)+"] not compatible with state ["+displayTransactionStatus(utx)+"]");
		}
		if (transactionAttribute==TRANSACTION_ATTRIBUTE_REQUIRED) {
			UserTransaction utx = getUserTransaction();
			return !inTransaction(utx);
		}
		return transactionAttribute==TRANSACTION_ATTRIBUTE_REQUIRES_NEW;
	}

	private static boolean stateEvaluationRequired(int transactionAttribute) {
		return transactionAttribute>=0 && 
			   transactionAttribute!=TRANSACTION_ATTRIBUTE_REQUIRES_NEW &&
			   transactionAttribute!=TRANSACTION_ATTRIBUTE_SUPPORTS;
	}
	
	/**
     * This method has been deprecated and should no longer be used. Use Spring Transaction Framework instead.
     * @deprecated Use Spring Transaction Framework instead.
     */
    public static void startTransaction() throws NamingException, NotSupportedException, SystemException {
		log.debug("starting new transaction");
		utx=getUserTransaction();
		utx.begin();
	}

	/**
     * This method has been deprecated and should no longer be used. Use Spring Transaction Framework instead.
     * @deprecated Use Spring Transaction Framework instead.
     */
	public static void finishTransaction() throws NamingException, IllegalStateException, SecurityException, SystemException {
		finishTransaction(false);
	}
	
	/**
     * This method has been deprecated and should no longer be used. Use Spring Transaction Framework instead.
     * @deprecated Use Spring Transaction Framework instead.
     */
	public static void finishTransaction(boolean rollbackonly) throws NamingException, IllegalStateException, SecurityException, SystemException {
		utx=getUserTransaction();
		try {
			if (inTransaction(utx) && !rollbackonly) {
				log.debug("committing transaction");
				utx.commit();
			} else {
				log.debug("rolling back transaction");
				utx.rollback();
			}
		} catch (Throwable t1) {
			try {
				int currentStatus=-1;
				try {
					currentStatus=utx.getStatus();
				} catch (Throwable t) {
					log.debug("caught exception obtaining transaction status: "+ t.getMessage());
				}
				if (currentStatus != Status.STATUS_COMMITTED &&
					currentStatus != Status.STATUS_NO_TRANSACTION &&
					currentStatus != Status.STATUS_ROLLEDBACK &&
					currentStatus != Status.STATUS_ROLLING_BACK) {
						log.warn("current status ["+displayTransactionStatus(currentStatus)+"], trying to roll back transaction after exception ",t1);
						utx.rollback();
				} else {
					log.info("current status ["+displayTransactionStatus(currentStatus)+"], will not issue rollback command");
				}
			} catch (Throwable t2) {
				log.warn("exception rolling back transaction",t2);
			}
		}		
	}
}
