package com.atos.ngin.hec.units.hec_logic;

import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelURLImpl;
import gov.nist.javax.sip.header.ExtensionHeaderImpl;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssertedIdentity;
import gov.nist.javax.sip.header.ims.PAssertedIdentityHeader;
import gov.nist.javax.sip.header.ims.Privacy;
import gov.nist.javax.sip.header.ims.PrivacyHeader;
import gov.nist.javax.sip.parser.StringMsgParser;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.header.ExtensionHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

import com.atos.ivpn.dummy.ratype.CorrelationNumberContainer;
import com.atos.ivpn.dummy.ratype.DummyRAActivity;
import com.atos.ivpn.dummy.ratype.DummyRAActivityProvider;
import com.atos.ngin.db.nginpool.exception.DBException;
import com.atos.ngin.db.nginpool.exception.InvalidParamException;
import com.atos.ngin.db.nginpool.exception.NoDataFoundException;
import com.atos.ngin.db.nginpool.exception.TTException;
import com.atos.ngin.hec.hec_db.catalog.country.CountryManager;
import com.atos.ngin.hec.hec_db.catalog.country.CountryVO;
import com.atos.ngin.hec.hec_db.catalog.releasecause.ReleaseCauseHelper;
import com.atos.ngin.hec.hec_db.catalog.releasecause.ReleaseCauseVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoDAO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoVO.ExtType;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_GroupDAO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_GroupVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_IndextDAO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_IndextVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_McidDAO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_McidVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_OpcoDAO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_OpcoVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_SpecialNumsDAO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_SpecialNumsVO;
import com.atos.ngin.hec.units.sbb.common.Constants;
import com.atos.ngin.hec.units.sbb.helpers.ConfigurationHelper;
import com.atos.ngin.hec.units.sbb.helpers.StatisticsHelper;
import com.atos.ngin.hec.writer_ra.writer_ra_type.WriterProvider;


public class HECLogic 
{
	
	final static Logger log = Logger.getLogger("HEC.logic");
	
	public static final String IAC_PLUS_SIGN = "+";
	public static final String AT_SIGN = "@";
	public static final String OPEN_BRACKET = "<";
	public static final String CLOSE_BRACKET = ">";
	
	public static final String SIP_URI_PREFIX = "sip:";
	
	public static final String HDR_ROUTE = "Route";
	public static final String HDR_PSU = "P-Served-User";
	public static final String HDR_FROM = FromHeader.NAME;
	public static final String HDR_PAID = PAssertedIdentity.NAME;//"P-Asserted-Identity";
	public static final String HDR_PRIVACY = PrivacyHeader.NAME;
	
	public static final String HEC_CORPORTATE_DOMAIN_PARAMETER = "HEC_CORPORATE_DOMAIN";	
	
	private static Pattern patternUCMTrunkGroup;
	private static Pattern patternUCMTrunkCtx;
	private static Pattern patternSIPNameAddress;
	private static Pattern patternSIPNameAddressAdvanced;
	static
	{
		//TODO check valid characters for 'trgrp' and 'trunk-context'
		//		patternUCMTrunkCtx = Pattern.compile("(;trunk-context=([\\w]*))");
		patternUCMTrunkCtx = Pattern.compile("(;trunk-context=([^;^@^:]*))");
		//		patternUCMTrunkGroup = Pattern.compile("(;tgrp=([\\w]*))");
		patternUCMTrunkGroup = Pattern.compile("(;tgrp=([^;^@^:]*))");
		patternSIPNameAddress = Pattern.compile("(<(.*)>)");		
		patternSIPNameAddressAdvanced = Pattern.compile("^(?:(\"[^\"]*\"))?[\\s]*<((?:sip|tel|sips):(?:([^@:;]*)(?:;([^@:]*))?(?::([^@:]*))?@)?([^@<>]*))>");		
	}
	
//	public enum CallType
//	{
//		ONNET,
//		OFFNET
//	}
	
	public enum CSScenario
	{
		//Originating
		MO_NO_RR,
		//Terminating 
		MT_NO_RR,
		UCM_OUT_ONNET_NO_INTERCONN,
		//Error scenario
		UNSUPPORTED_NOAI,
		ERROR_NO_SCENARIO
	}
	
	public static class CSTriggerValidation
	{
		public CSScenario scenario;
		public String payload;
		public Hec_OpcoVO opCo;
		public CSTriggerValidation(CSScenario sc, Hec_OpcoVO oC, String payL)
		{
			scenario = sc;
			payload = payL;
			opCo = oC;
		}
		public CSTriggerValidation(CSScenario sc, Hec_OpcoVO oC)
		{
			scenario = sc;		
			opCo = oC;
		}		
	}
	public enum IMSTrigger {
		hec_mo, 
		hec_mt, 
		hec_ucm_incoming, 
		hec_ucm_outgoing
	}	
	public enum IMSScenario
	{
		MO_NO_RR,
		MO_RR,
		MT_NO_RR,
		MT_RR,
		UCM_IN_PSTN,
		UCM_OUT_ONNET,
//		UCM_OUT_ONNET_NO_INTERCONN, This cannot be detected in trigger validation time
		UCM_OUT_OFFNET,
		UCM_OUT_OFFNET_EMERGENCY
	}
	
	/**
	 * 
	 * @author A128166
	 *
	 */
	public static class IMSTriggerValidation
	{
		public IMSScenario scenario;
//		public String addressPayload;
		public IMSBNumber addressPayload;
		public String addressDomain;
		public Hec_OpcoVO opCoVO;
		public IMSTriggerValidation(IMSScenario sc, Hec_OpcoVO oCVO, IMSBNumber payload, String domain)
		{
			scenario = sc;
			addressPayload = payload;
			addressDomain = domain;
			opCoVO = oCVO;
		}
//		public IMSTriggerValidation(IMSScenario sc, Integer oCId)
//		{
//			scenario = sc;		
//			opCoId = oCId;
//		}		
	}

	/**
	 * 
	 * @author A128166
	 *
	 */
	public static class RoamingInfo
	{
		public CountryVO homeCountry;
		public CountryVO mscCountry;
		public boolean isInRoaming;
		public RoamingInfo (boolean iIR, CountryVO hCC, CountryVO vCC)
		{
			isInRoaming = iIR;
			homeCountry = hCC;
			mscCountry = vCC;
		}
		public String toString()
		{
			StringBuilder sb = new StringBuilder("Roaming:").append(isInRoaming).
			append("  V[").append(mscCountry).append("] H [").append(homeCountry).append("]");
			return sb.toString();
		}
	}
	
	/**
	 * This one is like CSTriggerValidation for MT scenarios
	 * 
	 * @author A128166
	 *
	 */
	public static class CSMONormalizedANumber
	{
		public Hec_OpcoVO opco;
		public String normalizedANumber;
		public CSMONormalizedANumber(Hec_OpcoVO opco, String normalizedA)
		{
			this.opco = opco;
			this.normalizedANumber = normalizedA;
		}
	}
	
	public enum BNumberType {SpecialDestination, PNPShortNumber, PublicNumberNAT, PublicNumberINT, Correlation, NonDecimalNumber, EmergencyNumber};	
	/**
	 * 
	 * @author A128166
	 *
	 */
	public static class CSNormalizedBNumber
	{
		//What about aligning these with those below

		public BNumberType bNumberType;
		public String normalizedBNumber;
		public String originalBNumber;
		public Hec_SpecialNumsVO specialDestinationVO;
		public CSNormalizedBNumber(BNumberType type, String normNum, String origNum)
		{
			bNumberType = type;
			normalizedBNumber = normNum;
			originalBNumber = origNum;
		}
		//For Special Destinations
		public CSNormalizedBNumber(String origNum, Hec_SpecialNumsVO sDVO)
		{
			bNumberType = BNumberType.SpecialDestination;
			normalizedBNumber = sDVO.getSpecShortNumber(); //incoming number 
			originalBNumber = origNum;
			specialDestinationVO = sDVO;
		}		
		public String toString()
		{
			switch (bNumberType)
			{
			case SpecialDestination:
				return new StringBuilder("[BNumType=").append(bNumberType).
				append(", Normalized=").append(normalizedBNumber).
				append(", Original=").append(originalBNumber).
				append(", Routing Number=").append(specialDestinationVO.getSpecLongNumber()).
				append("]").toString();			
				
			case PNPShortNumber:
			case PublicNumberNAT:
			case PublicNumberINT:
			default:
				return new StringBuilder("[BNumType=").append(bNumberType).
				append(", Normalized=").append(normalizedBNumber).
				append(", Original=").append(originalBNumber).
				append("]").toString();
			}
		}
	}	
	

	public static class URIUserParam
	{
		public String paramName;
		public String paramValue;
		public URIUserParam(String nameValue)
		{
			int index = nameValue.indexOf('=');
			if(index > 0)
			{
//				String[] tokens = nameValue.split("="); slow...
//				paramName=tokens[0];
//				paramValue=tokens[1];
				paramName=nameValue.substring(0,index);
				paramValue=nameValue.substring(index+1);
				//no more validations by the moment
			}
			else
			{
				paramName=nameValue;
				paramValue=null; //or "", we'll see
			}
		}
		public URIUserParam(String name, String value)
		{
			paramName=name;
			paramValue=value;
		}
		public String toString()
		{
			return paramValue!=null? new StringBuilder(paramName).append('=').append(paramValue).toString()
					:paramName;
		}
	}
	public static class URIUserPart
	{
		public String user;
		public URIUserParam[] params;
		public String paramsString;		
		public URIUserPart (String userPart)
		{
			int index = userPart.indexOf(";");
			if(index != -1)
			{
				String[] tokens = userPart.split(";");
				user=tokens[0];
				params = new URIUserParam[tokens.length-1];
				for (int i=1;i<tokens.length;i++)
				{
					params[i-1]=new URIUserParam(tokens[i]);
				}
				paramsString=userPart.substring(index+1);
			}
			else
			{
				user = userPart;
				params = null;
				paramsString = null;
			}
		}
		public String toString()
		{
			return paramsString!=null? new StringBuilder(user).append(';').append(paramsString).toString()
					:user;
		}
	}
	public static class IMSBNumber
	{
		//What about aligning these with those above
		//		public enum BNumberType {SpecialDestination, Correlation, NumBInt, CSCdPN};

