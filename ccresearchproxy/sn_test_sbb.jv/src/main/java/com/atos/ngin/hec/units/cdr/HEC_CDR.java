package com.atos.ngin.hec.units.cdr;

import java.util.Hashtable;

import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoVO.ExtType;
import com.atos.ngin.hec.simpleNist.type.GenericCDR;
import com.atos.ngin.hec.units.hec_logic.HECLogic;

public class HEC_CDR extends GenericCDR{

	
	public static final String RT_MO = "MO";
	public static final String RT_IO = "IO";
	public static final String RT_MT = "MT";
	public static final String RT_IT = "IT";
	public static final String RT_FW = "FW.O";
	
	public static enum CallType {onnet, offnet, emergency, indetermined};
	
	String fieldsList[] = new String[]
		{
		recordType_Label,						//0
		nodeID_Label,							//1
		seqNumber_Label,						//2
		startDate_Label,						//3
		endDate_Label,							//4
		callDuration_Label,						//5
		causeForRecordClosing_Label,			//6
									
		icId_Label,								//7
		callId_Label,							//8
		originalCallId_Label,					//9
		userAgent_Label,	 					//10
		reqUri_Label,		 					//11
		calledPN_Label,							//12
		callingPN_Label,						//13
		to_Label,								//14
		from_Label,								//15
		redirectingPN_Label, 					//16
		
		mscAddress_Label,						//17
				 							
		vpnNameA_Label,							//18
		vpnGroupA_Label,						//19
		extTypeA_Label,							//20
		imsiA_Label,							//21
		msisdnA_Label,							//22
		isdnA_Label,							//23
		sipA_Label,								//24
		vpnNameB_Label,							//25
		vpnGroupB_Label,						//26
		extTypeB_Label,							//27
		imsiB_Label,							//28
		msisdnB_Label,							//29
		isdnB_Label,							//30
		sipB_Label,								//31
		vpnCallType_Label,						//32
		pabxIdA_Label,							//33
		pabxIdB_Label							//34
		};
	 
	Hashtable<String,String> cdrFieldsMap;
	public Hashtable<String, String> getCdrFieldsMap()
	{
		return cdrFieldsMap;
	}	
	/**
	 * For Testing
	 */
	public HEC_CDR()
	{
		cdrFieldsMap = new Hashtable<String,String>();
//		int i=0;
//		for (String fieldLabel:fieldsList)
//		{
//			if(!fieldLabel.startsWith("VPN") && !fieldLabel.startsWith("IN") && 
//					!fieldLabel.startsWith("SIP") && !fieldLabel.startsWith("General"))
//			{
//				cdrFieldsMap.put(fieldLabel, "valor"+(i++)+fieldLabel);
//			}
//			
//		}
	}
	
