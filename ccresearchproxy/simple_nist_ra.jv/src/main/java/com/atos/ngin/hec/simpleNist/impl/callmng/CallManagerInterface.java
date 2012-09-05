package com.atos.ngin.hec.simpleNist.impl.callmng;

import javax.sip.Dialog;
import javax.sip.Timeout;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.atos.ngin.hec.simpleNist.type.CallLeg;

public interface CallManagerInterface
{

	public void bridgeRequest (CallLeg incomingCL, CallLeg outgoingCL, Request incomingRequest);
	
	public void bridgeResponse (CallLeg incomingCL, CallLeg outgoingCL, Response incomingResponse);
		
}
