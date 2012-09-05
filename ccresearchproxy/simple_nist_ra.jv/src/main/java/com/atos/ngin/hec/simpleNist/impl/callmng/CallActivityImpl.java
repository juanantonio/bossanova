/*
com.atos.ngin.hec.simpleNist.impl.CallActivityImpl.java

Copyright (c) 2009 AtosOrigin
Albarracin 25, 28037 Madrid
All Rights Reserved.

This software is confidential and proprietary information of AtosOrigin. 
You shall not disclose such confidential information and shall use it only 
in accordance with the terms of the license agreement established with 
AtosOrigin.
*/
package com.atos.ngin.hec.simpleNist.impl.callmng;



import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

import com.atos.ngin.hec.simpleNist.impl.SimpleNISTResourceAdaptor;
import com.atos.ngin.hec.simpleNist.impl.SimpleNistProcessor;
import com.atos.ngin.hec.simpleNist.type.CallActivity;
import com.atos.ngin.hec.simpleNist.type.CallLeg;
import com.atos.ngin.hec.simpleNist.type.CallActivity.CallState;
import com.atos.ngin.hec.simpleNist.type.CallLeg.CallLegType;
import com.atos.ngin.hec.simpleNist.type.GenericCDR;

/**
* Client activity implementation.
* 
* @author JACAR
* @since  3/May/2012
*/ 
public class CallActivityImpl implements CallActivity
{
	//ATTRIBUTES ---------------------------------------------------------------
	protected CallActivityHandle  handle;
	private SimpleNISTResourceAdaptor   ra;
	private SimpleNistProcessor   callProcessor;
	private CallLegImpl ingressCallLeg;
	private CallLegImpl egressCallLeg;
//	private CallManagerInterface callManager;
	
	private CallHistoric historic;
	private GenericCDR cdrData;
	private CallLegType cdrCallLegType;

	private long initTime = 0;
	
	private CallState state = CallState.IDLE;
	
	private static int DEFAULT_RELEASE_CODE = 503;
	
	private static Logger logger = Logger.getLogger("simpleNIST.CallActivityImpl");
	//METHODS ------------------------------------------------------------------
	
	/**
	 * ClientActivityImpl constructor. Activity identifier is specified
	 * as parameter
	 * 
	 * @param ar_id Activity identifier as an integer
	 */
	public CallActivityImpl(String callId, CallLegImpl incomingCallLeg, SimpleNISTResourceAdaptor ar_ra, SimpleNistProcessor callProcessor)
	{		  
	  this.ra = ar_ra;
	  this.callProcessor = callProcessor;
	  this.ingressCallLeg = incomingCallLeg;
	  this.handle = new CallActivityHandle(callId, ra.getRaContext().getEntityName());
	  
	  historic = new CallHistoric();
	  initTime = System.currentTimeMillis();
	  setState(CallState.IDLE);
	}	
	
	/**
	 * Method to retrieve the connectionId member value
	 * 
	 * @return ConnectionId {@link ClientConnectionID}
	 */
	public CallActivityHandle getCallActivityHandle() 
	{
	  return handle;
	}  
	
	/**
	 *  This method name might as well be changed at any time...
	 */
	public boolean sendCDREvent (CallLegType callLegType, GenericCDR cdr)
	{
		if(cdr!=null)cdr.serialNumber=callProcessor.incrementAndGetSerialNumber();
		this.cdrData = cdr;
		this.cdrCallLegType = callLegType;
		return true;
	}	
	public void dispatchCDR()
	{
		long endTime = System.currentTimeMillis();
		if(cdrData != null)
		{
			cdrData.callDuration = Long.toString(endTime - initTime);
			cdrData.startDate = Long.toString(initTime);
			cdrData.endDate = Long.toString(endTime);
			//		cdrData.setHistoric(historic);
			ra.writeCDR(cdrData);
		}
		if(historic != null)
		{
			ra.writeHistoric(historic);
		}
	}
	