	public static final String O_GROUP_SPECIFIC_EOL = "<group name=\"Specific\">\n";
	public static final String C_GROUP_SPECIFIC_EOL = "</group>\n";
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder(1600);
		sb.append(HEADING)
		.append(O_RECORDS_EOL);
		appendRecord(sb);
		sb.append(C_RECORDS_EOL);		
		return sb.toString();
	}
	protected void appendRecord(StringBuilder sb)
	{
		sb.append(O_RECORD_EOL);
		appendGeneralGroup(sb);
		appendSpecificGroup(sb);
		sb.append(C_RECORD_EOL);

	}	
	protected void appendSpecificGroup(StringBuilder sb)
	{
		sb.append(O_GROUP_SPECIFIC_EOL);
		appendSpecificFields(sb);
		sb.append(C_GROUP_SPECIFIC_EOL);		
	}
	protected void appendSpecificFields(StringBuilder sb)
	{
		for(int i=0;i<fieldsList.length;i++)
		{
			sb.append(FIELD_INIT).append(fieldsList[i]).append(FIELD_MIDDLE).append(cdrFieldsMap.get(fieldsList[i])).append(FIELD_END_EOL);
		}
	}	
	/**
	 * 
	 * @param label
	 * @param value
	 */
	public void setField(String label, String value)
	{
		cdrFieldsMap.put(label, value);		
	}
	
	public void setOnNet()
	{
		setField(HEC_CDR.vpnCallType_Label, HEC_CDR.CallType.onnet.toString());	
	}
	
	public void setOffNet()
	{
		setField(HEC_CDR.vpnCallType_Label, HEC_CDR.CallType.offnet.toString());	
	}
	
	public void setEmergency()
	{
		setField(HEC_CDR.vpnCallType_Label, HEC_CDR.CallType.emergency.toString());	
	}
	
	public void setIndetermined()
	{
		setField(HEC_CDR.vpnCallType_Label, HEC_CDR.CallType.indetermined.toString());	
	}	
	/**
	 * 
	 * @param String isdn, String sipA
	 */
	public void setAPartyInfoForEmergency (String isdn, String sipA)
	{			
			cdrFieldsMap.put(extTypeA_Label,ExtType.pstn.toString());
			cdrFieldsMap.put(isdnA_Label,HECLogic.IAC_PLUS_SIGN+isdn);
			cdrFieldsMap.put(sipA_Label,sipA);
	}	
	/**
	 * 
	 * @param extInfoA
	 */
	public void setAPartyInfo (Hec_ExtensionInfoVO extInfoA)
	{
		if (extInfoA != null)
		{
			cdrFieldsMap.put(vpnNameA_Label,extInfoA.getVpnName());
			cdrFieldsMap.put(vpnGroupA_Label,extInfoA.getGroupName());
			cdrFieldsMap.put(extTypeA_Label,extInfoA.getExtType().toString());
			cdrFieldsMap.put(pabxIdA_Label, extInfoA.getPabxId());			
			switch(extInfoA.getExtType())
			{
				case mobile: 	cdrFieldsMap.put(msisdnA_Label,HECLogic.IAC_PLUS_SIGN+extInfoA.getMsisdn());						
							 	cdrFieldsMap.put(imsiA_Label,extInfoA.getImsi());
							 	break;
				case pstn:		cdrFieldsMap.put(isdnA_Label,HECLogic.IAC_PLUS_SIGN+extInfoA.getIsdn());
								cdrFieldsMap.put(sipA_Label,extInfoA.getSip());
								break;
				default:		//NO default
			}
			
		}
	}
	/**
	 * 
	 * @param extInfoB
	 */
	public void setBPartyInfo (Hec_ExtensionInfoVO extInfoB)
	{
		if (extInfoB != null)
		{
			cdrFieldsMap.put(vpnNameB_Label,extInfoB.getVpnName());
			cdrFieldsMap.put(vpnGroupB_Label,extInfoB.getGroupName());
			cdrFieldsMap.put(extTypeB_Label,extInfoB.getExtType().toString());
			cdrFieldsMap.put(pabxIdB_Label, extInfoB.getPabxId());
			switch(extInfoB.getExtType())
			{
				case mobile: 	cdrFieldsMap.put(msisdnB_Label,HECLogic.IAC_PLUS_SIGN+extInfoB.getMsisdn());						
							 	cdrFieldsMap.put(imsiB_Label,extInfoB.getImsi());
							 	break;
				case pstn:		cdrFieldsMap.put(isdnB_Label,HECLogic.IAC_PLUS_SIGN+extInfoB.getIsdn());
								cdrFieldsMap.put(sipB_Label,extInfoB.getSip());
								break;
				default:		//NO default
			}
			
		}
	}
	/**
	 * General fields
	 */
	private String recordType;
	public static String recordType_Label="General.RecordTypeHEC";
	
	private String nodeID;
	public static String nodeID_Label="General.NodeID";

	private String seqNumber;
	public static String seqNumber_Label="General.SequenceNumber";

	private String startDate;
	public static String startDate_Label="General.StartDate";

	private String endDate;
	public static String endDate_Label="General.EndDate";

	private String callDuration;
	public static String callDuration_Label="General.CallDuration";

	private String causeForRecordClosing;
	public static String causeForRecordClosing_Label="General.CauseForRecordClosing";
	
	/**
	 * SIP fields
	 */	
	//TODO esperar a ver que nos puede dar el MPCC
	private String icId;
	public static String icId_Label="SIP.ICID";
	
	private String callId;
	public static String callId_Label="SIP.CallID";
	
	private String originalCallId;
	public static String originalCallId_Label="SIP.Original_CallID";
	
	private String userAgent;
	public static String userAgent_Label="SIP.User-Agent";
	
	private String reqUri;
	public static String reqUri_Label="SIP.Request-URI";
	
	private String calledPN;
	public static String calledPN_Label="SIP.CalledPartyNumber";
	
	private String callingPN;
	public static String callingPN_Label="SIP.CallingPartyNumber";
	
	private String to;
	public static String to_Label="SIP.To";
	
	private String from;
	public static String from_Label="SIP.From";

	//This is the one we'll need for sure
	private String redirectingPN;
	public static String redirectingPN_Label = "SIP.RedirectingPartyNumber";
	
	/**
	 * IN fields
	 */

	private String mscAddress;
	public static String mscAddress_Label="IN.MSCAddress";

	/**
	 * VPN fields
	 */
	private String vpnNameA;
	public static String vpnNameA_Label="VPN.VPN_NAME_A";
	
	private String vpnGroupA;
	public static String vpnGroupA_Label="VPN.VPN_Group_A";
	
	private String extTypeA;
	public static String extTypeA_Label="VPN.A_ExtType";
	
	private String imsiA;
	public static String imsiA_Label="VPN.A_IMSI";
	
	private String msisdnA;
	public static String msisdnA_Label="VPN.A_MSISDN";
	
	private String isdnA;
	public static String isdnA_Label="VPN.A_ISDN";	

	private String sipA;
	public static String sipA_Label="VPN.A_SIP";
		
	private String vpnNameB;
	public static String vpnNameB_Label="VPN.VPN_NAME_B";
	
	private String vpnGroupB;
	public static String vpnGroupB_Label="VPN.VPN_Group_B";
	
	private String extTypeB;
	public static String extTypeB_Label="VPN.B_ExtType";
	
	private String imsiB;
	public static String imsiB_Label="VPN.B_IMSI";
	
	private String msisdnB;
	public static String msisdnB_Label="VPN.B_MSISDN";
	
	private String isdnB;
	public static String isdnB_Label="VPN.B_ISDN";
	
	private String sipB;
	public static String sipB_Label="VPN.B_SIP";
	
	private String vpnCallType;
	public static String vpnCallType_Label="VPN.VPN_CallType";
	
	private String pabxIdA;
	public static String pabxIdA_Label="VPN.A_PABXID";
	
	private String pabxIdB;
	public static String pabxIdB_Label="VPN.B_PABXID";
	
	/**
	 * GETTERS & SETTERS
	 * @return
	 */
