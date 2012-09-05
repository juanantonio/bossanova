package com.atos.ngin.hec.simpleNist.type;

import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.slee.resource.ActivityHandle;

import com.atos.ngin.hec.simpleNist.type.CallLeg.CallLegType;


public interface CallActivity
{
	public enum CallState{IDLE, PENDING_SA, CONNECTING, CONNECTED, REJECTED, CANCELLING, CANCELLED, ENDING,  ENDED};
	
	public String getIncomingCallId();
	public CallLeg getIncomingCallLeg();
	
	public Request createEgressRequest();
//	public CallLeg getOutgoingCallLeg();
	public CallLeg dispatchEgressRequest(Request egressRequest);
	
	public void releaseCall(String releaseCode);
	
	public AddressFactory getAddressFactory();
	public HeaderFactory getHeaderFactory();
	public MessageFactory getMessageFactory();	
	public boolean sendCDREvent (CallLegType callLegType, GenericCDR cdr);
	
	public ActivityHandle getCallActivityHandle();
}
