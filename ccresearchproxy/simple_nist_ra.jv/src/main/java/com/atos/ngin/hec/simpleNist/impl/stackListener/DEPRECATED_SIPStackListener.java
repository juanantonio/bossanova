package com.atos.ngin.hec.simpleNist.impl.stackListener;

//-------------------------------------------------------------------------
//Project:      WO
//-------------------------------------------------------------------------
//Java File:    SIPStack.java
//
//Description:
//
//Implementation of SIPStack class.
//
//-------------------------------------------------------------------------
//Version Control:
//
//$Log: SIPStack.java,v $
//Revision 1.23  2009/05/04 09:00:43  rmlob
//Adding additional listening points to the provider
//
//Revision 1.22  2007/02/01 12:13:06  ftv
//virtaulLegs & transfer
//
//Revision 1.21  2007/02/01 08:48:59  olp
//wrong branch update solved
//
//Revision 1.19  2006/07/03 14:41:05  ftv
//close & shutdown
//
//Revision 1.18  2006/06/22 13:02:33  olp
//mispelled method
//
//Revision 1.17  2006/06/16 15:24:24  ftv
//quit ex.printStackTrace
//
//Revision 1.16  2006/06/16 15:14:00  ftv
//traces & quit RETRANSMISSION_FILTER
//
//Revision 1.15  2006/06/14 15:39:42  ftv
//radvision parameters
//
//Revision 1.14  2006/06/07 14:14:51  ftv
//new serverTransaction if null
//
//Revision 1.13  2006/06/07 10:22:05  ftv
//quit traces in dialog & transaction terminated
//
//Revision 1.12  2006/06/06 14:33:58  ftv
//clear version
//
//Revision 1.11  2006/05/26 14:01:41  ftv
//ifat
//
//Revision 1.10  2006/05/17 14:12:07  ftv
//notifyTimeoutEvent
//
//Revision 1.9  2006/05/12 11:26:31  ftv
//REENTRANT_LISTENER
//
//Revision 1.8  2006/05/03 13:33:43  olp
//added get communication stack
//
//Revision 1.7  2006/04/28 13:36:56  ftv
//get route from configuration file
//
//Revision 1.6  2006/04/21 10:55:36  ftv
//shutdown
//
//Revision 1.5  2006/04/21 10:34:58  ftv
//Version before clean
//
//Revision 1.4  2006/02/24 11:16:13  ftv
//call manager version
//
//Revision 1.3  2006/02/17 11:20:30  olp
//added configuration of parameters
//
//Revision 1.2  2006/02/15 11:57:29  ftv
//New agent version
//
//-------------------------------------------------------------------------
//Author: $Author: jacar $      AtosOrigin
//-------------------------------------------------------------------------

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.Transaction;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.atos.ngin.hec.simpleNist.impl.SimpleNISTResourceAdaptor;


/**
 * 
 * SIP Stack.
 * 
 * @author AtosOrigin (Network Engineering Group)
 * @version
 */

public class DEPRECATED_SIPStackListener implements SipListener {
	
	// should these be statics: what about multiple instances?
	public AddressFactory addressFactory;

	public MessageFactory messageFactory;

	public HeaderFactory headerFactory;
	
	public int stackPort    = 5060;
	private String stackAddress = "localhost";
	private String stackUri = stackAddress + ":" + stackPort;
	private String stackRoute = null;
	private String stackTransport = "udp";

	public  SipProvider sipProvider;	
	private ListeningPoint lp = null;
	
	private int stackThreads = 10;
	private int stackTraceLevel = 0;
	private String stackDebugLog = "spe2debug.txt";
	private String stackServerLog = "spe2log.txt";
	private String stackPathName = "gov.nist";
	
	private boolean closing = false;
	/**
	 * log4j.
	 */
	private Logger logger; 	

	private SipStack sipStack;
	
	private SIPEventNotifier notifier;
	
    public DEPRECATED_SIPStackListener(SIPEventNotifier notifier) {
		this.notifier = notifier;		
	}
    
