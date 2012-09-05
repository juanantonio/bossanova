package com.atos.ngin.hec.simpleNist.impl.callmng;

public interface CallProcessorMBean
{

	// ATTRIBUTES
	public String getQueuesState();
	//	public String getCPEQueueState();
	//	public String getSAQueueState();
	public String getStackState();
	
	// OPERATIONS
	public String dumpCounters();
	public String resetCounters();
	public String terminatePendingCalls();
}
