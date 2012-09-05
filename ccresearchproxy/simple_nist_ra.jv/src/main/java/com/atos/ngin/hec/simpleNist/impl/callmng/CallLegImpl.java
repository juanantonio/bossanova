package com.atos.ngin.hec.simpleNist.impl.callmng;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.TS0_fake_header;

import java.util.concurrent.atomic.AtomicLong;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.ServerTransaction;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

import com.atos.ngin.hec.simpleNist.impl.SimpleNISTResourceAdaptor;
import com.atos.ngin.hec.simpleNist.type.CallLeg;
public class CallLegImpl implements CallLeg
{
	
	private static Logger logger = Logger.getLogger("simpleNIST.CallLegImpl");

	private CallActivityImpl callActivity; //
	private CallLegType callLegType; // Set in constructor
	private String callId;

	private ServerTransaction initialServerTransaction;
	private ClientTransaction initialClientTransaction;
	// Should we keep this?
	//	private ConcurrentHashMap<String, Transaction> pendingTransactions = new ConcurrentHashMap<String, Transaction>(10);
	// Or maybe something more specific?
	//	private ConcurrentHashMap<String, Transaction> pendingClientTransactions = new ConcurrentHashMap<String, Transaction>(10);
	//	private ConcurrentHashMap<String, Transaction> pendingServerTransactions = new ConcurrentHashMap<String, Transaction>(10);
	private Dialog dialog;

	private SimpleNISTResourceAdaptor ra;
	
	private CallLegState state = CallLegState.IDLE;
	

	private long lastEventTS = -1;
	private String lastEventType = null;
	private String lastEventCSeq = null;
	
	/**
	 * Constructor for INGRESS call leg
	 * 
	 * @param callId
	 * @param serverTransaction
	 * @param ra
	 */
	public CallLegImpl (String callId, ServerTransaction serverTransaction, SimpleNISTResourceAdaptor ra)
	{		
		callLegType = CallLegType.INGRESS;
		this.ra=ra;
		this.callId = callId;
		this.initialServerTransaction = serverTransaction;
		this.dialog = serverTransaction.getDialog();
	}
	/**
	 * Should be called immediately after the 'ingress' constructor
	 * @param callActivity
	 */
	public void setCallActivity(CallActivityImpl callActivity)
	{
		this.callActivity = callActivity;
		state = CallLegState.IDLE;
	}	
	
	/**
	 * Constructor for internally created EGRESS call leg
	 * 
	 * @param callActivity
	 * @param callId
	 * @param clientTransaction
	 * @param ra
	 */
	public CallLegImpl (CallActivityImpl callActivity, String callId, ClientTransaction clientTransaction, SimpleNISTResourceAdaptor ra)	
	{
		callLegType = CallLegType.EGRESS;
		this.callActivity = callActivity;
		this.ra=ra;
		this.callId = callId;
		this.initialClientTransaction = clientTransaction;
		this.dialog = clientTransaction.getDialog();
		callActivity.setEgressCallLeg(this);
		state = CallLegState.IDLE;
	}	
	/**
	 * 
	 * @return
	 */
	public String getCallId()
	{
		// TODO Auto-generated method stub
		return callId;
	}

	/**
	 * 
	 * @return
	 */
	public CallActivityImpl getCallActivity()
	{
		return callActivity;
	}
	/**
	 * 
	 * @return
	 */
	public Dialog getDialog()
	{
		return dialog;
	}

	/**
	 * 
	 * @return
	 */
	public ServerTransaction getInitialServerTransaction()
	{
		return initialServerTransaction;
	}
	/**
	 * 
	 */
	public ClientTransaction getInitialClientTransaction()
	{
		return initialClientTransaction;
	}

	/**
	 * Call Leg type management
	 *   
	 * @return
	 */
	public CallLegType getType()
	{
		return callLegType;
	}
	public boolean isEgressCL()
	{
		return callLegType.equals(CallLegType.EGRESS);
	}
	
	/**
	 *  TODO: check this 
	 *  1-do we need it? 
	 *  2-if we do, how long is it possible to find the request within the transactions
	 *  
	 * @return
	 */
	public Request getInitialRequest()
	{
		//Are these still valid?
		return callLegType.equals(CallLegType.INGRESS)? initialServerTransaction.getRequest() : initialClientTransaction.getRequest(); 
	}
	
	/**
	 * 
	 * @return
	 */
	public String toString()
	{
		return "CL:"+callId+" / ICT: "+initialClientTransaction+
		" / CT: "+initialClientTransaction+
		" / IST: "+initialServerTransaction+
		" / ST: "+initialServerTransaction;
	}
	