    public Object getStack()
    {
        return sipStack;
    }
	

	
	/**
	 * Process request
	 * 
	 * @param requestEvent
	 *            Request Event
	 */
	public void processRequest(RequestEvent requestEvent) {
		Request request = requestEvent.getRequest();
		ServerTransaction serverTransactionId = requestEvent
				.getServerTransaction();
		

		if (logger.isDebugEnabled())
			logger.debug("Request " + request + " received at "
					+ sipStack.getStackName() + " with server transaction id "
					+ serverTransactionId);

		//if ( (serverTransactionId == null) && request.getMethod().equals(Request.INVITE) ) 
		//		if ( (serverTransactionId == null) && !request.getMethod().equals(Request.ACK) )
		//		{
		//			try {
		//				SipProvider sipProvider = (SipProvider) requestEvent
		//						.getSource();
		//				serverTransactionId = sipProvider
		//						.getNewServerTransaction(request);
		//			} catch (Exception ex) {
		//				//System.out.println(ex);
		//				logger.warn("Probably transaction already exists=" + ex);
		//				//ex.printStackTrace();
		//			}
		//		}

		if (notifier != null)
			notifier.notifyRequestEvent(requestEvent, serverTransactionId);

	}

	/**
	 * Process response
	 * 
	 * @param responseReceivedEvent
	 *            Response Event
	 */
	public void processResponse(ResponseEvent responseReceivedEvent) {
		Response response = responseReceivedEvent.getResponse();
		ClientTransaction clientTransactionId = responseReceivedEvent
				.getClientTransaction();

		/*
		 * System.out.println( "\n\nResponse " + response + " received at " +
		 * sipStack.getStackName() + " with server transaction id " +
		 * clientTransactionId);
		 */
		
//		ResponseThread rt = new ResponseThread(responseReceivedEvent, notifier);
//		try
//		{
//			threadPool.assign(rt);
//			return;
//		}
//		catch (Exception ex)
//		{
//			logger.error("Exception:" + ex.getStackTrace());
//		}

		if (logger.isDebugEnabled())
			logger.debug("Response " + response + " received at "
					+ sipStack.getStackName() + " with server transaction id "
					+ clientTransactionId);

		if (notifier != null)
			notifier.notifyResponseEvent(responseReceivedEvent);
	}

//	/**
//	 * Process ACK request
//	 * 
//	 * @param requestEvent
//	 *            Request Event
//	 * @param serverTransaction
//	 *            Server Transaction
//	 */
//	public void processAck(RequestEvent requestEvent,
//			ServerTransaction serverTransaction) {
//	}
//
//	/**
//	 * Process INVITE request
//	 * 
//	 * @param requestEvent
//	 *            Request Event
//	 * @param serverTransaction
//	 *            Server Transaction
//	 */
//	public void processInvite(RequestEvent requestEvent,
//			ServerTransaction serverTransaction) {
//	}
//
//	/**
//	 * Process BYE request
//	 * 
//	 * @param requestEvent
//	 *            Request Event
//	 * @param serverTransaction
//	 *            Server Transaction
//	 */
//	public void processBye(RequestEvent requestEvent,
//			ServerTransaction serverTransactionId) {
//	}

	/**
	 * Process time out
	 * 
	 * @param timeoutEvent
	 *            Timeout Event
	 */
	public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
		logger.warn("Timeout " + timeoutEvent + " received at "
				+ sipStack.getStackName());

		Transaction tr = null;
		ServerTransaction st = timeoutEvent.getServerTransaction();
		if (st != null)
		{
			logger.warn("Request= " + st.getRequest().getMethod()
							+ " - for Dialog="
							+ st.getDialog().getCallId().getCallId());
			tr = st;
		}

		ClientTransaction ct = timeoutEvent.getClientTransaction();
		if (ct != null)
		{
			logger.warn("Request= " + ct.getRequest().getMethod()
							+ " - for Dialog="
							+ ct.getDialog().getCallId().getCallId());
			tr = ct;
		}
		