//	public String getRecordType()
//	{
//		return recordType;
//	}
//	public void setRecordType(String recordType)
//	{
//		this.recordType = recordType;
//	}
//	public String getNodeID()
//	{
//		return nodeID;
//	}
//	public void setNodeID(String nodeID)
//	{
//		this.nodeID = nodeID;
//	}
//	public String getSeqNumber()
//	{
//		return seqNumber;
//	}
//	public void setSeqNumber(String seqNumber)
//	{
//		this.seqNumber = seqNumber;
//	}
//	public String getStartDate()
//	{
//		return startDate;
//	}
//	public void setStartDate(String startDate)
//	{
//		this.startDate = startDate;
//	}
//	public String getEndDate()
//	{
//		return endDate;
//	}
//	public void setEndDate(String endDate)
//	{
//		this.endDate = endDate;
//	}
//	public String getCallDuration()
//	{
//		return callDuration;
//	}
//	public void setCallDuration(String callDuration)
//	{
//		this.callDuration = callDuration;
//	}
//	public String getCauseForRecordClosing()
//	{
//		return causeForRecordClosing;
//	}
//	public void setCauseForRecordClosing(String causeForRecordClosing)
//	{
//		this.causeForRecordClosing = causeForRecordClosing;
//	}
//	public String getIcId()
//	{
//		return icId;
//	}
//	public void setIcId(String icId)
//	{
//		this.icId = icId;
//	}
//	public String getCallId()
//	{
//		return callId;
//	}
//	public void setCallId(String callId)
//	{
//		this.callId = callId;
//	}
//	public String getOriginalCallId()
//	{
//		return originalCallId;
//	}
//	public void setOriginalCallId(String originalCallId)
//	{
//		this.originalCallId = originalCallId;
//	}
//	public String getUserAgent()
//	{
//		return userAgent;
//	}
//	public void setUserAgent(String userAgent)
//	{
//		this.userAgent = userAgent;
//	}
//	public String getReqUri()
//	{
//		return reqUri;
//	}
//	public void setReqUri(String reqUri)
//	{
//		this.reqUri = reqUri;
//	}
//	public String getCalledPN()
//	{
//		return calledPN;
//	}
//	public void setCalledPN(String calledPN)
//	{
//		this.calledPN = calledPN;
//	}
//	public String getCallingPN()
//	{
//		return callingPN;
//	}
//	public void setCallingPN(String callingPN)
//	{
//		this.callingPN = callingPN;
//	}
//	public String getTo()
//	{
//		return to;
//	}
//	public void setTo(String to)
//	{
//		this.to = to;
//	}
//	public String getFrom()
//	{
//		return from;
//	}
//	public void setFrom(String from)
//	{
//		this.from = from;
//	}
//	public String getMscAddress()
//	{
//		return mscAddress;
//	}
//	public void setMscAddress(String mscAddress)
//	{
//		this.mscAddress = mscAddress;
//	}
//	public String getVpnNameA()
//	{
//		return vpnNameA;
//	}
//	public void setVpnNameA(String vpnNameA)
//	{
//		this.vpnNameA = vpnNameA;
//	}
//	public String getVpnGroupA()
//	{
//		return vpnGroupA;
//	}
//	public void setVpnGroupA(String vpnGroupA)
//	{
//		this.vpnGroupA = vpnGroupA;
//	}
//	public String getExtTypeA()
//	{
//		return extTypeA;
//	}
//	public void setExtTypeA(String extTypeA)
//	{
//		this.extTypeA = extTypeA;
//	}
//	public String getImsiA()
//	{
//		return imsiA;
//	}
//	public void setImsiA(String imsiA)
//	{
//		this.imsiA = imsiA;
//	}
//	public String getMsisdnA()
//	{
//		return msisdnA;
//	}
//	public void setMsisdnA(String msisdnA)
//	{
//		this.msisdnA = msisdnA;
//	}
//	public String getVpnNameB()
//	{
//		return vpnNameB;
//	}
//	public void setVpnNameB(String vpnNameB)
//	{
//		this.vpnNameB = vpnNameB;
//	}
//	public String getVpnGroupB()
//	{
//		return vpnGroupB;
//	}
//	public void setVpnGroupB(String vpnGroupB)
//	{
//		this.vpnGroupB = vpnGroupB;
//	}
//	public String getExtTypeB()
//	{
//		return extTypeB;
//	}
//	public void setExtTypeB(String extTypeB)
//	{
//		this.extTypeB = extTypeB;
//	}
//	public String getImsiB()
//	{
//		return imsiB;
//	}
//	public void setImsiB(String imsiB)
//	{
//		this.imsiB = imsiB;
//	}
//	public String getMsisdnB()
//	{
//		return msisdnB;
//	}
//	public void setMsisdnB(String msisdnB)
//	{
//		this.msisdnB = msisdnB;
//	}
//	public String getIsdnB()
//	{
//		return isdnB;
//	}
//	public void setIsdnB(String isdnB)
//	{
//		this.isdnB = isdnB;
//	}
//	public String getSipB()
//	{
//		return sipB;
//	}
//	public void setSipB(String sipB)
//	{
//		this.sipB = sipB;
//	}
//	public String getVpnCallType()
//	{
//		return vpnCallType;
//	}
//	public void setVpnCallType(String vpnCallType)
//	{
//		this.vpnCallType = vpnCallType;
//	}
//	public String getPabxIdA()
//	{
//		return pabxIdA;
//	}
//	public void setPabxIdA(String pabxIdA)
//	{
//		this.pabxIdA = pabxIdA;
//	}
//	public String getPabxIdB()
//	{
//		return pabxIdB;
//	}
//	public void setPabxIdB(String pabxIdB)
//	{
//		this.pabxIdB = pabxIdB;
//	}
//	public String getRedirectingPN()
//	{
//		return redirectingPN;
//	}
//	public void setRedirectingPN(String redirectingPN)
//	{
//		this.redirectingPN = redirectingPN;
//	}

	
	
}

