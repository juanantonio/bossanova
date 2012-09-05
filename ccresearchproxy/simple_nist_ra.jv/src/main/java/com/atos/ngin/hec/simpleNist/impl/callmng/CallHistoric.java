package com.atos.ngin.hec.simpleNist.impl.callmng;


import java.util.ArrayList;

import com.atos.ngin.hec.simpleNist.type.CallLeg.CallLegType;
import com.atos.ngin.hec.simpleNist.type.Writable;

public class CallHistoric implements Writable
{

	private static String INGRESS_INDICATOR = ">>>>>>>>>> ";
	private static String EGRESS_INDICATOR = "<<<<<<<<<< ";
	private static String INITIAL_INDICATOR = "INITIAL ";
	private static int EXPECTED_EVENT_SIZE = 40;
	private static int EXPECTED_NUM_OF_EVENTS = 10;
	private static int EXPECTED_HISTORIC_SIZE=(EXPECTED_EVENT_SIZE+2)*EXPECTED_NUM_OF_EVENTS;
	
	public static class CallProcessEvent
	{
		long TSEntry, TS1, TS2, TS3;
		CallLegType callLegType;
		String callLegId;
		String reason; //method if request, code if response, timeout otherwise: just text to display
		boolean initial = false;
		public CallProcessEvent (Long[] tsArray, CallLegType callLegType, String callLegId, String reason, boolean initial)
		{
			this.TSEntry = tsArray != null && tsArray[0] != null? tsArray[0]:-1 ;
			this.TS1 = tsArray != null && tsArray[1] != null? tsArray[1]:-1;
			this.callLegType = callLegType;
			this.callLegId = callLegId;
			this.reason = reason;
			this.initial = initial;
		}
		public void setTS2(long tS2)
		{
			TS2 = tS2;
		}
		public void setTS3(long tS3)
		{
			TS3 = tS3;
		}
		public String toString()
		{
			StringBuilder sb = new StringBuilder(EXPECTED_EVENT_SIZE);
			sb.append(callLegType.equals(CallLegType.INGRESS)?INGRESS_INDICATOR:EGRESS_INDICATOR);
			if(initial)sb.append(INITIAL_INDICATOR);
			sb.append(reason)
			.append(" ( ").append(TSEntry).append(" / ");
			long delta = TS1-TSEntry;
			long criticity = Math.round(Math.log((double)delta/20));
			for(long i=0;i<criticity;i++)
			{
				sb.append('*');
			}
			//			if(delta > 10)
			//			{
			//				sb.append('*');
			//				if(delta > 100)sb.append('*');
			//			}
			sb.append(delta).append(" / ");
			delta = TS2-TS1;
			criticity = Math.round(Math.log((double)delta/20));
			for(long i=0;i<criticity;i++)
			{
				sb.append('*');
			}			
			//			if(delta > 10)
			//			{
			//				sb.append('*');
			//				if(delta > 100)sb.append('*');
			//			}
			sb.append(delta).append(" / ");
			delta = TS3-TS2;
			criticity = Math.round(Math.log((double)delta/20));
			for(long i=0;i<criticity;i++)
			{
				sb.append('*');
			}
			//			if(delta > 10)
			//			{
			//				sb.append('*');
			//				if(delta > 100)sb.append('*');
			//			}
			sb.append(delta).append(" ) ")
			.append(callLegId);
			return sb.toString();
		}
		
	}
	
	private ArrayList<CallProcessEvent> callHistoric = new ArrayList<CallProcessEvent>(EXPECTED_NUM_OF_EVENTS);
	
	public void addEvent(CallProcessEvent event)
	{
		callHistoric.add(event);		
	}
	public String toString()
	{
		StringBuilder sb = new StringBuilder(EXPECTED_HISTORIC_SIZE);
		for (CallProcessEvent event:callHistoric)
		{
			sb.append(event).append("\n");
		}
		return sb.toString();
	}
}