		public BNumberType bNumberType;
		public String addressPayloadStr;
		public URIUserPart originalBNumber;
				
		public IMSBNumber(BNumberType type, String payload, String origNum)
		{
			bNumberType = type;
			addressPayloadStr = payload;
			originalBNumber = new URIUserPart(origNum);
		}
		public IMSBNumber(BNumberType type, String payload, URIUserPart origNum)
		{
			bNumberType = type;
			addressPayloadStr = payload;
			originalBNumber = origNum;
		}		
		public String toString()
		{
			return new StringBuilder("[BNumType=").append(bNumberType).
			append(", Normalized=").append(addressPayloadStr).
			append(", Original=").append(originalBNumber).
			append("]").toString();
		}		
		
	}

	public static class NameAddress
	{
		public String displayName;
		public GenericURI addrSpec;
		
		public NameAddress(String displayName, GenericURI addrSpec)
		{
			this.displayName = displayName;
			this.addrSpec = addrSpec;
		}
		
//		public static NameAddress getNameAddress (String headerParam)
//		{
//			
//		}
		
		public String toString()
		{
			return new StringBuilder(displayName!=null?displayName:"").append('<').append(addrSpec).append('>').toString();	
		}
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////		CS LOGIC								//////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param numberIntFormat    Must include country code
	 * @return Number with no CC
	 * 
	 */
	public static String stripCountryCode (String numberIntFormat)
	{
		CountryVO country = null;
		try
		{
			country = CountryManager.findByNumber(numberIntFormat);
			short cc = country.getCountryCode();
			//			short numDigits = cc/100 > 0?(short)3:
			//										(cc/10>0?(short)2
			//												:(short)1);
			short numDigits = (short)Integer.toString(cc).length();
			
			
			return numberIntFormat.substring(numDigits, numberIntFormat.length());
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		} catch (TTException e)
		{
			log.warn("TTException Exception "+e);
			e.printStackTrace();
		} catch (DBException e)
		{
			log.warn("DBException Exception "+e);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			log.warn("General Exception "+e);
			e.printStackTrace();
		}
		return null;
		
	}

	/**
	 * 
	 * @param callingPN  formatted as CC<Nat#>
	 * @param mscAddress as received: CC<Nat#>
	 * @return
	 */
	public static RoamingInfo getRoamingInfo (String callingPN, String mscAddress)
	{
		CountryVO vCountry = null;
		CountryVO hCountry = null;
		try
		{
			if(log.isDebugEnabled())log.debug("Searching VPLMN country by mscaddress "+mscAddress);			
			vCountry = CountryManager.findByNumber(mscAddress);

		} catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		} catch (TTException e)
		{
			log.warn("TTException Exception "+e);
			e.printStackTrace();
		} catch (DBException e)
		{
			log.warn("DBException Exception "+e);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			log.warn("General Exception "+e);
			e.printStackTrace();
		}
		try
		{
			if(log.isDebugEnabled())log.debug("Searching home country by callingPN "+callingPN);
			hCountry = CountryManager.findByNumber(callingPN);	

		} catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		} catch (TTException e)
		{
			log.warn("TTException Exception "+e);
			e.printStackTrace();
		} catch (DBException e)
		{
			log.warn("DBException Exception "+e);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			log.warn("General Exception "+e);
			e.printStackTrace();
		}
			
