package com.atos.ngin.hec.simpleNist.impl;

import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.slee.resource.ActivityHandle;

import com.atos.ngin.hec.simpleNist.impl.callmng.CallActivityImpl;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallLegImpl;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallProcessorStats;
import com.atos.ngin.hec.simpleNist.type.CallActivity;

public interface SimpleNistProcessor
{
	public SipStack init(String configFilePath, SimpleNISTResourceAdaptor ra);
	public void shutdown() throws Exception;
	
	public CallProcessorStats getCallProcessorStats();
	//	public void registerOutgoingCallLeg(String outgoingCallId, CallLegImpl outgoingCallLeg);
	public CallLegImpl dispatchEgressCallLeg(CallActivityImpl callActivity, Request egressRequest);
	public Request createEgressRequest(CallActivityImpl callActivity);
	public void rejectIncomingCall(int code, CallLegImpl ingressCallLeg);
	
	public SipStack getSipStack();
	public SipProvider getIngressSipProvider();	
	public SipProvider getEgressSipProvider();	
	public AddressFactory getAddressFactory();
	public HeaderFactory getHeaderFactory();
	public MessageFactory getMessageFactory();
	public ContactHeader getStackContactHeader();
	
	public String getStackAddress(); 
	public int getStackPort();
	public String getStackTransport();
	
	public int getCPEOccupancy();
	public int getSAOccupancy();
	public int getCDRQueueOccupancy();
	public String terminatePendingCalls();
	
	public long incrementAndGetSerialNumber();
	public long getSerialNumber();

	public CallActivity findCallActivity (ActivityHandle handle);
	public CallLegImpl removeCallLeg(String callLegKey);
	public CallActivityImpl removeCall(String callKey);	
}