	/**
	 * 
	 * 
	 */
	public void cleanUp()
	{		
		// This should have been done in processTransactionTerminated. 
		// This is in case we want to keep the initial txs for the whole call time
		if (this.initialClientTransaction != null)
		{
			try
			{
				this.initialClientTransaction.terminate();
				if(logger.isDebugEnabled())logger.debug("Terminating initial CT: "+initialClientTransaction);
			}
			catch (Exception ex) {
				logger.error("LegId=" + callId + " - Exception deleting clientTransaction=" + ex, ex);
			}
		}				
		if (this.initialServerTransaction != null)
		{
			try
			{
				if(logger.isDebugEnabled())logger.debug("Terminating initial ST: "+initialServerTransaction);
				this.initialServerTransaction.terminate();
			}
			catch (Exception ex) {
				logger.error("LegId=" + callId + " - Exception deleting serverTransaction=" + ex, ex);
			}
		}		
		//TODO: clean pending transactions if any
		
		
		this.initialClientTransaction = null;
		this.initialServerTransaction = null;
	
	}
	/**
	 * 
	 * @return
	 */
	public CallLegState getState()
	{
		return state;
	}
	/**
	 * 
	 * @param state
	 */
	public void setState(CallLegState state)
	{
		this.state = state;
	}

	public CallHistoric.CallProcessEvent setLastEvent(SIPRequest request)
	{
		return setLastEvent(request, false);
	}
	public CallHistoric.CallProcessEvent setLastEvent(SIPRequest request, boolean initial)
	{
		lastEventTS = System.currentTimeMillis();
		Long[] tsArray = null;
		if (request!=null)
		{
			lastEventType = request.getMethod();
			CSeqHeader cSeqHeader = request.getCSeq();
			if(cSeqHeader != null) 
			{
				lastEventCSeq = cSeqHeader.toString(); 
			}
			else
			{
				lastEventCSeq = "undefined";
			}
			//			TS0_fake_header tsHeader = (TS0_fake_header)type.getHeader(TS0_fake_header.NAME);			
			//			if(tsHeader != null)
			//			{
			//				try
			//				{
			//					entryTS = Long.parseLong(tsHeader.getValue());
			//				}
			//				catch(Throwable e)
			//				{
			//					entryTS = -2;
			//				}
			//			}
			Object appData = request.getApplicationData();
			if (appData != null)
			{
				tsArray = (Long[])appData;
			}
			else
			{
				if(logger.isDebugEnabled())logger.debug("No appData found. Cannot record TS statistics");
			}
		}
		else
		{
			lastEventType = "undefined request";
			lastEventCSeq = "undefined";
		}	
		CallHistoric.CallProcessEvent callProcessEvent = new CallHistoric.CallProcessEvent(tsArray,  callLegType, callId, lastEventType, initial); 
		callProcessEvent.setTS2(lastEventTS);
		callActivity.getHistoric().addEvent(callProcessEvent);
		return callProcessEvent;
	}
	public CallHistoric.CallProcessEvent setLastEvent(SIPResponse response)
	{
		lastEventTS = System.currentTimeMillis();
		Long[] tsArray = null;
		if (response!=null)
		{
			lastEventType = Integer.toString(response.getStatusCode());
			CSeqHeader cSeqHeader = response.getCSeq();
			if(cSeqHeader != null) 
			{
				lastEventCSeq = cSeqHeader.toString(); 
			}
			else
			{
				lastEventCSeq = "undefined";
			}
			//			TS0_fake_header tsHeader = (TS0_fake_header)type.getHeader(TS0_fake_header.NAME);
			//			if(tsHeader != null)
			//			{
			//				try
			//				{
			//					entryTS = Long.parseLong(tsHeader.getValue());
			//				}
			//				catch(Throwable e)
			//				{
			//					entryTS = -2;
			//				}
			//			}	
			Object appData = response.getApplicationData();
			if (appData != null)
			{
				tsArray = (Long[])appData;
			}			
		}
		else
		{
			lastEventType = "undefined response";
			lastEventCSeq = "undefined";
		}
		CallHistoric.CallProcessEvent callProcessEvent = new CallHistoric.CallProcessEvent(tsArray, callLegType, callId, lastEventType, false); 
		callProcessEvent.setTS2(lastEventTS);
		callActivity.getHistoric().addEvent(callProcessEvent);
		return callProcessEvent;		
	}
	
	public AtomicLong pendingAckCSeq = new AtomicLong(-2);
	public long getPendingAckCSeq()
	{
		return pendingAckCSeq.get();
	}
	public long setPendingAckCSeq(long pending)
	{
		long oldPending = pendingAckCSeq.getAndSet(pending); 
		if(logger.isDebugEnabled())logger.debug(callLegType+ "  setting pending ACK cSeq "+oldPending+" / "+pending);
		return oldPending;
	}
	public String getLastEvent()
	{
		StringBuilder sb = new StringBuilder(100);
		sb.append(callLegType).append(" : ").append(lastEventType).append(" : ")
		.append(lastEventCSeq).append(" : ")		
		.append(System.currentTimeMillis()-lastEventTS).append(" ms ago : ").append(callId);
		return sb.toString();
	}
}