		if (notifier != null)
				notifier.notifyTimeoutEvent(timeoutEvent, tr);
	}
	
	/**
	 * Process IOException
	 * 
	 * @param exceptionEvent
	 *            Exception Event
	 */
	public void processIOException(IOExceptionEvent exceptionEvent)
	{
		logger.error("IOException " + exceptionEvent + " received at "
				+ sipStack.getStackName());
	}
	
	/**
	 * Process Transaction Terminated
	 * 
	 * @param transactionTerminatedEvent
	 *            Transaction Terminated Event
	 */
	public void processTransactionTerminated(TransactionTerminatedEvent 
            transactionTerminatedEvent)
	{
		if (transactionTerminatedEvent.getClientTransaction() != null)
			if (logger.isDebugEnabled())logger.debug("Client TransactionTerminated " + transactionTerminatedEvent.getClientTransaction().getDialog().getCallId().getCallId() + " received at "
				+ sipStack.getStackName());
		else
			if (logger.isDebugEnabled())logger.debug("Server TransactionTerminated " + transactionTerminatedEvent.getServerTransaction().getDialog().getCallId().getCallId() + " received at "
					+ sipStack.getStackName());
	}

	/**
	 * Process Dialog Terminated
	 * 
	 * @param dialogTerminatedEvent
	 *            Dialog Terminated Event
	 */
	public void processDialogTerminated(DialogTerminatedEvent 
            dialogTerminatedEvent)
	{
		if (logger.isDebugEnabled())logger.debug("DialogTerminated " + dialogTerminatedEvent.getDialog().getCallId().getCallId() + " received at "
				+ sipStack.getStackName());
	}
	
	/**
	 * Shutdown
	 */
	public void shutdown() throws Exception
	{
		sipStack.stop();
		sipProvider.removeSipListener(this);
		sipProvider.removeListeningPoint(lp);
		sipStack.deleteListeningPoint(lp);
		sipStack.deleteSipProvider(sipProvider);
		
		sipProvider = null;
		messageFactory = null;
		headerFactory = null;
	}
	
	/**
	 * Set the state as closing
	 */
	public void close() throws Exception
	{
		closing = true;
	
	}

	/**
	 * Init the stack
	 */
	public SipStack init(String configFilePath, SimpleNISTResourceAdaptor ra) {

		SipFactory sipFactory = null;
		sipStack = null;		
		logger = Logger.getLogger(ra.getRaContext().getEntityName()+".SIPStackListener");		
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName(stackPathName);
		
		Properties properties = new Properties();		
        //try retrieve data from file
		try 
		{
		
			//			properties.load(new FileInputStream("../etc/cc_stack.properties"));
			if (logger.isInfoEnabled())logger.info("Loading stack properties file: "+configFilePath);
			properties.load(new FileInputStream(configFilePath));
			
		            
		}
		catch(IOException e)
		{
			logger.error("Could not find properties file. Using default values");
			//			e.printStackTrace();
			properties.setProperty("com.atos.cc.IP_ADDRESS", stackAddress);
			//en la version 1.2 no sirve
			//properties.setProperty("javax.sip.RETRANSMISSION_FILTER", "true");
			properties.setProperty("javax.sip.STACK_NAME", "spe");
			properties.setProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS", "false");
			
			// properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT","off");
			// You need 16 for logging traces. 32 for debug + traces.
			properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", ""+stackTraceLevel);
			properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", stackDebugLog);
			properties.setProperty("gov.nist.javax.sip.SERVER_LOG", stackServerLog);
			// Guard against starvation.
			//properties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
			//properties.setProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE", "4096");
			//properties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS", "false");
			//properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
			properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");
			properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", ""+stackThreads);
			//properties.setProperty("gov.nist.javax.sip.MAX_CONNECTIONS", "5");			
			//properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
					
		}		
		if (logger.isInfoEnabled())logger.info("Stack properties: "+properties.toString());

		try {
			// Create SipStack object
			String deprecatedStackAddressParam = properties.getProperty("javax.sip.IP_ADDRESS"); //
			if (deprecatedStackAddressParam != null)
			{
				logger.error("javax.sip.IP_ADDRESS property is no longer valid. Remove it, please, and use com.atos.cc.IP_ADDRESS instead. Ignoring anyway.");				
				properties.remove("javax.sip.IP_ADDRESS"); //
			}
			stackAddress = properties.getProperty("com.atos.cc.IP_ADDRESS");
			stackTransport = properties.getProperty("com.atos.cc.STACK_TRANSPORT");
			stackPort = Integer.parseInt(properties.getProperty("com.atos.cc.STACK_PORT"));
			
			sipStack = sipFactory.createSipStack(properties);
			if (logger.isInfoEnabled())logger.info("sipStack = " + sipStack + ", address = "
					+ stackAddress + ", port = " + stackPort);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception= " + e.getMessage());
			if (e.getCause() != null)
				e.getCause().printStackTrace();
			return null;
		}

		try {
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
			lp = sipStack.createListeningPoint(stackAddress, stackPort, stackTransport);
			
			DEPRECATED_SIPStackListener listener = this;

			sipProvider = sipStack.createSipProvider(lp);
			sipProvider.addSipListener(listener);
						
			sipStack.start();
			
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Exception=" + ex.getMessage());
			if (ex.getCause() != null)
				ex.getCause().printStackTrace();
			return null;
		}
		return sipStack;
	}

	public int getStackPort()
	{
		return stackPort;
	}

	public String getStackAddress()
	{
		return stackAddress;
	}

	public String getStackTransport()
	{
		return stackTransport;
	}

}
