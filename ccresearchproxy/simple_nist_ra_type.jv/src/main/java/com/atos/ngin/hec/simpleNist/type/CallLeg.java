package com.atos.ngin.hec.simpleNist.type;

import javax.sip.ClientTransaction;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;

public interface CallLeg
{
	public enum CallLegType{INGRESS,EGRESS};
	public enum CallLegState{IDLE, ESTABLISHING, ESTABLISHED, ESTABLISHED_PENDING_TRANSACTIONS, REJECTED, CANCELLING, CANCELLED, ENDING,  ENDED}
	public CallLegType getType();
	public CallActivity getCallActivity();
	public Request getInitialRequest();
//	public Request getInitialRequest();
	public String getCallId();
	public ServerTransaction getInitialServerTransaction();
//	public ServerTransaction getCurrentServerTransaction();
	public ClientTransaction getInitialClientTransaction();
	public void cleanUp();
	
}
