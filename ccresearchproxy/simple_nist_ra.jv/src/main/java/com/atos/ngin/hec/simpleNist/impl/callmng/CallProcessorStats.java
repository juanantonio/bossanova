package com.atos.ngin.hec.simpleNist.impl.callmng;

import gov.nist.javax.sip.SipStackImpl;

import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.atos.ngin.hec.simpleNist.impl.SimpleNistProcessor;

public class CallProcessorStats extends StandardMBean implements CallProcessorMBean
{

	SimpleNistProcessor callProcessor;
	MessageStats counters;
	
	public CallProcessorStats(SimpleNistProcessor callProcessor)throws NotCompliantMBeanException
	{
		super(CallProcessorMBean.class);
		this.callProcessor = callProcessor;
		this.counters = new MessageStats();
	}
	public MessageStats getCounters()
	{
		return counters;
	}
	public String getQueuesState()
	{
		// TODO Auto-generated method stub
		int saOccupancy = callProcessor.getSAOccupancy();
		int cpeOccupancy = callProcessor.getCPEOccupancy();
		int cdrOccupancy = callProcessor.getCDRQueueOccupancy();
		long cdrSN = callProcessor.getSerialNumber();
		StringBuilder sb = new StringBuilder("Queues Occupancy (SA / CPE / CDR / SN ): \n")
							.append("( ").append(saOccupancy).append(" / ")
							.append(cpeOccupancy).append(" / ")
							.append(cdrOccupancy).append(" / ")
							.append(cdrSN).append(" )\n");

		return sb.toString();
	}

	public String getCPEQueueState()
	{
		// TODO Auto-generated method stub
		int cpeOccupancy = callProcessor.getCPEOccupancy();
		StringBuilder sb = new StringBuilder("Queues Occupancy: \n")
							.append("\tCPE: ").append(cpeOccupancy).append("\n");

		return sb.toString();
	}

	public String getSAQueueState()
	{
		// TODO Auto-generated method stub
		int saOccupancy = callProcessor.getSAOccupancy();
		StringBuilder sb = new StringBuilder("Queues Occupancy: \n")
							.append("\tSA: ").append(saOccupancy).append("\n");
		return sb.toString();
	}
	private int getStackPendingTransactionTableSize()
	{
		//This is a bit tricky: we'll use the CongestionControlMessageValve instead in the future
		return ((SipStackImpl)callProcessor.getSipStack()).getPendingTransactionsSize();
	}
	private int getStackTransactionTableSize()
	{
		//This is a bit tricky: we'll use the CongestionControlMessageValve instead in the future
		return ((SipStackImpl)callProcessor.getSipStack()).getServerTransactionTableSize();
	}
	private int getStackDialogTableSize()
	{
		//This is a bit tricky: we'll use the CongestionControlMessageValve instead in the future
		return ((SipStackImpl)callProcessor.getSipStack()).getDialogsSize();
	}
	public String getStackState()
	{
		StringBuilder sb = new StringBuilder(50);
		sb.append(" Dialogs / Transactions / PendingTransactions : ")
			.append(getStackDialogTableSize()).append(" / ")
			.append(getStackTransactionTableSize()).append(" / ")
			.append(getStackPendingTransactionTableSize());
		return sb.toString();
	}
	public String dumpCounters()
	{
		return counters.toString();
	}
	
	public String resetCounters()
	{
		String countersDump = counters.toString();
		counters.reset();
		return countersDump;
	}
	public String terminatePendingCalls()
	{
		return callProcessor.terminatePendingCalls();
	}
	public static class MessageStats
	{
		public static enum MessageCounter { SERVICE_ACTION, FIRST_INVITE, INVITE_RETRANS, REINVITE, RESPONSE, ACK, ACK_SENT, ACK_RETRANS, PRACK, BYE, UPDATE, OTHER,											
											DIALOG_TERMINATED, TRANSACION_TERMINATED, 
											TIMEOUT, DIALOG_TIMEOUT, IOEXCEPTION,
											ACTIVITY_TERMINATED};   
		AtomicIntegerArray counterArray = new AtomicIntegerArray(MessageCounter.values().length);
		
		public int incrementCounter(MessageCounter counter)
		{
			return counterArray.getAndIncrement(counter.ordinal());
		}
		public void reset()
		{
			for (MessageCounter counter : MessageCounter.values())
			{
				counterArray.lazySet(counter.ordinal(), 0);
			}
		}
		public String toString()
		{
			StringBuilder sb = new StringBuilder(MessageCounter.values().length * 20);
			for (MessageCounter counter : MessageCounter.values())
			{
				sb.append(counter.name()).append("( ").append(counterArray.get(counter.ordinal())).append(" ) \n");
			}
			return sb.toString();
		}
	}
}
