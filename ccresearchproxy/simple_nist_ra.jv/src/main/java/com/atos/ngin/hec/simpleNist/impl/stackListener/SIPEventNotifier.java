package com.atos.ngin.hec.simpleNist.impl.stackListener;

//-------------------------------------------------------------------------
//Project:      WO2
//-------------------------------------------------------------------------
//Java File:    SIPEventNotifier.java
//
//Description:
//
//Implementation of SIPEventNotifier class.
//
//-------------------------------------------------------------------------
//Version Control:
//
//$Log: SIPEventNotifier.java,v $
//Revision 1.3  2006/05/17 14:10:57  ftv
//notifyTimeoutEvent
//
//Revision 1.2  2006/02/15 11:57:29  ftv
//New agent version
//
//-------------------------------------------------------------------------
//Author: $Author: womk $      AtosOrigin
//-------------------------------------------------------------------------

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.ServerTransaction;

/**
*
*  SIPStack uses this notifier to communicate SIPAgent the reception of network event.
*
*  @author         AtosOrigin (Network Engineering Group)
*  @version
*/

public interface SIPEventNotifier {

	/**
	 * Notify Request Event
	 * @param event Event
	 * @param st Server Transaction
	 */
	public void notifyTimeoutEvent(TimeoutEvent event, Transaction tr);
	
	/**
	 * Notify Timeout Event
	 * @param event Event
	 * @param st Server Transaction
	 */
	public void notifyRequestEvent(RequestEvent event, ServerTransaction st);
	
	/**
	 * @param event Event 
	 * @param event
	 */
	public void notifyResponseEvent(ResponseEvent event);
}