	// SERVICE INTERFACE
	// This is the expected Service Action
	public CallLegImpl dispatchEgressRequest(Request egressRequest)
	{
		if(getState() != CallState.PENDING_SA)
		{
			logger.error("dispatchEgressRequest: Illegal state: "+getState());
			return null;
		}
		CallLegImpl callLeg = callProcessor.dispatchEgressCallLeg(this, egressRequest);
		if (callLeg != null)
		{
			setState(CallState.CONNECTING);	
		}
		else
		{
			rejectIncomingCall();
		}
		return callLeg;
		
	}	
	// Return CallLeg or the initial Request
	public Request createEgressRequest()
	{		
		if(getState() != CallState.PENDING_SA)
		{
			logger.error("createEgressRequest: Illegal state: "+getState());
			return null;
		}
		return callProcessor.createEgressRequest(this);
	}

	public void rejectIncomingCall()
	{
		if(getState() != CallState.PENDING_SA)
		{
			logger.error("rejectIncomingCall: Illegal state: "+getState());
		}		
		callProcessor.rejectIncomingCall(DEFAULT_RELEASE_CODE, ingressCallLeg);
		setState(CallState.REJECTED);
		terminate();
	}
		
	/**
	 *  TODO: 
	 */
	public void releaseCall(String releaseCode)
	{		
		int code = DEFAULT_RELEASE_CODE;
		try
		{
			code = Integer.parseInt(releaseCode);
		}
		catch(NumberFormatException e)
		{
			//TODO say sth
		}
		//TODO: not always. This primitive might be used to 'release' an established call
		if(getState() == CallState.PENDING_SA)
		{
			callProcessor.rejectIncomingCall(code, ingressCallLeg);
			setState(CallState.REJECTED);
			terminate();
		}
		else
		{
			logger.error("Implement call releasing!!");
		}
		
	}

	public void terminate()
	{
		dispatchCDR();
		ra.getEndpoint().endActivity(getCallActivityHandle());
		CallLegImpl cltoremove = callProcessor.removeCallLeg(getIncomingCallId());
		if (logger.isDebugEnabled())
			logger.debug("IncomingCallLeg removed: " + cltoremove + " Id: " + getIncomingCallId());
		if(getEgressCallLeg() != null)
		{
			cltoremove = callProcessor.removeCallLeg(getEgressCallLeg().getCallId());
			if (logger.isDebugEnabled())
			logger.debug("OutgoingCallLeg removed: " + cltoremove + " Id " + getEgressCallLeg().getCallId());
		}
		CallActivityImpl catoremove = callProcessor.removeCall(getCallActivityHandle().toString());
		if (logger.isDebugEnabled())
			logger.debug("CallActivity released: " + catoremove);
		// callActivity.getOutgoingCallLeg().cleanUp();
		// callActivity.getIncomingCallLeg().cleanUp();
		cleanUp();

	}
	/**
	 * Check every resource is freed
	 * 
	 */
	public void cleanUp()
	{
		if (ingressCallLeg != null) ingressCallLeg.cleanUp();
		if (egressCallLeg != null) egressCallLeg.cleanUp();
		ingressCallLeg=null;
		egressCallLeg=null;
		callProcessor=null;
		historic=null;
		handle=null;
		cdrData=null;
	}
	public String toString()
	{
		StringBuilder sb = new StringBuilder("CallActivity: ");
		sb.append(state)
		.append("//")
		.append(ingressCallLeg)
		.append("//")
		.append(egressCallLeg)
		;
		return sb.toString();
	}

	
	/////////////
	// Getters //
	/////////////
	public CallLeg getOutgoingCallLeg()
	{

		return egressCallLeg;
	}
	// This could be something like 'add' when we generalize about callLegs
	public void setEgressCallLeg(CallLegImpl egressCallLeg)
	{
		this.egressCallLeg = egressCallLeg;
	}
	public CallLegImpl getEgressCallLeg()
	{
		return egressCallLeg;
	}
	public String getIncomingCallId()
	{

		return handle.getCallId();
	}

	public CallLeg getIncomingCallLeg()
	{

		return ingressCallLeg;
	}
	public CallLegImpl getIngressCallLeg()
	{
		return ingressCallLeg;
	}

	public AddressFactory getAddressFactory()
	{

		return callProcessor.getAddressFactory();
	}

	public HeaderFactory getHeaderFactory()
	{

		return callProcessor.getHeaderFactory();
	}
	public MessageFactory getMessageFactory()
	{

		return callProcessor.getMessageFactory();
	}
	public CallHistoric getHistoric()
	{
		return historic;
	}

	public CallState getState()
	{
		return state;
	}

	public void setState(CallState state)
	{
		this.state = state;
	}

}