		return new RoamingInfo((vCountry == null || hCountry == null || vCountry.getCountryCode() != hCountry.getCountryCode()), 
				hCountry, 
				vCountry);
	}

	/**
	 * ******** Currently Disabled Feature ************
	 * MO scenario
	 * 	
	 * @param mscAddress E.164 international format
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
//	public static boolean validateNetwork (String mscAddress,
//				ConfigurationHelper confHelper, StatisticsHelper statsHelper)
//	{
//		if(log.isDebugEnabled())log.debug("Validating originating network for IDP with MSC "+mscAddress);
//		NetPrefixHelper networkHelper = new NetPrefixHelper();
//		try
//		{	
//			//Best matching in net prefixes table. 
//			boolean validated = networkHelper.checkIfExistsByPK(mscAddress);
//			//Just for testing
//			return true;//validated;
////			if(opcoVO == null)log.warn("No rerouting prefix could be found for IDP with MSC "+mscAddress);	
////			return opcoVO;					
//		}
//		catch (NoDataFoundException e)
//		{
//			log.warn("NoDataFoundException Exception "+e);
//			e.printStackTrace();
//		} catch (InvalidParamException e)
//		{
//			log.warn("InvalidParamException Exception "+e);
//			e.printStackTrace();
//		} catch (DBException e)
//		{
//			log.warn("DBException Exception "+e);
//			e.printStackTrace();
//		}
//		//Just for testing
//		return true;
////		return false;				
//	}


	/**
	 * MO scenario	
	 * MSC address must be in international format	
	 * Invoked by CS 'processMO' and 'processMT_NO_RR' methods

	 * @param subscriberAddress E.164 international formated address
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static Hec_OpcoVO getSubscriberOpco (String subscriberAddress,
				ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(log.isDebugEnabled())log.debug("Obtaining OpCo for subscriber "+subscriberAddress);
		try
		{	
			//Best matching in opcos table. 
			Hec_OpcoVO opco = Hec_OpcoDAO.getOpcoByInternationalAddress(subscriberAddress); 
			//Just for testing
			return opco;//validated;
//			if(opcoVO == null)log.warn("No rerouting prefix could be found for IDP with MSC "+mscAddress);	
//			return opcoVO;					
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}
		return null;				
	}

	/**
	 * 
	 * @param subscriberAddressCandidate  MSISDN or PSTN number with CC
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
//	public static Integer getSubscriberVpn (String subscriberAddressCandidate,
//			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
//	{
//		if(log.isDebugEnabled())log.debug("Obtaining Hec Vpn for subscriber "+subscriberAddressCandidate);
//		
//		Integer vpnId = Hec_VpnDAO.getVpnIdBySubscriber(subscriberAddressCandidate);
//		
//		return vpnId;
//	}
	
	/**
	 * 
	 * @param subscriberAddressCandidate
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static Hec_ExtensionInfoVO validateHECExtension (String subscriberAddressCandidate,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(log.isDebugEnabled())log.debug("Searching Hec extension info for subscriber "+subscriberAddressCandidate);
		
		//This method uses just one JDBC connection for both queries:
		Hec_ExtensionInfoVO extInfo = Hec_ExtensionInfoDAO.getExtensionInfoBySubscriber(subscriberAddressCandidate);
		//BUG 7607
		if(extInfo != null && extInfo.getExtType() == Hec_ExtensionInfoVO.ExtType.pstn)
		{
			extInfo.setSip(new StringBuilder(HECLogic.SIP_URI_PREFIX).append(HECLogic.IAC_PLUS_SIGN).append(extInfo.getIsdn()).
					  append(HECLogic.AT_SIGN).append(confHelper.get(HEC_CORPORTATE_DOMAIN_PARAMETER)).toString());
		}			
		if(log.isDebugEnabled())log.debug("Obtained info: "+extInfo);		
		
		return extInfo;
	}
	/**
	 * 
	 * @param subscriberAddressCandidate
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static Hec_ExtensionInfoVO validateHECMobileExtension (String subscriberAddressCandidate)
//			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(log.isDebugEnabled())log.debug("Searching Hec mobile extension info for subscriber "+subscriberAddressCandidate);
		
		Hec_ExtensionInfoVO extInfo = Hec_ExtensionInfoDAO.getExtensionInfoByMobileNumber(subscriberAddressCandidate);
		if(log.isDebugEnabled())log.debug("Obtained info: "+extInfo);
		
		return extInfo;
	}
	/**
	 * 
	 * @param subscriberAddressCandidate
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static Hec_ExtensionInfoVO validateHECGeographicExtension (String subscriberAddressCandidate,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(log.isDebugEnabled())log.debug("Searching Hec geographic extension info for subscriber "+subscriberAddressCandidate);
		
		Hec_ExtensionInfoVO extInfo = Hec_ExtensionInfoDAO.getExtensionInfoByGeographicNumber(subscriberAddressCandidate);
		//BUG 7607
		if(extInfo != null && extInfo.getExtType() == Hec_ExtensionInfoVO.ExtType.pstn)
		{
			extInfo.setSip(new StringBuilder(HECLogic.SIP_URI_PREFIX).append(HECLogic.IAC_PLUS_SIGN).append(extInfo.getIsdn()).
					  append(HECLogic.AT_SIGN).append(confHelper.get(HEC_CORPORTATE_DOMAIN_PARAMETER)).toString());
		}		
		if(log.isDebugEnabled())log.debug("Obtained info: "+extInfo);		
		return extInfo;
	}	
	/** 
	 * No formatting needed. Expected A number in E.164 with NOAI=unknown, that is <CC><Nat num>
	 * 
	 * @param cgPN CallingPartyNumber from IDP
	 * @return
	 */
	public static /*Hec_GroupVO*/Hec_ExtensionInfoVO validateCSMO_A_MSISDN (String cgPN, RoamingInfo roamingInfo)
	{
		if(log.isDebugEnabled())log.debug("Validating A number for MO IDP with CgPN "+cgPN);		
		try
		{	
			// 3.1.4: A party validation:
			// [..] in case of CS originating scenarios, [..] remove the International Access Code 
			// [..] E.164 international format with noai=unknown

			// 1 - Strip IAC from Roaming country (if needed):
			String normalizedCgPN = cgPN;
			for (String iac : roamingInfo.mscCountry.getInternationalCodes())
			{
				if(log.isDebugEnabled())log.debug("Checking IAC: "+iac);
				if(iac != null && iac.trim().length() != 0 && cgPN.startsWith(iac))
				{
					//stripIAC(bNum)		
					if(log.isDebugEnabled())log.debug("IAC detected: "+iac);
					normalizedCgPN = cgPN.substring(iac.length());
					break;
				}				
			}
											
			// 2 - Search the DB						
//			Hec_GroupVO groupVO = HECLogic.validateHEC_MSISDN(normalizedCgPN);		
//			if (groupVO == null) log.warn("Could not find a groupId for Msisdn: "+normalizedCgPN);
			Hec_ExtensionInfoVO extInfo = HECLogic.validateHECMobileExtension(normalizedCgPN);
			if (extInfo == null) log.warn("Could not find hec info for Msisdn: "+normalizedCgPN);
			return extInfo;					
		} 
		catch (Exception e)
		{
			log.warn("validateMO_A_MSISDN: General Exception "+e);
			e.printStackTrace();			
		}
		return null;		
	}

	/**
	 * MO scenario from UCM_OUT_ONNET_NO_INTERCONN 
	 * invoked by 'processMO' to distinguish from MO_NO_RR and UCM_OUT scenarios
	 * (bug 13156)
	 * 
	 * @param calledPN  E.164 international formatted address with noai=Unknown
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */	
	public static CSTriggerValidation validateCS_MO_Scenario (String calledPN, int noai, 
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)	
	{
		if(log.isInfoEnabled())log.info("Validating MO scenario for "+calledPN);		
		try
		{
			ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
			String token1=null;
			
			for (Hec_OpcoVO opco:opcoList)
			{
				//<IAC>CC<OPcode><P1>   				
				//				token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getOperatorCode()).append(opco.getPrefix1()).toString();
				if(opco.getIac() != null && noai == cgPN_cdPN_noai_UNK)
				{
					//<IAC>CC get B number   
					token1 = new StringBuffer(opco.getIac()).append(opco.getCountryCode()).toString();
					if(log.isInfoEnabled())log.info("Trying to match cdPN: "+calledPN+" | <IAC><CC>: "+token1);
				}
				else if (noai == cgPN_cdPN_noai_INT)
				{
					//CC  get B  number
					token1 = Integer.toString(opco.getCountryCode());
					if(log.isInfoEnabled())log.info("Trying to match cdPN: "+calledPN+" | <CC>: "+token1);
				}
				// TODO check this is ok
				else if (noai == cgPN_cdPN_noai_NAT) 
				{
					//<NAC> get B number. don't we need roaming info for this??   
					//					token1 = opco.getNac();
					token1="";
					if(log.isInfoEnabled())log.info("Assuming national significant cdPN: "+calledPN);
				}
				else
				{
					//Actually, this should never happen!!!
					if(log.isInfoEnabled())log.info("Unsupported NOAI: "+noai);					
					//BUG 12060
					//					return null;
					return new CSTriggerValidation(CSScenario.UNSUPPORTED_NOAI, null, null);
				}
				

				//1st strip IAC+CC
				//2nd try to strip OPCode+P1 -> UCM_OUT_ONNET_NO_INTERCONN
				//                		else -> MT_NO_RR
				if(calledPN != null && token1 != null && calledPN.startsWith(token1))
				{
					//BINGO
					String numBNatFormat = calledPN.substring(token1.length());  
					String token2 = new StringBuffer(opco.getOperatorCode()).append(opco.getPrefix1()).toString();
					//BUG 7848
					//					if(calledPN.startsWith(token2))
					if(log.isInfoEnabled())log.info("Trying to match "+numBNatFormat+" | <OPCode><P1>: "+token2);
					if(numBNatFormat.startsWith(token2))
					{
						String correlationId = numBNatFormat.substring(token2.length());
						if(log.isInfoEnabled())log.info("Found UCM BREAKOUT ONNET with no INTERCONNECTION scenario. Opco: "+opco.getOpCoName());
						return new CSTriggerValidation(CSScenario.UCM_OUT_ONNET_NO_INTERCONN, opco, correlationId);
					}
					else
					{
						//						String numB = opco.getCountryCode()+numBNatFormat;
						if(log.isInfoEnabled())log.info("Found MO_NO_RR scenario. Opco: "+opco.getOpCoName());	
						return new CSTriggerValidation(CSScenario.MO_NO_RR, opco, numBNatFormat);
					}
				}							
			}
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}		
		return null;

	}	
	/**
	 * MT scenario from UCM_OUT_ONNET_NO_INTERCONN
	 * invoked by 'processMT' to distinguish from MT_NO_RR and UCM_OUT scenarios
	 * 
	 * @param calledPN  E.164 international formatted address with noai=Unknown
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */	
	public static CSTriggerValidation validateCS_MT_Scenario (String calledPN, int noai, 
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)	
	{
		if(log.isInfoEnabled())log.info("Validating MT scenario for "+calledPN);		
		try
		{
			ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
			String token1=null;
			
			for (Hec_OpcoVO opco:opcoList)
			{
				//<IAC>CC<OPcode><P1>   				
				//				token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getOperatorCode()).append(opco.getPrefix1()).toString();
				if(opco.getIac() != null && noai == cgPN_cdPN_noai_UNK)
				{
					//<IAC>CC get B number   
					token1 = new StringBuffer(opco.getIac()).append(opco.getCountryCode()).toString();
					if(log.isInfoEnabled())log.info("Trying to match cdPN: "+calledPN+" | <IAC><CC>: "+token1);
				}
				else if (noai == cgPN_cdPN_noai_INT)
				{
					//CC  get B  number
					token1 = Integer.toString(opco.getCountryCode());
					if(log.isInfoEnabled())log.info("Trying to match cdPN: "+calledPN+" | <CC>: "+token1);
				}
				// TODO check this is ok
				else if (noai == cgPN_cdPN_noai_NAT) 
				{
					//<NAC> get B number. don't we need roaming info for this??   
					//					token1 = opco.getNac();
					token1="";
					if(log.isInfoEnabled())log.info("Assuming national significant cdPN: "+calledPN);
				}
				else
				{
					//Actually, this should never happen!!!
					if(log.isInfoEnabled())log.info("Unsupported NOAI: "+noai);					
					//BUG 12060
					//					return null;
					return new CSTriggerValidation(CSScenario.UNSUPPORTED_NOAI, null, null);
				}
				

				//1st strip IAC+CC
				//2nd try to strip OPCode+P1 -> UCM_OUT_ONNET_NO_INTERCONN
				//                		else -> MT_NO_RR
				if(calledPN != null && token1 != null && calledPN.startsWith(token1))
				{
					//BINGO
					String numBNatFormat = calledPN.substring(token1.length());
					///// BUG 13156: do this:
					if(log.isInfoEnabled())log.info("Found MT_NO_RR scenario. Opco: "+opco.getOpCoName());	
					return new CSTriggerValidation(CSScenario.MT_NO_RR, opco, numBNatFormat);
					///// BUG 13156: ...instead of this:
					//					String token2 = new StringBuffer(opco.getOperatorCode()).append(opco.getPrefix1()).toString();
					//					//BUG 7848
					//					//					if(calledPN.startsWith(token2))
					//					if(log.isInfoEnabled())log.info("Trying to match "+numBNatFormat+" | <OPCode><P1>: "+token2);
					//					if(numBNatFormat.startsWith(token2))
					//					{
					//						String correlationId = numBNatFormat.substring(token2.length());
					//						if(log.isInfoEnabled())log.info("Found UCM BREAKOUT ONNET with no INTERCONNECTION scenario. Opco: "+opco.getOpCoName());
					//						return new CSTriggerValidation(CSScenario.UCM_OUT_ONNET_NO_INTERCONN, opco, correlationId);
					//					}
					//					else
					//					{
					//						//						String numB = opco.getCountryCode()+numBNatFormat;
					//						if(log.isInfoEnabled())log.info("Found MT_NO_RR scenario. Opco: "+opco.getOpCoName());	
					//						return new CSTriggerValidation(CSScenario.MT_NO_RR, opco, numBNatFormat);
					//					}
				}							
			}
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}		
		return null;

	}
	/**
	 * 
	 * @param callingPN
	 * @param noai
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static CSMONormalizedANumber getCSMONormalizedANumber (String callingPN, int noai, String mscAddress,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)	
	{
		if(log.isInfoEnabled())log.info("Processing CgPN for MO scenario "+callingPN);		
		try
		{
			ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
			String token=null;			
			for (Hec_OpcoVO opco:opcoList)
			{
				if(opco.getIac() != null && noai == cgPN_cdPN_noai_UNK)
				{
					//<IAC>CC get B number: [callingPN with IAC stripped]
					token = new StringBuffer(opco.getIac()).append(opco.getCountryCode()).toString();
					if(log.isInfoEnabled())log.info("Normalizing A. NOAI UNK. Trying to match IAC CC: "+callingPN+" | token: "+token);					
					if(callingPN != null && token != null && callingPN.startsWith(token))
					{
						String normalizedA = callingPN.substring(opco.getIac().length());
						return new CSMONormalizedANumber(opco, normalizedA);
					}
				}
				else if (noai == cgPN_cdPN_noai_INT)
				{
					//CC  get B  number: [callingPN AS-IS]									
					token = Integer.toString(opco.getCountryCode());
					if(log.isInfoEnabled())log.info("Normalizing A. NOAI INT. Trying to match CC: "+callingPN+" | token: "+token);					
					if(callingPN != null && token != null && callingPN.startsWith(token))
					{
						return new CSMONormalizedANumber(opco, callingPN);
					}					
				}
				else if (noai == cgPN_cdPN_noai_NAT)
				{
					//no CC: find CC from MSCAddress, making the assumption that if A party in MO is 
					// in national format, the msc MUST be in a provisioned OpCo.
					// [CC+callingPN]				
					token = Integer.toString(opco.getCountryCode());
					if(log.isInfoEnabled())log.info("Normalizing A. NOAI NAT. Trying to match msc CC: "+mscAddress+" | token: "+token);	
					if(mscAddress != null && token != null && mscAddress.startsWith(token))
					{
						
						return new CSMONormalizedANumber(opco, token+callingPN);
					}					
				}				
				else // other noai values
				{
					//Actually, this should never happen!!!
					if(log.isInfoEnabled())log.info("Unsupported NOAI: "+noai);					
					return null;					
				}										
			}
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}		
		return null;

	}	
	/**
	 * Tool function to map internal RCC to SIP 4xx,5xx,6xx responses 
	 * 
	 * @param releaseCauseId
	 * @return
	 */
	public static ReleaseCauseVO getIMSReleaseCause(int releaseCauseId)
	{		
		try
		{
			return ReleaseCauseHelper.getReleaseCauseCode(releaseCauseId, ReleaseCauseVO.DOMAIN_IMS);
		} catch (NoDataFoundException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Tool function to map internal RCC to CAP RCC 
	 * 
	 * @param releaseCauseId
	 * @return
	 */
	public static ReleaseCauseVO getCSReleaseCause(int releaseCauseId)
	{		
		try
		{
			return ReleaseCauseHelper.getReleaseCauseCode(releaseCauseId, ReleaseCauseVO.DOMAIN_CS);

		} 
		catch (NoDataFoundException e)
		{			
			e.printStackTrace();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		
		log.error("Could not find proper Release cause for "+releaseCauseId);
		//TODO choose a default one
		return null;
	}	
	
	
	// Number Type & noai constants
	public final static int nt_INT = 1; //CalledPartyBCDNumber.NumberType.INTERNATIONAL;	 			
	public final static int nt_NAT = 2; //CalledPartyBCDNumber.NumberType.NATIONAL;
	public final static int nt_UNK = 0; //CalledPartyBCDNumber.NumberType.UNKNOWN		
	public final static int cgPN_cdPN_noai_INT = 4; //Called&CallingPartyNumber.Nature._INTERNATIONAL
	public final static int cgPN_cdPN_noai_NAT = 3; //Called&CallingPartyNumber.Nature._NATIONAL     This is definitely not expected.
	public final static int cgPN_cdPN_noai_UNK = 2; //Called&CallingPartyNumber.Nature._UNKNOWN
	public final static int cgPN_cdPN_noai_UNSUPPORTED = -1; //UNSUPPORTED NOAI
	/**
	 * By the moment, there is no need to implement the opposite mapping
	 * @param bcdNT
	 * @return
	 */
	public static int convertBCDNT2NOAI (int bcdNT)
	{
		int mappedNTValue = -1;
    	switch(bcdNT)
    	{
    		case HECLogic.nt_UNK: 	mappedNTValue = HECLogic.cgPN_cdPN_noai_UNK;
    								break;
    		case HECLogic.nt_NAT: 	mappedNTValue = HECLogic.cgPN_cdPN_noai_NAT;
    								break;
    		case HECLogic.nt_INT: 	mappedNTValue = HECLogic.cgPN_cdPN_noai_INT;
    								break;
    		default:				mappedNTValue = HECLogic.cgPN_cdPN_noai_UNSUPPORTED; //BUG 12060
    	}	
    	return mappedNTValue;
	}
	
	/**
	 * Utility to detect addresses with '*' '#' or any non-decimal characters
	 * @param address
	 * @return
	 */
	public static boolean checkNonDecimalDigits(String address)
	{
		if(address == null) return false;
		for(int i=0 ; i<address.length();i++)
		{
			if(address.charAt(i) > '9' || address.charAt(i) < '0') return true;
		}
		return false;
	}
	/**
	 * B number normalization and Special Number detection. 
	 * Only for CS Originating scenario.
	 * 2 versions: pre-CR and post-CR
	 * 
	 * 
	 * @param cdPN
	 * @return
	 */
	//	public static CSNormalizedBNumber csNormalizeBNumber_PRE_CR(String numB, int numberType, RoamingInfo roamingInfo,
	//			/*Hec_GroupVO*/Hec_ExtensionInfoVO extInfoA, 
	//			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	//	{
	//		if(numB == null)
	//		{
	//			log.warn("Null B number: "+numB);
	//			return null;
	//		}
	//
	//		String normalizedB = null;
	//		Hec_SpecialNumsVO sdVO = null;
	//		switch(numberType)
	//		{
	////			case nt_UNK:
	//			case cgPN_cdPN_noai_UNK:
	//				statsHelper.incCounter(Constants.ct_CS_MO_B_NOAI_Unknown);				
	//				//for every IAC:
	//				if(log.isDebugEnabled())log.debug("Processing B number with Unknown NOAI. Roaming Info: "+roamingInfo);
	//				//BUG 8296:increment both counters('noai unk' and 'nondecimal') when nondecimal digits are detected
	//				// Check non decimal digits in the address. In this case, do not normalize.
	//				if(checkNonDecimalDigits(numB))
	//				{
	//					if(log.isDebugEnabled())log.debug("Non decimal digits detected. Leaving the address as is: "+numB);
	//					statsHelper.incCounter(Constants.ct_CS_MO_B_Nondecimal);
	//					return new CSNormalizedBNumber(BNumberType.NonDecimalNumber, numB, numB);
	//				}				
	//				String[] iacList = roamingInfo.mscCountry.getInternationalCodes();
	//				//IAC testing
	//				for(String iac : iacList)
	//				{
	//					if(log.isDebugEnabled())log.debug("Checking IAC: "+iac);
	//					if(iac != null && iac.trim().length() != 0 && numB.startsWith(iac))
	//					{
	//						//stripIAC(bNum)		
	//						if(log.isDebugEnabled())log.debug("IAC detected: "+iac);
	//						normalizedB = numB.substring(iac.length());
	//						//checkSpecialDestination(bNum); Only if not in roaming
	//						if(!roamingInfo.isInRoaming && (sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
	//						{
	//							//<IAC><CC><Special Destination>							
	//							if(log.isDebugEnabled())log.debug("SpecialDestination with IAC detected: "+normalizedB);
	//							statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);
	//							return new CSNormalizedBNumber(numB, sdVO);
	//						}
	//						else
	//						{
	//							//<IAC><CC><National#>
	//							if(log.isDebugEnabled())log.debug("Public number detected: "+normalizedB);
	//							statsHelper.incCounter(Constants.ct_CS_MO_B_Public);
	//							return new CSNormalizedBNumber(BNumberType.PublicNumberINT, normalizedB, numB);
	//						}
	//					}
	//				}
	//				String[] nacList = roamingInfo.mscCountry.getNationalCodes();
	//				//NAC testing
	//				for(String nac : nacList)
	//				{
	//					if(log.isDebugEnabled())log.debug("Checking NAC: "+nac);
	//					if(nac != null && nac.trim().length() != 0 &&  numB.startsWith(nac))
	//					{
	//						//stripNAC(bNum)		
	//						if(log.isDebugEnabled())log.debug("NAC detected: "+nac);
	//						//CC+bNumNational
	//						normalizedB = roamingInfo.mscCountry.getCountryCode()+numB.substring(nac.length());
	//						//checkSpecialDestination(bNum); Only if not in roaming
	//						if(!roamingInfo.isInRoaming && (sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
	//						{
	//							//<NAC><Special Destination>
	//							if(log.isDebugEnabled())log.debug("SpecialDestination with NAC detected: "+normalizedB);
	//							statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);
	//							return new CSNormalizedBNumber(numB, sdVO);
	//						}
	//						else
	//						{
	//							//<NAC><National#>
	//							if(log.isDebugEnabled())log.debug("Public number detected (supposed): "+normalizedB);
	//							statsHelper.incCounter(Constants.ct_CS_MO_B_Public);
	//							return new CSNormalizedBNumber(BNumberType.PublicNumberNAT, normalizedB, numB);
	//						}
	//					}
	//					else
	//					{
	//						if(log.isDebugEnabled())log.debug("Non matching NAC discarded: "+nac);
	//					}
	//				}				
	//				//No NAC detected: CC+numB 
	//				normalizedB = roamingInfo.mscCountry.getCountryCode()+numB;
	//				if(log.isDebugEnabled())log.debug("Checking special destination with no NAC nor IAC, normalized with CC: "+normalizedB);				
	//				if(!roamingInfo.isInRoaming && (sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
	//				{
	//					//<Special Destination>
	//					if(log.isDebugEnabled())log.debug("SpecialDestination without NAC detected: "+normalizedB);
	//					statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);
	//					return new CSNormalizedBNumber(numB, sdVO);
	//				}
	//				else if (!roamingInfo.isInRoaming)
	//				{
	//					//<NAC><National#> in countries with NAC=null
	//					if(log.isDebugEnabled())log.debug("Public number guessed, after discarding all other possibilities: "+normalizedB);
	//					statsHelper.incCounter(Constants.ct_CS_MO_B_Public);
	//					return new CSNormalizedBNumber(BNumberType.PublicNumberNAT, normalizedB, numB);					
	//				}
	//				else
	//				{
	//					//PNP Short Number: leave it untouched
	//					normalizedB = numB;
	//					if(log.isDebugEnabled())log.debug("PNP Short number detected: "+normalizedB);
	//					statsHelper.incCounter(Constants.ct_CS_MO_B_PNP_candidate);
	//					return new CSNormalizedBNumber(BNumberType.PNPShortNumber, normalizedB, numB);
	//				}							
	////				break; Unreachable
	//			default://Unsupported
	//				log.warn("Unsupported NOAI "+numberType+" for B number "+numB);
	//				statsHelper.incCounter(Constants.ct_CS_MO_B_Unsupported_NOAI);
	//				return null;				
	//		}			
	//	}	
	
	/**
	 * B number normalization and Special Number detection. 
	 * Only for CS Originating scenario. POST_CR version
	 * 
	 * @param numB
	 * @param numberType
	 * @param roamingInfo
	 * @param extInfoA
	 * @return
	 */
	/*************************************************************/
	/** TODO: update with changes from the method above **********/
	/*************************************************************/
	public static CSNormalizedBNumber csNormalizeBNumber(String numB, int numberType, RoamingInfo roamingInfo, 
			/*Hec_GroupVO*/Hec_ExtensionInfoVO extInfoA,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(numB == null)
		{
			log.warn("Null B number: "+numB);
			return null;
		}
		
		String normalizedB = null;
		Hec_SpecialNumsVO sdVO = null;
		switch(numberType)
		{
//			case nt_INT:
			case cgPN_cdPN_noai_INT:
				statsHelper.incCounter(Constants.ct_CS_MO_B_NOAI_International);				
				if(log.isDebugEnabled())log.debug("Processing B number with International NOAI in country: "+roamingInfo.mscCountry);	
				// Check non decimal digits in the address. In this case, do not normalize. BUG 11356
				if(checkNonDecimalDigits(numB))
				{
					if(log.isDebugEnabled())log.debug("Non decimal digits detected. Leaving the address as is: "+numB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Nondecimal);
					return new CSNormalizedBNumber(BNumberType.NonDecimalNumber, numB, numB);
				}				
				//<CC><Special Destination>
				normalizedB = numB;
				//checkSpecialDestination(bNum);
				sdVO=null;
				if((sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
				{
					//CC<Special Destination>
					if(log.isDebugEnabled())log.debug("SpecialDestination detected: "+normalizedB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);//BUG 12188: included
					return new CSNormalizedBNumber(numB, sdVO);
				}
				else
				{
					//CC<National#>
					if(log.isDebugEnabled())log.debug("Public international number detected: "+normalizedB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Public);//BUG 12188: included
					return new CSNormalizedBNumber(BNumberType.PublicNumberINT, normalizedB, numB);
				}												
//								break;
//			case nt_NAT:
			case cgPN_cdPN_noai_NAT:	
				statsHelper.incCounter(Constants.ct_CS_MO_B_NOAI_National);				
				if(log.isDebugEnabled())log.debug("Processing B number with National NOAI in country: "+roamingInfo.mscCountry);
				// Check non decimal digits in the address. In this case, do not normalize. BUG 11356
				if(checkNonDecimalDigits(numB))
				{
					if(log.isDebugEnabled())log.debug("Non decimal digits detected. Leaving the address as is: "+numB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Nondecimal);
					return new CSNormalizedBNumber(BNumberType.NonDecimalNumber, numB, numB);
				}				
				//Normalize CC+numB
				normalizedB = roamingInfo.mscCountry.getCountryCode()+numB;
				if((sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
				{
					//<Special Destination>
					if(log.isDebugEnabled())log.debug("SpecialDestination detected: "+normalizedB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);//BUG 12188: included
					return new CSNormalizedBNumber(numB, sdVO);
				}
				else
				{
					//<National#>
					if(log.isDebugEnabled())log.debug("Public national number detected: "+normalizedB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Public);//BUG 12188: included
					return new CSNormalizedBNumber(BNumberType.PublicNumberNAT, normalizedB, numB);
				}				
				
				//				break;
//			case nt_UNK:
			case cgPN_cdPN_noai_UNK:
				statsHelper.incCounter(Constants.ct_CS_MO_B_NOAI_Unknown);
				//for every IAC:
				if(log.isDebugEnabled())log.debug("Processing B number with Unknown NOAI in country: "+roamingInfo.mscCountry);
				//BUG 8296:increment both counters('noai unk' and 'nondecimal') when nondecimal digits are detected
				// Check non decimal digits in the address. In this case, do not normalize.
				if(checkNonDecimalDigits(numB))
				{
					if(log.isDebugEnabled())log.debug("Non decimal digits detected. Leaving the address as is: "+numB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Nondecimal);
					return new CSNormalizedBNumber(BNumberType.NonDecimalNumber, numB, numB);
				}					
				String[] iacList = roamingInfo.mscCountry.getInternationalCodes();
				for(String iac : iacList)
				{
					if(log.isDebugEnabled())log.debug("Checking IAC: "+iac);
					if(iac != null && iac.trim().length() != 0 && numB.startsWith(iac))
					{
						//stripIAC(bNum)		
						if(log.isDebugEnabled())log.debug("IAC detected: "+iac);
						normalizedB = numB.substring(iac.length());
						//checkSpecialDestination(bNum);
						if((sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
						{
							//<IAC><CC><Special Destination>
							if(log.isDebugEnabled())log.debug("SpecialDestination with IAC detected: "+normalizedB);
							statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);
							return new CSNormalizedBNumber(numB, sdVO);
						}
						else
						{
							//<IAC><CC><National#>
							if(log.isDebugEnabled())log.debug("Public number detected: "+normalizedB);
							statsHelper.incCounter(Constants.ct_CS_MO_B_Public);
							return new CSNormalizedBNumber(BNumberType.PublicNumberINT, normalizedB, numB);
						}
					}
				}
				String[] nacList = roamingInfo.mscCountry.getNationalCodes();
				for(String nac : nacList)
				{
					if(log.isDebugEnabled())log.debug("Checking NAC: "+nac);
					if(nac != null && nac.trim().length() != 0 &&  numB.startsWith(nac))
					{
						//stripNAC(bNum)		
						if(log.isDebugEnabled())log.debug("NAC detected: "+nac);
						//CC+bNumNational
						normalizedB = roamingInfo.mscCountry.getCountryCode()+numB.substring(nac.length());
						//checkSpecialDestination(bNum);
						if((sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
						{
							//<NAC><Special Destination>
							if(log.isDebugEnabled())log.debug("SpecialDestination with NAC detected: "+normalizedB);
							statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);
							return new CSNormalizedBNumber(numB, sdVO);
						}
						else
						{
							//<NAC><National#>
							if(log.isDebugEnabled())log.debug("Public number detected (supposed): "+normalizedB);
							statsHelper.incCounter(Constants.ct_CS_MO_B_Public);
							return new CSNormalizedBNumber(BNumberType.PublicNumberNAT, normalizedB, numB);
						}
					}
					else
					{
						if(log.isDebugEnabled())log.debug("Non matching NAC discarded: "+nac);
					}
				}	
				
				/*********************************************************************************/
				/* Ad-hoc treatment for C&R in the spanish network. Remove as soon as possible. ***/
				/*********************************************************************************/

				//local network / OpCo   in the 'special list' ?
				
				// if so, then check prefix list 
				
				// if numB starts with one of the configured prefixes then set it as PublicNumberNAT
				
				//continue with the following logic, otherwise
				/*********************************************************************************/
				
				
				//No NAC detected: CC+numB 
				normalizedB = roamingInfo.mscCountry.getCountryCode()+numB;
				if(log.isDebugEnabled())log.debug("Checking special destination with no NAC nor IAC, normalized with CC: "+normalizedB);				
				if((sdVO=checkSpecialDestination(normalizedB, extInfoA.getVpnId()))!= null)
				{
					//<Special Destination>
					if(log.isDebugEnabled())log.debug("SpecialDestination without NAC detected: "+normalizedB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_Special_Destination);
					return new CSNormalizedBNumber(numB, sdVO);
				}
				else
				{
					//PNP Short Number: leave it untouched
					normalizedB = numB;
					if(log.isDebugEnabled())log.debug("PNP Short number detected: "+normalizedB);
					statsHelper.incCounter(Constants.ct_CS_MO_B_PNP_candidate);
					return new CSNormalizedBNumber(BNumberType.PNPShortNumber, normalizedB, numB);
				}							
				//				break; Unreachable
			default://Unsupported
				log.warn("Unsupported NOAI "+numberType+" for B number "+numB);
				statsHelper.incCounter(Constants.ct_CS_MO_B_Unsupported_NOAI);
				return null;				
		}	
		
	}
	
	/**
	 * TODO: under construction
	 * 
	 * @param sDCandidate
	 * @param vpnId
	 * @return
	 */
	public static Hec_SpecialNumsVO checkSpecialDestination(String sDCandidate, Integer vpnId)
	{
		try
		{
			return Hec_SpecialNumsDAO.getSpNByVPNAndIncomingNumber(sDCandidate, vpnId);
			
		} catch (NoDataFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	/** 
	 * NOT CALLED ANYMORE
	 * Called in IMS domain and CS domain
	 * 
	 * No formatting needed. Expected B number in E.164 with NOAI=unknown, that is <IAC><CC><Nat num>
	 * 
	 * @param cdPN
	 */
//	public static Hec_GroupVO validateMT_B_MSISDN (String cdPN, RoamingInfo roamingInfo)
//	public static Hec_GroupVO validateHEC_MSISDN (String cdPN)
//	{
//		if(log.isDebugEnabled())log.debug("Trying to validate B number as a HEC subscriber: "+cdPN);		
//		try
//		{	
// 
//			//Do this again in a more compact way:
//			
//			Hec_IndextVO indextVO = Hec_IndextDAO.getIndextByMsisdn(cdPN);
//			if(indextVO == null)
//			{
//				log.warn("Could not find msisdn "+cdPN);
//				return null;
//			}			
//			if(log.isDebugEnabled())log.debug("Searching group for groupId "+indextVO.getGroupId());
//			Hec_GroupVO groupVO = Hec_GroupDAO.getGroupByGroupId(indextVO.getGroupId());
//			if (groupVO == null) log.warn("Msisdn "+cdPN+" has a non existing groupId "+indextVO.getGroupId());
//			return groupVO;					
//		}
//		catch (NoDataFoundException e)
//		{
//			log.warn("validateMT_B_MSISDN: NoDataFoundException Exception "+e);
//			e.printStackTrace();
//		} catch (InvalidParamException e)
//		{
//			log.warn("validateMT_B_MSISDN: InvalidParamException Exception "+e);
//			e.printStackTrace();
//		}
//		return null;		
//	}
	
	/**
	 * This one is used by CS Rerouting *************** CURRENTLY NOT IN USE
	 * 
	 * @param internationalAddress
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
//	public static String getMOReroutingPrefix (String internationalAddress,
//			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
//	{
//		if(log.isDebugEnabled())log.debug("Trying to find rerouting prefix for "+internationalAddress);
//		Hec_OpcoVO opcoVO = new Hec_OpcoVO();
//		try
//		{	
//			//Best matching in opcos table. 
//			opcoVO = Hec_OpcoDAO.getOpcoByInternationalAddress(internationalAddress);
//			if (opcoVO != null) return opcoVO.getCountryCode()+opcoVO.getOperatorCode()+opcoVO.getPrefix1();
//			if(log.isInfoEnabled())log.info("No rerouting prefix could be found for "+internationalAddress);			
//		}
//		catch (NoDataFoundException e)
//		{
//			log.warn("NoDataFoundException Exception "+e);
//			e.printStackTrace();
//		} catch (InvalidParamException e)
//		{
//			log.warn("InvalidParamException Exception "+e);
//			e.printStackTrace();
//		}
//		return null;		
//	}
	
	/**
	 * Called in CS domain from 'processMT_NO_RR'
	 * 
	 * @param cdPN
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static String getMTReroutingPrefix (String cdPN,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(log.isDebugEnabled())log.debug("Trying to find rerouting prefix for IDP with CdPN "+cdPN);
		Hec_OpcoVO opcoVO = new Hec_OpcoVO();
		try
		{	
			//Best matching in opcos table. 
			opcoVO = Hec_OpcoDAO.getOpcoByInternationalAddress(cdPN);
			if (opcoVO != null)
			{
				String numBNat = cdPN.substring(opcoVO.getCountryCode().toString().length(), cdPN.length());			
				return opcoVO.getCountryCode()+opcoVO.getPrefix3()+numBNat;
			}
			if(log.isInfoEnabled())log.info("No rerouting prefix could be found for IDP with CdPN "+cdPN);			
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}
		return null;		
	}		


	/**
	 *  Encode Connect DRA according originating/destination scenarios
	 *  
	 * @param scenario
	 * @param bNumber
	 * @param ticket
	 * @param opcoVO
	 * @return
	 * @throws UnsupportedOperationException
	 */
//	public static String getEncodedDra(IMSScenario scenario, String bNumber, String ticket, Hec_OpcoVO opcoVO)
//	  throws UnsupportedOperationException
//	  {
//	    String rv = null;
//	    if(scenario.equals(IMSScenario.MO_NO_RR))
//	    {	    	
//	      StringBuilder sb = new StringBuilder();
//	      sb.append(opcoVO.getCountryCode())
//	      	.append(opcoVO.getOperatorCode())
//	      	.append(opcoVO.getPrefix1())
//	        .append(ticket); 
//	      rv = sb.toString();
//	    }
//	    else if(scenario.equals(IMSScenario.MT_NO_RR))
//	    {
//	      StringBuilder sb = new StringBuilder();
//	      sb.append(opcoVO.getCountryCode())
//	      	.append(opcoVO.getPrefix3())
//	      	.append(bNumber)
//	        .append(ticket); 
//	      rv = sb.toString();      
//	    }
//	    else
//	    {
//	      //rv will be null
//	    }
//	    return rv;
//	  } 	
	
	
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////		IMS LOGIC								//////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////			
	/**
	 * 
	 * @param trigger: the TriggerName string as is, i.e. "hec_mo", "hec_mt", etc
	 * @param requestURIUser
	 * @param other
	 * @return
	 */
	//TODO statistics
	public static IMSTriggerValidation validateTrigger (String trigger, HECLogic.URIUserPart requestURIUser, String reqURIDomain, boolean emergency,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		if(log.isInfoEnabled())log.info("Validating trigger for "+trigger+" URI:"+requestURIUser);		
		try
		{	
			//hec_mo			
			if (IMSTrigger.hec_mo.toString().equals(trigger))
			{
				statsHelper.incCounter(Constants.ct_IMS_hec_mo);
				int index=-1; 		
				ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
				String token=null;
				for (Hec_OpcoVO opco:opcoList)
				{
					//+CC<OPcode><P1>  get CorrelationID					
					token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getOperatorCode()).append(opco.getPrefix1()).toString();
					if(log.isInfoEnabled())log.info("Trying to match prefix1 "+requestURIUser+" | token: "+token);
					index = requestURIUser.user.indexOf(token); 
//					if(index != -1)
					if(index == 0)
					{
						//BINGO
						//TODO this must include only the number, not other params like domain, etc
						String correlationId = requestURIUser.user.substring(index+token.length());  
						if(log.isInfoEnabled())log.info("Found MO with no rerouting scenario. Opco: "+opco.getOpCoName());
						return new IMSTriggerValidation(IMSScenario.MO_NO_RR, opco, 
								new IMSBNumber(BNumberType.Correlation, correlationId, requestURIUser),
								reqURIDomain);
					}
				// This scenario has been discarded					
				//					else
				//					{
				//						//+CC<P2>  get NumB
				//						token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getPrefix2()).toString();
				//						if(log.isInfoEnabled())log.info("Trying to match prefix2 "+requestURIUser+" | token: "+token);
				//						index = requestURIUser.indexOf(token); 
				//						if(index != -1)
				//						{
				//							/* BINGO */
				//							String numB = requestURIUser.substring(index+token.length()); 
				//							if(log.isInfoEnabled())log.info("Found MO with rerouting scenario. Opco: "+opco.getOpCoName());
				//							return new IMSTriggerValidation(IMSScenario.MO_RR, opco, numB, reqURIDomain);
				//						}
				//					}
				}
				if(log.isInfoEnabled())log.info("Could not validate MO trigger");
			}
			//hec_mt
			else if (IMSTrigger.hec_mt.toString().equals(trigger))
			{
				statsHelper.incCounter(Constants.ct_IMS_hec_mt);
				int index=-1; //TODO check found index is 0		
				ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
				String token=null;
				for (Hec_OpcoVO opco:opcoList)
				{
					//+CC<P3> get NumB
					token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getPrefix3()).toString();
					if(log.isInfoEnabled())log.info("Trying to match prefix3 "+requestURIUser+" | token: "+token);
					index = requestURIUser.user.indexOf(token);					
//					if(index != -1)
					if(index == 0)
					{
						//BINGO
						String numB = opco.getCountryCode()+requestURIUser.user.substring(index+token.length()); 
						if(log.isInfoEnabled())log.info("Found MT with no rerouting scenario. Opco: "+opco.getOpCoName());
						
						return new IMSTriggerValidation(IMSScenario.MT_NO_RR, opco, 
								new IMSBNumber(BNumberType.PublicNumberINT, numB, requestURIUser), 
								reqURIDomain);
					}
					else
					{
						//+CC<P4> get NumB
						token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getPrefix4()).toString();
						if(log.isInfoEnabled())log.info("Trying to match prefix4 "+requestURIUser+" | token: "+token);						
						index = requestURIUser.user.indexOf(token); 
//						if(index != -1)
						if(index == 0)
						{
							/* BINGO */
							String numB = opco.getCountryCode()+requestURIUser.user.substring(index+token.length()); 
							if(log.isInfoEnabled())log.info("Found MT with rerouting scenario. Opco: "+opco.getOpCoName());							
							return new IMSTriggerValidation(IMSScenario.MT_RR, opco, 
									new IMSBNumber(BNumberType.PublicNumberINT, numB, requestURIUser), 
									reqURIDomain);
						}
					}					
				}
				if(log.isInfoEnabled())log.info("Could not validate MT trigger");
			}
			//hec_ucm_incoming
			else if (IMSTrigger.hec_ucm_incoming.toString().equals(trigger))
			{
				statsHelper.incCounter(Constants.ct_IMS_hec_ucm_incoming);
				int index=-1; //TODO check found index is 0		
				ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
				String token=null;				
				for (Hec_OpcoVO opco:opcoList)
				{
					//+CC<P5> get NumB					
					token = new StringBuffer(IAC_PLUS_SIGN).append(opco.getCountryCode()).append(opco.getPrefix5()).toString();
					if(log.isInfoEnabled())log.info("Trying to match prefix5 "+requestURIUser+" | token: "+token);						
					index = requestURIUser.user.indexOf(token); 					
//					if(index != -1)
					if(index == 0)
					{
						//BINGO
						String numB = opco.getCountryCode()+requestURIUser.user.substring(index+token.length()); 
						if(log.isInfoEnabled())log.info("Found PSTN HEC terminated scenario. Opco: "+opco.getOpCoName());						
						return new IMSTriggerValidation(IMSScenario.UCM_IN_PSTN, opco, 
								new IMSBNumber(BNumberType.PublicNumberINT, numB, requestURIUser),  
								reqURIDomain);
					}
				}
				if(log.isInfoEnabled())log.info("Could not validate UCM INCOMING trigger");
			}
			//Breakout:
			//hec_ucm_outgoing: 
			//this one is different. We already know the group by the Contact Header and the trigger is equal for all groups
			else if (IMSTrigger.hec_ucm_outgoing.toString().equals(trigger))
			{
				statsHelper.incCounter(Constants.ct_IMS_hec_ucm_outgoing);
				//No need to get opCo list as far as I know....
				int index=-1; //TODO check found index is 0		
				//				ArrayList<Hec_OpcoVO> opcoList = Hec_OpcoDAO.getAllOpcos();
				//get group by contact, Opco by CC<numBNat>
				String token=null;				
				//Accesible from SL_PARAMS 
				String psiOrig = confHelper.getString("HEC_PSI_ORIG");
				String onNetPrefix = confHelper.getString("HEC_ONNET_PREFIX");
				if(log.isInfoEnabled())log.info("Configured UCM outgoing prefixes: "+psiOrig+" / "+onNetPrefix);				

				//+<PSIOrig><onNet>CC  get NumB
				token = new StringBuffer(IAC_PLUS_SIGN).append(psiOrig).append(onNetPrefix).toString();
				if(log.isInfoEnabled())log.info("Trying to match PSIOrig Onnet prefix "+requestURIUser+" | token: "+token);						
				index = requestURIUser.user.indexOf(token); 					
				if(index != -1)
				{
					//BINGO
					String numB = requestURIUser.user.substring(index+token.length()); 			
					if(log.isInfoEnabled())log.info("Found PSTN HEC breakout onnet scenario. Num B: "+numB);						
					return new IMSTriggerValidation(IMSScenario.UCM_OUT_ONNET, null, 
							new IMSBNumber(BNumberType.PublicNumberINT, numB, requestURIUser),  
							reqURIDomain);
				}
				else
				{
					//+<PSIOrig>CC get NumB
					token = new StringBuffer(IAC_PLUS_SIGN).append(psiOrig).toString();
					if(log.isInfoEnabled())log.info("Trying to match PSI prefix "+requestURIUser+" | token: "+token);						
					index = requestURIUser.user.indexOf(token); 						
					if(index != -1)
					{
						//BINGO
						String numB = requestURIUser.user.substring(index+token.length()); 							
						if(log.isInfoEnabled())log.info("Found PSTN HEC breakout offnet scenario. Num B: "+numB);
						return new IMSTriggerValidation(IMSScenario.UCM_OUT_OFFNET, null, 
								new IMSBNumber(BNumberType.PublicNumberINT, numB, requestURIUser),  
								reqURIDomain);
					}
					else
					{
						//Still, it can be a ServiceAccessCode or an emergency number
						//<PSIOrig> get NumB
						// BUG 7611 FIXED
						//					token = new StringBuffer(IAC_PLUS_SIGN).append(psiOrig).toString();
						token = psiOrig;
						if(log.isInfoEnabled())log.info("Trying to match PSI prefix for possible Emergency/SAC "+requestURIUser+" | token: "+token);						
						index = requestURIUser.user.indexOf(token); 							
						if(index != -1)
						{
							//CR004
							if(emergency)
							{
								String numB = requestURIUser.user.substring(index+token.length()); 
								if(log.isInfoEnabled())log.info("Found PSTN HEC breakout emergency scenario. Num B: "+numB);
								return new IMSTriggerValidation(IMSScenario.UCM_OUT_OFFNET_EMERGENCY, null, 
										new IMSBNumber(BNumberType.EmergencyNumber, numB, requestURIUser),  
										reqURIDomain);
							}
							else
							{
								String numB = requestURIUser.user.substring(index+token.length()); 
								if(log.isInfoEnabled())log.info("Found PSTN HEC breakout special access code scenario. Num B: "+numB);
								return new IMSTriggerValidation(IMSScenario.UCM_OUT_OFFNET, null, 
										new IMSBNumber(BNumberType.SpecialDestination, numB, requestURIUser),  
										reqURIDomain);								
							}
						}							
					}
				}

				if(log.isInfoEnabled())log.info("Could not validate UCM OUTGOING trigger");				
			}
			statsHelper.incCounter(Constants.ct_IMS_Unsupported_Trigger);
			if(log.isInfoEnabled())log.info("Trigger not configured");
		} 
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Called in CS domain from 'processMT_NO_RR'
	 * 
	 * @param cdPN
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
//	public static Hec_GroupVO getHecGroupById (Integer groupId,
//			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
//	{
//		if(log.isDebugEnabled())log.debug("Trying to find group info for id "+groupId);
//		try
//		{	
//			//Best matching in opcos table. 
//			Hec_GroupVO groupVO = Hec_GroupDAO.getGroupByGroupId(groupId);
//			
//			if (groupVO != null)
//			{
//				return groupVO;
//			}
//			if(log.isDebugEnabled())log.debug("No group found for group id "+groupId);			
//		}
//		catch (NoDataFoundException e)
//		{
//			log.warn("NoDataFoundException Exception "+e);
//			e.printStackTrace();
//		} catch (InvalidParamException e)
//		{
//			log.warn("InvalidParamException Exception "+e);
//			e.printStackTrace();
//		}
//		return null;		
//	}		
	

	/**
	 * 
	 * @param genericHeader
	 * @return
	 */
//	public static Hec_ExtensionInfoVO processPAIDExtension (ExtensionHeaderImpl paidHeader)
//	public static Hec_ExtensionInfoVO processEHExtension (ExtensionHeaderImpl extensionHeader, 
	public static Hec_ExtensionInfoVO processEHExtension (SIPHeader genericHeader,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		try
		{
		  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" is "+genericHeader.getHeaderValue());		  
//		  SipUri extensionHeaderURI = HECLogic.getSipUriFromHeaderNameAddress(extensionHeader.getHeaderValue());				  			  
		  GenericURI extensionHeaderURI = HECLogic.getGenericUriFromHeaderNameAddress(genericHeader.getHeaderValue());
		  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" URI is "+extensionHeaderURI);	
		  String user=getUserFromGenericURL(extensionHeaderURI);
		  if(user != null)
		  {
			  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" USER is "+user);
			  return HECLogic.validateHECExtension(user, confHelper, statsHelper);
		  }
		  else
		  {
			  return null;
		  }
//		  return groupVO;
		}
		catch(Exception e)
		{
			log.warn("Exception processing "+genericHeader.getHeaderName()+": "+genericHeader);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 
	 * @param paidHeader
	 * @return
	 */
//	public static Hec_ExtensionInfoVO processPAIDMobile (ExtensionHeaderImpl paidHeader)
//	public static Hec_ExtensionInfoVO processEHMobile (ExtensionHeaderImpl extensionHeader)
	public static Hec_ExtensionInfoVO processEHMobile (SIPHeader genericHeader)
	{
		try
		{
		  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" is "+genericHeader.getHeaderValue());		  
//		  SipUri extensionHeaderURI = HECLogic.getSipUriFromHeaderNameAddress(extensionHeader.getHeaderValue());				  			  
//		  if(log.isDebugEnabled())log.debug(extensionHeader.getHeaderName()+" URI is "+extensionHeaderURI);		  		  
//		  if(log.isDebugEnabled())log.debug(extensionHeader.getHeaderName()+" USER is "+extensionHeaderURI.getUser());	  
//		  //Now, strip IAC-PLUS_SIGN if necessary		  
//		  String user = extensionHeaderURI.getUser().startsWith(IAC_PLUS_SIGN)?extensionHeaderURI.getUser().substring(IAC_PLUS_SIGN.length()):extensionHeaderURI.getUser();
		  GenericURI extensionHeaderURI = HECLogic.getGenericUriFromHeaderNameAddress(genericHeader.getHeaderValue());
		  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" URI is "+extensionHeaderURI);	
		  String user=getUserFromGenericURL(extensionHeaderURI);
		  if(user != null)
		  {
			  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" USER is "+user);
			  return HECLogic.validateHECMobileExtension(user);
		  }
		  else
		  {
			  return null;
		  }		  
//		  return groupVO;
		}
		catch(Exception e)
		{
			log.warn("Exception processing "+genericHeader.getHeaderName()+": "+genericHeader);
			e.printStackTrace();
		}
		return null;
	}	
	/**
	 * 
	 * @param paidHeader
	 * @return
	 */
//	public static Hec_ExtensionInfoVO processPAIDGeographic (ExtensionHeaderImpl paidHeader)
//	public static Hec_ExtensionInfoVO processEHGeographic (ExtensionHeaderImpl extensionHeader,
	public static Hec_ExtensionInfoVO processEHGeographic (SIPHeader genericHeader,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		try
		{
		  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" is "+genericHeader.getHeaderValue());		  
//		  SipUri extensionHeaderURI = HECLogic.getSipUriFromHeaderNameAddress(extensionHeader.getHeaderValue());				  			  
//		  if(log.isDebugEnabled())log.debug(extensionHeader.getHeaderName()+" URI is "+extensionHeaderURI);		  		  
//		  if(log.isDebugEnabled())log.debug(extensionHeader.getHeaderName()+" USER is "+extensionHeaderURI.getUser());	  
//		  //Now, strip IAC-PLUS_SIGN if necessary		  
//		  String user = extensionHeaderURI.getUser().startsWith(IAC_PLUS_SIGN)?extensionHeaderURI.getUser().substring(IAC_PLUS_SIGN.length()):extensionHeaderURI.getUser();
		  GenericURI extensionHeaderURI = HECLogic.getGenericUriFromHeaderNameAddress(genericHeader.getHeaderValue());
		  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" URI is "+extensionHeaderURI);	
		  String user=getUserFromGenericURL(extensionHeaderURI);
		  if(user != null)
		  {
			  if(log.isDebugEnabled())log.debug(genericHeader.getHeaderName()+" USER is "+user);
			  Hec_ExtensionInfoVO extInfVO = HECLogic.validateHECGeographicExtension(user, confHelper, statsHelper);
			  return extInfVO;
		  }
		  else
		  {
			  return null;
		  }			  
//		  return groupVO;
		}
		catch(Exception e)
		{
			log.warn("Exception processing "+genericHeader.getHeaderName()+": "+genericHeader);
			e.printStackTrace();
		}
		return null;
	}		
	/**
	 * 
	 * @param paid
	 * @return
	 */
//	public static Hec_ExtensionInfoVO processUCM_PSTN_PAID(String paid)
//	{
//		try
//		{			
////			Hec_PnRangeVO pnRangeVO = Hec_PnRangeDAO.getPnRangeBySubscriber(paid);
////			return Hec_GroupDAO.getGroupByGeographicNumber(paid);
//			return Hec_ExtensionInfoDAO.getExtensionInfoByGeographicNumber(paid);
//		} catch (NoDataFoundException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvalidParamException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
//	}
	/**
	 * 
	 * @param contactHeader
	 * @return
	 */
	public static Hec_GroupVO processUCMContactHeader(String contactHeader)
	{
		if(log.isDebugEnabled())log.debug("Looking for trunk params in contact header: " +contactHeader);
		Matcher ctxMatcher = patternUCMTrunkCtx.matcher(contactHeader);	    
	    String trunkCtx = null, trunkGroup = null;
	    if (ctxMatcher.find()) 
	    {
			trunkCtx = ctxMatcher.group(2); 
			if(log.isDebugEnabled())log.debug("Found trunk context from contact " +trunkCtx);
			Matcher groupMatcher = patternUCMTrunkGroup.matcher(contactHeader);
			if (groupMatcher.find())
			{
				  trunkGroup = groupMatcher.group(2);
				  if(log.isDebugEnabled())log.debug("Found trunk group from contact " +trunkGroup);
			}
			try
			{	
				//Best matching in opcos table. 
				Hec_GroupVO groupVO = Hec_GroupDAO.getGroupByTrunkParams(trunkCtx, trunkGroup);
				
				if (groupVO != null)
				{
					return groupVO;
				}
				if(log.isDebugEnabled())log.debug("No group found for trunk params "+trunkCtx+" / "+trunkGroup);			
			}
			catch (NoDataFoundException e)
			{
				log.warn("NoDataFoundException Exception "+e);
				e.printStackTrace();
			} catch (InvalidParamException e)
			{
				log.warn("InvalidParamException Exception "+e);
				e.printStackTrace();
			}	      			
			return null;
	    }
	    else
	    {
	      log.warn("No requested DB, so it must be a CUR Request.");
	      return null;
	    }		
	}
	
	/**
	 * 
	 * @param numB
	 * @param confHelper
	 * @param statsHelper
	 * @return
	 */
	public static Hec_OpcoVO getServingOpCo (String numB)
	{
		if(log.isDebugEnabled())log.debug("Trying to find serving opco for hec mobile extension: "+numB);
		Hec_OpcoVO opcoVO = new Hec_OpcoVO();
		try
		{	
			//Best matching in opcos table. 
			opcoVO = Hec_OpcoDAO.getOpcoByInternationalAddress(numB);
			if (opcoVO != null)
			{						
				return opcoVO;
			}
			if(log.isInfoEnabled())log.info("No opco could be found for extension: "+numB);			
		}
		catch (NoDataFoundException e)
		{
			log.warn("NoDataFoundException Exception "+e);
			e.printStackTrace();
		} catch (InvalidParamException e)
		{
			log.warn("InvalidParamException Exception "+e);
			e.printStackTrace();
		}
		return null;		
	}

//	/**
//	 * This is just to prove compilation access to these classes 
//	 * TODO: remove if finally this classes are not used
//	 * 
//	 * @param header
//	 * @param requestUri
//	 * @return
//	 */
//	public boolean validateTrigger (Header header, SipUri requestUri)
//	{
//		return false;
//	}
	
	
	
	//////////////////////////////////// UTILS ///////////////////////////////////////////////////////////////////////
		
	
	  /**
	   * Writes the DummyNumber received as an argument on the DummyNumber-RA using the
	   * RA provider. The Dummy-RA does not fire events on the service, so it is not 
	   * necessary to attach to the activity.
	   * 
	   * TODO: Consider to do a iDummyHelper for us containing this (the e
	   * 
	   * @return
	   */
	public static String recordCorrelationContext(String opCoId, byte[] payload, DummyRAActivityProvider correlationRaActivityProvider)
		throws Exception
	{
	    DummyRAActivity correlationActivity = correlationRaActivityProvider.newDummyActivity();
	    if (correlationActivity != null)
	    {
	        CorrelationNumberContainer correlationContainer = new CorrelationNumberContainer();
	        correlationContainer.setPayload(payload);    
	        correlationContainer.setCreationDate( System.currentTimeMillis() );
		    String ticket = correlationActivity.setDummyNumber(opCoId, correlationContainer);
		    correlationActivity.endActivity();
					    
					    // The Dummy-RA does not fire any event on the service, so it is not 
					    // necessary to attach the service to the RA activity. Thus, the 
					    // following lines are not necessary (keep them commented)
					    // if(dummyActivity != null)
					    // {
					    //    ActivityContextInterface dummyACI = aciFactory.getActivityContextInterface(dummyActivity);
					    //    dummyACI.attach(getSbbLocalObject());
					    // }
					    
					    // Return the activity
		    if(ticket == null) throw new Exception("Unavailable correlation number");
		    
		    return ticket;
	    }
	    //This should release the call RELEASE_CAUSE
	    throw new Exception("Unavailable correlation activity");
	}
	
	/**
	 * 
	 * ASN.1:
	 * 
	 * name-addr	=	 [ display-name ] LAQUOT addr-spec RAQUOT
	 * ; example:
	 * ;    Bob <sip:bob@biloxi.example.com> 
	 * 
	 * @param nameAddress
	 * @return
	 */
	public static /*SipUri*/ GenericURI getGenericUriFromHeaderNameAddress (String nameAddress)
	{
		Matcher sipAddressMatcher = patternSIPNameAddress.matcher(nameAddress);
		try
		{
			if(sipAddressMatcher.find())
			{
				StringMsgParser smp = new StringMsgParser();
//				return smp.parseSIPUrl(sipAddressMatcher.group(2));
				return smp.parseUrl(sipAddressMatcher.group(2));
			}
			else
			{
				StringMsgParser smp = new StringMsgParser();
//				return smp.parseSIPUrl(nameAddress);
				return smp.parseUrl(nameAddress);
			}			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * ASN.1:
	 * 
	 * name-addr	=	 [ display-name ] LAQUOT addr-spec RAQUOT
	 * ; example:
	 * ;    Bob <sip:bob@biloxi.example.com> 
	 * 
	 * @param nameAddress
	 * @return
	 */
	public static NameAddress getNameAddressFromHeaderNameAddress (String nameAddress)
	{
//		Matcher sipAddressMatcher = patternSIPNameAddress.matcher(nameAddress);
		Matcher sipAddressMatcher = patternSIPNameAddressAdvanced.matcher(nameAddress);
		try
		{
			if(sipAddressMatcher.find())
			{
				StringMsgParser smp = new StringMsgParser();
//				return smp.parseSIPUrl(sipAddressMatcher.group(2));
//				return smp.parseUrl(sipAddressMatcher.group(2));
				return new NameAddress(sipAddressMatcher.group(1), smp.parseUrl(sipAddressMatcher.group(2)));
			}
			else
			{
				StringMsgParser smp = new StringMsgParser();
//				return smp.parseSIPUrl(nameAddress);
				return new NameAddress(null,smp.parseUrl(nameAddress));
			}			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}	
	public static String getUserFromGenericURL (GenericURI extensionHeaderURI)
	{
		  	
		  String user=null;
		  if (extensionHeaderURI.getScheme().equals(GenericURI.TEL))
		  {
			  //Now, strip IAC-PLUS_SIGN if necessary		  
			  user = ((TelURLImpl)extensionHeaderURI).getPhoneNumber();	  
			  //(These are actually free from the prefix '+')			  
		  }
		  else
		  {			  
			  //Now, strip IAC-PLUS_SIGN if necessary		  
			  user = ((SipUri)extensionHeaderURI).getUser();			  	 
			  //			  user=user.startsWith(IAC_PLUS_SIGN)?user.substring(IAC_PLUS_SIGN.length()):user;�
			  //BUG 10436
			  if(user != null && user.startsWith(IAC_PLUS_SIGN))
			  {
				  user = user.substring(IAC_PLUS_SIGN.length());
				  //Also, strip user params
				  if(user.contains(";"))
				  {
					  user = user.substring(0, user.indexOf(';'));
				  }
			  }
			  else
			  {
				  if(log.isDebugEnabled())log.debug("Unsupported format: "+user);
				  user = null;
			  }			  
		  }
		  return user;
	}
	/**
	 * CR003: method to get the Diversion Address and compose a P-served-User address
	 * 
	 * @param diversion
	 * @param sipProtocolData
	 * @return
	 */
	public static /*ExtensionHeaderImpl*/Header generatePAIDHeaderFromDiversion (ExtensionHeaderImpl diversion, Request sipProtocolData)
	{
		//		GenericURI diversionHeaderURI = HECLogic.getGenericUriFromHeaderNameAddress(diversion.getHeaderValue());	
		//BUG 11655
		NameAddress diversionHeaderNameAddress = getNameAddressFromHeaderNameAddress(diversion.getHeaderValue());
		//		String diversionValue = diversion.getHeaderValue();
		//		//Strip diversion header specific parameters: TODO check we need to do this
		//		int paramIndex = diversionValue.indexOf(";");	  
		//		String strippedDiversionValue = paramIndex > -1 ? diversionValue.substring(0, paramIndex): diversionValue;
		
		//		StringBuilder paidString = new StringBuilder("<").append(diversionHeaderURI).append('>').append(";sescase=term;regstate=unreg");
		StringBuilder paidString = new StringBuilder(diversionHeaderNameAddress.toString()).append(";sescase=term;regstate=unreg");
//		Header paidHeader = sipProtocolData.createHeader("P-Served-User", paidString.toString());JA>TODO
//		headerFactory.createHeader("P-Served-User", paidString.toString());

 
		return null/*paidHeader*/;
	}
	
	////////////////////////
	// MCID Feature CR007 //
	////////////////////////
	public static void checkMCID (Request sipProtocolData, Hec_ExtensionInfoVO extInfoB,
			WriterProvider mcidLogWriterProvider,
			ConfigurationHelper confHelper, StatisticsHelper statsHelper)
	{
		try
		{
			String calledPartyNumber = extInfoB.getExtType() == ExtType.mobile?
					extInfoB.getMsisdn()
					:extInfoB.getIsdn();
			if(log.isDebugEnabled())log.debug("Searching MCID state for: "+calledPartyNumber);
			Hec_McidVO mcidVO = Hec_McidDAO.getMcidByLongNumber(calledPartyNumber); 
			if(mcidVO != null && mcidVO.getMcidStatus()==1)
			{
				if(log.isDebugEnabled())log.debug("MCID found and active for: "+calledPartyNumber);
				//				Privacy privacyHeader = (Privacy)sipProtocolData.getHeader(HDR_PRIVACY);
				// that did not work, so
				ExtensionHeader privacyHeader = (ExtensionHeader)sipProtocolData.getHeader(HDR_PRIVACY);
				if (privacyHeader != null)	if(log.isDebugEnabled())log.debug("Found PRIVACY header: "+privacyHeader);

				//[..]
				
				// TIMESTAMP
				StringBuilder mcidLog = new StringBuilder(Long.toString(System.currentTimeMillis()));
				// FROM				
				From fromHeader = (From)sipProtocolData.getHeader(HDR_FROM);
				if(fromHeader != null)
				{
					if(log.isDebugEnabled())log.debug("Found FROM header: "+fromHeader.getValue());				
					mcidLog.append('|').append(fromHeader.getHeaderValue());
				}
				else
				{
					log.error("FROM header NOT found. ");	
					mcidLog.append('|');
				}
				// PAID			
				// We could do sth like this:
				//				ListIterator<SIPHeader> paidHeaders = sipProtocolData.getHeaders(HDR_PAID);
				//				while (paidHeaders.hasNext())
				//				{
				//					SIPHeader paidHeader = paidHeaders.next();
				//					if(log.isDebugEnabled())log.debug("Found PAID header: "+paidHeader.getValue());
				//					if(log.isDebugEnabled())log.debug("------------------ "+getGenericUriFromHeaderNameAddress(paidHeader.getValue()));
				//					mcidLog.append('|').append(paidHeader);
				//				}
				// Let's use the topmost, anyway
				PAssertedIdentity paidHeader = (PAssertedIdentity)sipProtocolData.getHeader(HDR_PAID);
				if(paidHeader!=null)
				{
					if(log.isDebugEnabled())log.debug("Found PAID header: "+paidHeader);	
					mcidLog.append('|').append(paidHeader.getAddress());
				}
				else
				{
					if(log.isDebugEnabled())log.debug("PAID header NOT found ");	
					mcidLog.append('|');
				}
				// CALLED PARTY NUMBER
				mcidLog.append('|').append(calledPartyNumber);
				if(log.isDebugEnabled())log.debug("Dumping MCID log: ["+mcidLog+"]"); 
				mcidLogWriterProvider.writeMessage(mcidLog.toString());
			}
			else if(mcidVO != null && mcidVO.getMcidStatus()!=1)
			{
				if(log.isDebugEnabled())log.debug("MCID found but inactive for: "+calledPartyNumber);
			}
			else
			{
				if(log.isDebugEnabled())log.debug("No MCID found for: "+calledPartyNumber);
			}
			
		}
		catch(Exception e)
		{
			log.error("Processing MCID for "+extInfoB+ ". Exception: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
