/*
 * com.atos.ngin.hec.units.sbb.main.CSReroutingSbb.java        January 28, 2011 
 * 
 * Copyright (c) 2011 AtosOrigin
 * Albarracin 25, 28037 Madrid
 * All Rights Reserved.
 * 
 * This software is confidential and proprietary information of AtosOrigin.
 * You shall not disclose such confidential information and shall use it only
 * in accordance with the terms of the license agreement established with
 * AtosOrigin.
 */
package com.atos.ngin.hec.units.sbb;



import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.InitialEventSelector;
import javax.slee.SbbContext;
import javax.slee.TransactionRolledbackLocalException;
import javax.slee.facilities.FacilityException;

import org.apache.log4j.Level;

import com.atos.ivpn.dummy.ratype.CorrelationNumberContainer;
import com.atos.ivpn.dummy.ratype.DummyRAActivity;
import com.atos.ivpn.dummy.ratype.DummyRAActivityContextInterfaceFactory;
import com.atos.ivpn.dummy.ratype.DummyRAActivityProvider;
import com.atos.ngin.hec.hec_db.catalog.releasecause.ReleaseCauseVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_OpcoVO;
import com.atos.ngin.hec.units.hec_logic.HECContextFactory;
import com.atos.ngin.hec.units.hec_logic.HECLogic;
import com.atos.ngin.hec.units.hec_logic.HECLogic.CSMONormalizedANumber;
import com.atos.ngin.hec.units.hec_logic.HECLogic.CSNormalizedBNumber;
import com.atos.ngin.hec.units.hec_logic.HECLogic.CSScenario;
import com.atos.ngin.hec.units.hec_logic.HECLogic.CSTriggerValidation;
import com.atos.ngin.hec.units.hec_logic.HECLogic.RoamingInfo;
import com.atos.ngin.hec.units.hec_logic.MO_NO_RR_Context;
import com.atos.ngin.hec.units.hec_logic.UCM_OUT_NO_INTERCONN_Context;
import com.atos.ngin.hec.units.sbb.common.Constants;
import com.atos.ngin.hec.units.sbb.common.Constants.HECUsageCounter;
import com.atos.ngin.hec.units.sbb.common.HecBaseSbb;
import com.atos.ngin.hec.units.usage.CSReroutingUsageParameters;
import com.atos.ngin.hec.units.usage.HECUsageParameters;
import com.opencloud.slee.resources.cgin.ComponentEvent;
import com.opencloud.slee.resources.cgin.Dialog;
import com.opencloud.slee.resources.cgin.DialogOpenRequestEvent;
import com.opencloud.slee.resources.cgin.ProtocolException;
import com.opencloud.slee.resources.cgin.SccpAddress;
import com.opencloud.slee.resources.cgin.SccpAddressParser;
import com.opencloud.slee.resources.cgin.TcapApplicationContext;
import com.opencloud.slee.resources.cgin.TooManyInvokesException;
import com.opencloud.slee.resources.cgin.callcontrol.CCConnectArg;
import com.opencloud.slee.resources.cgin.callcontrol.CCDialog;
import com.opencloud.slee.resources.cgin.callcontrol.CCEventTypeBCSM;
import com.opencloud.slee.resources.cgin.callcontrol.CCInitialDPArg;
import com.opencloud.slee.resources.cgin.callcontrol.CCReleaseCallArg;
import com.opencloud.slee.resources.cgin.callcontrol.events.CCInitialDPRequestEvent;
import com.opencloud.slee.resources.cgin.cap_v1.CAP1InitialDPArg;
import com.opencloud.slee.resources.cgin.cap_v1.metadata.CAP1ApplicationContexts;
import com.opencloud.slee.resources.cgin.cap_v2.metadata.CAP2ApplicationContexts;
import com.opencloud.slee.resources.cgin.etsi_inap_cs1.metadata.CS1ApplicationContexts;
import com.opencloud.slee.resources.cgin.map.MAPLocationInformation;
import com.opencloud.slee.resources.in.datatypes.BCDStringCodec;
import com.opencloud.slee.resources.in.datatypes.cc.AddressString;
import com.opencloud.slee.resources.in.datatypes.cc.CalledPartyBCDNumber;
import com.opencloud.slee.resources.in.datatypes.cc.CalledPartyNumber;
import com.opencloud.slee.resources.in.datatypes.cc.CallingPartyNumber;
import com.opencloud.slee.resources.in.datatypes.cc.CallingPartysCategory;
import com.opencloud.slee.resources.in.datatypes.cc.Cause;
import com.opencloud.slee.resources.in.datatypes.cc.GenericNumber;
import com.opencloud.slee.resources.in.datatypes.cc.OriginalCalledNumber;

/**
 * This SBB implements the CS rerouting functionality of the service logic for HEC.
 * Basically, it accepts a call-control dialog by receiving an IDP event and
 * reroutes the call by sending a special prefix and a correlation id in a connect message.
 * 
 * It uses the OpenCloud CGIN-RA framework (for IN dialogs).
 */
public abstract class CSReroutingSbb extends HecBaseSbb
{
  //--------------------------------- Constants ----------------------------//
  
	//Release Causes
//	< 1, CS, 34, Internal Error >
//	< 2, CS, 33, Parameter error >
//	< 3, CS, 35, ICS >
//	< 4, CS, 36, OCS >
//	< 5, CS, 20, Originating extension inactive >
//	< 6, CS, 20, Originating extension inactive and the call is forwarded. >
//	< 7, CS, 27, Destination extension inactive >
//	< 8, CS, 47, No announcement in roaming >
//	< 9, CS, 47, No message found for announcement and language >
//	< 10, CS, 31, Unexpected data for SMS Logic >
//	< 11, CS, 17, Standard busy release cause. >
//	< 12, CS, 31, Error in NEXUS request. >
//	< 14, CS, 1, Not Found >
//	< 17, CS, 34, CALLED NOT AVAILABLE >
//	< 18, CS, 17, CALLED BUSY >
//	< 19, CS, 18, CALLED NO ANSWER >
//	< 20, CS, 3, CALLED NOT REACHABLE >
//	< 21, CS, 31, UNDEFINED >
//	< 22, CS, 21, Intra Convergent calls >
  
  public static final int RC_INCONSISTENT_IDP_DATA = 2;
  public static final int RC_NO_NETWORK_VALIDATION = 2; //UNUSED
  public static final int RC_NO_CORRELATION_AVAILABLE = 1;
  public static final int RC_NO_A_VALIDATION = 14; // 1/'Not found'  BUG 7850
  public static final int RC_NO_RR_PREFIX_AVAILABLE = 1;
  public static final int RC_NO_ROAMING_INFO = 14;
  public static final int RC_NO_B_VALIDATION = 14; // 1/'Not found'  BUG 7867
  public static final int RC_CANNOT_NORMALIZE_B = 14;
  public static final int RC_INTERNAL_ERROR = 1;
  public static final int RC_UNSUPPORTED_CAMEL_VERSION = 2;
  
  public static final int RC_DEFAULT = 14; //must be parseable as integer...
  
  // DummyRA Binding
  protected final static String CORRELATIONRA_PROVIDER = "CorrelationRAActivityProvider";
  protected final static String CORRELATIONRA_FACTORY  = "CorrelationRAActivityContextInterfaceFactory";
  
  // Address codec for DRA
  protected static final String bcdStringCodecBase = "0123456789abcdef";
  protected static final String bcdDiallingStringCodecBase= "0123456789*#cdef";
  protected static final BCDStringCodec<CalledPartyNumber> bcdStringCodecCdPN = new BCDStringCodec<CalledPartyNumber>(bcdStringCodecBase, true, 0, true, 1);
  protected static final BCDStringCodec<CalledPartyBCDNumber> bcdStringCodecCdPBCDN = new BCDStringCodec<CalledPartyBCDNumber>(bcdDiallingStringCodecBase, true, 0, true, 1);
  protected static final BCDStringCodec<CallingPartyNumber> bcdStringCodecCgPN = new BCDStringCodec<CallingPartyNumber>(bcdStringCodecBase, true, 0, true, 1);
  protected static final BCDStringCodec<AddressString> bcdStringCodecAS = new BCDStringCodec<AddressString>(bcdStringCodecBase, true, 0, true, 1);
  protected static final BCDStringCodec<OriginalCalledNumber> bcdStringCodecOCdPN = new BCDStringCodec<OriginalCalledNumber>(bcdDiallingStringCodecBase, true, 0, true, 1);
  
 
  //JC SCCP address switching method:
  private static final String SET_SCCP_CALLING_ADDRESS_PARAM_NAME = "HEC_SET_SCCP_CALLING_ADDRESS";
  private static final String LOCAL_SSN_PARAM_NAME = "HEC_LOCAL_SSN";
  private static final String LOCAL_SSN_DEF = "146"; //CAMEL SSN
  
  private static final String SCCP_HEADER_PARAM_NAME = "type=c7,ri=gt";
  private static final String SCCP_DIGITS_PARAM_NAME = ",digits=";
  private static final String SCCP_NATURE_PARAM_NAME = ",nature=";
  private static final String SCCP_TT_PARAM_NAME = ",tt=";
  private static final String SCCP_NUMBERING_PARAM_NAME = ",numbering=";
  private static final String SCCP_SSN_PARAM_NAME = ",ssn=";  
	//  private static final String ADDRESS_PARAM_NAME = "ADDRESS";
	//  private static final String NAI_PARAM_NAME = "NAI";
	//  private static final String TT_PARAM_NAME = "TT";
	//  private static final String NP_PARAM_NAME = "NP";
  private static final String SSN_PARAM_NAME = "SSN"; 
  private static final String PC_PARAM_NAME = "PC";     

  private static final String SCCP_GT_PARAM_NAME = "HEC_SCCP_GT_MAP_";
  private static final String SCCP_POP = "POP";
  private static final String SCCP_UNDERSCORE = "_";
  private static final String SCCP_GT_DEFAULT_PARAM_NAME = "HEC_SCCP_GT_DEFAULT";
  private static final String SCCP_RI_PARAM_NAME = ",ri=";
  private static final String SCCP_RI_GT_VALUE_NAME = "gt";
  private static final String SCCP_TT_DEF_VALUE_NAME = "0";
  private static final String SCCP_PC_PARAM_NAME = ",pc=";

  
//  private static final String HEC_CS_SERVICE_KEY_CONF_PARAM = "HEC_CS_SERVICE_KEY";
//  private static final int HEC_CS_DEFAULT_SERVICE_KEY = 60;
  
  //--------------------------------- Variables ----------------------------//
  
  // DummyRA PROVIDER
  protected DummyRAActivityProvider                correlationRaActivityProvider = null; 
  protected DummyRAActivityContextInterfaceFactory correlationRaAciFactory = null;  

  
  //---------------------------------- Methods -----------------------------//  
   
  /**
   * Invoke super to get common facilities, then get specific providers for this service. 
   */
  public void setSbbContext(SbbContext context)
  {
    super.setSbbContext(context);
    try
    {
      this.correlationRaActivityProvider = (DummyRAActivityProvider)this.jndiContext.lookup(CORRELATIONRA_PROVIDER);
      if(dbgEnabled())trace(Level.INFO,"DummyRA activity provider: " + this.correlationRaActivityProvider);
      this.correlationRaAciFactory = (DummyRAActivityContextInterfaceFactory)this.jndiContext.lookup(CORRELATIONRA_FACTORY);
      System.err.println("ACI factory: " + this.correlationRaAciFactory);
          
    } 
    catch (NamingException ne) {
      if(dbgEnabled())trace(Level.DEBUG,"** Error obtaining sbbContext",ne.getMessage());
    } catch (TransactionRolledbackLocalException e) {
      if(dbgEnabled())trace(Level.DEBUG,"** TransactionRolledbackLocalException" + e.getMessage());
    } catch (FacilityException e) {
      if(dbgEnabled())trace(Level.DEBUG,"** FacilityException", e.getMessage());
    } catch (NullPointerException e) {
      if(dbgEnabled())trace(Level.DEBUG,"** NullPointerException", e.getMessage());
    } catch (IllegalArgumentException e) {
      if(dbgEnabled())trace(Level.DEBUG,"** IllegalArgumentException", e.getMessage());
    }
    if(infoEnabled())trace(Level.DEBUG,"End setSbbcontext");    
  }
  
  
  /**
   * Open Request handler method. 
   * Added snippet by Chema
   *
   * @param event DialogOpenRequest event
   * @param aci ActivityContextInterface associated with the activity where the event was fired
   */
  public void onOpenRequest(DialogOpenRequestEvent event, ActivityContextInterface aci)
  {
    if(infoEnabled())trace(Level.INFO,"OpenRequest event received: ",event); 
    
    Dialog dialog = event.getDialog();    
    try
    {    	
	    
    	if(!(dialog instanceof CCDialog))
    	{
    		trace (Level.INFO, "Unsupported dialog type. Refusing open request: "+dialog);
    		incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
    		//dialog.refuseDialog((Object[])null);
    		dialog.refuseDialog();
    	}
    	else
    	{
    		CCDialog ccDialog = (CCDialog)dialog;
    	    // Generate a unique identifier based on the IDP dialog. It is stored as a CMP
    	    // field.
    	    String dialogId = ccDialog.getDialogID();
    	    this.storeTracePreamble(dialogId);
    	    if(dbgEnabled()) trace(Level.DEBUG,"IDP DialogId: ",dialogId);
    	    
//    		ccDialog.setSCCPReturnOption(true);
    		SccpAddress incomingCdAddress = event.getDestinationAddress();
    		// TODO: This is the source of location information in CS1
    		SccpAddress sccpCgPN = event.getOriginatingAddress();
	    	SccpAddress outgoingCgSccpAddr = null;
	    	try
	    	{
	    		outgoingCgSccpAddr = computeCgSccpAddress(incomingCdAddress);
	    	}
	    	catch(Exception e)
	    	{
	    		incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
	    		trace(Level.WARN,"Could not find HEC_Cluster_GT from Called SCCP: "+incomingCdAddress);
	    	}
	        if (outgoingCgSccpAddr != null) {
	        	ccDialog.acceptDialog (outgoingCgSccpAddr);
	            if (infoEnabled ())
	                trace (Level.INFO, "accepted dialog (cgSccpAddr=", outgoingCgSccpAddr, ")");
	        }
	        else {
	            ccDialog.acceptDialog ();
	            if (infoEnabled ())
	                trace (Level.INFO, "Accepted dialog.");
	        }
	        
	        boolean protocolVersionSupported = validateACN(event.getApplicationContext());
	        if(!protocolVersionSupported)
	        {
	        	trace (Level.INFO, "Unsupported ACN: "+event.getApplicationContext());
	        	statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid_Protocol);
	        	//        	dialog.refuseDialog(null);
	        	//	        	releaseCall(RC_UNSUPPORTED_CAMEL_VERSION, ccDialog, aci);
	        	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
	        	continueCall(ccDialog,aci);
	        }
	        else
	        {
		        final ComponentEvent[] components = event.getComponentEvents();
		        for (ComponentEvent component : components)
		        {
		            // IDP
		            if (component instanceof CCInitialDPRequestEvent)
		            {
		                onInitialDP((CCInitialDPRequestEvent) component, sccpCgPN, aci);
		            }
		        }
	        }
    	}
        
    }
    catch (ProtocolException pe)
    {
    	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
    	pe.printStackTrace();
    }
    catch (Exception pe)
    {
    	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
    	pe.printStackTrace();
    }    
  }   
  
  /**
   * Former IDP event handler method. 
   * Now invoked from 'onOpenRequest', the only event handler in this sbb.
   */
  public void 
  onInitialDP(CCInitialDPRequestEvent event, SccpAddress sccpCgPN, ActivityContextInterface aci)
  {
	long initTime = System.nanoTime();
    if(infoEnabled())trace(Level.INFO,"<<<< Received InitialDP event: ",event);
    
    statsHelper.incCounter(Constants.ct_CS_IDPS);
    incrementCounter(HECUsageCounter.INCOMING_MESSAGE);  
    
    
    // Receive the initial DP and identify calling and called party numbers
    CCDialog dialog = event.getDialog();
    CCInitialDPArg arg = event.getArgument();  
    
    //No SKEY checking is performed (different opcos will use different SKEYs):
	//    int serviceKey = arg.getServiceKey();
	//    int hecServiceKey = confHelper.getInt(HEC_CS_SERVICE_KEY_CONF_PARAM) != null?
	//    		confHelper.getInt(HEC_CS_SERVICE_KEY_CONF_PARAM):
	//    			HEC_CS_DEFAULT_SERVICE_KEY;
	//    if (serviceKey != hecServiceKey)
	//    {
	//    	if(dbgEnabled()) trace(Level.DEBUG,"Invalid Service Key: ",serviceKey," Expected: ",hecServiceKey);
	//    	releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	//    	statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid);
	//    	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
	//    	return;
	//    }
    
	//    CallingPartyNumber cgPN = arg.getCallingPartyNumber();
	//    CalledPartyNumber cdPN = arg.getCalledPartyNumber();
    GenericNumber additionalCgPN = arg.getAdditionalCallingPartyNumber();
    CallingPartysCategory cgPC = arg.getCallingPartysCategory();
    //    LocationNumber locationN = arg.getLocationNumber();
    CCEventTypeBCSM eType = arg.getEventTypeBCSM();
    //    if(infoEnabled()) trace(Level.INFO,"Number [",cgPN,"] calling to [",cdPN,']');
    if(infoEnabled()) trace(Level.INFO,"Additional CallingPN: "+additionalCgPN);
    if(infoEnabled()) trace(Level.INFO,"Calling Party Category: "+cgPC);
    //    if(infoEnabled()) trace(Level.INFO,"Location Number: "+locationN);
    //    if(infoEnabled()) trace(Level.INFO,"SCCP CgPN: "+sccpCgPN);
    if(infoEnabled()) trace(Level.INFO,"EventTypeBCSM: "+eType);
    //TODO check data consistency, i.e. no nulls
    boolean inconsistentData = false;
    if(inconsistentData)
    {    	
    	releaseCall(RC_INCONSISTENT_IDP_DATA,dialog,aci);
    	statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid);
    	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
    	return;
    }
    
    ////////////////////////
    //  TRIGGER SCENARIO  //
    ////////////////////////      
     if (isMobileOriginated(eType))
     {
    	 //MO_NO_RR HEC and UCM_OUT_ONNET_NO_INTERCONN HEC scenarios
    	 HECLogic.CSScenario csScenario = processMO(event, sccpCgPN, aci);
    	 statsHelper.incCounter(Constants.ct_CS_Mobile_Originated);
    	 if(infoEnabled())trace(Level.INFO,"IDP MO processed successfully");    
    	 sampleCounter(csScenario,(System.nanoTime()-initTime)/1000);
     }
     else
     {
    	 //MT_NO_RR 
    	 HECLogic.CSScenario csScenario = processMT(event, aci);
    	 statsHelper.incCounter(Constants.ct_CS_Mobile_Terminated);
    	 if(infoEnabled())trace(Level.INFO,"IDP MT processed successfully");
    	 
    	 sampleCounter(csScenario,(System.nanoTime()-initTime)/1000);
     }     
  }
  
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////ORIGINATING       								//////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
  
  
  /**
   * This method processes MO traffic: MO_NO_RR HEC scenario
   * 
   * @param event
   * @param aci
   */
  private HECLogic.CSScenario processMO (CCInitialDPRequestEvent event, SccpAddress sccpCgPN, ActivityContextInterface aci)
  {
	trace(Level.INFO, "Processing MO scenario");
	try
	{		
	  	CCDialog dialog = event.getDialog();
	  	CCInitialDPArg idpArg = event.getArgument();
	  	// This is for the change request:
	  	//  	CAP2InitialDPArg argCap2 = (CAP2InitialDPArg)event.getArgument();
	  	
	  	CAP1InitialDPArg argCap1 = null;
	  	if(idpArg instanceof CAP1InitialDPArg)
	  	{
	  		argCap1 = (CAP1InitialDPArg)idpArg;
	  	}
	    CallingPartyNumber cgPN = idpArg.getCallingPartyNumber();
	    

	    CalledPartyBCDNumber cdPBCDN = argCap1 != null?argCap1.getCalledPartyBCDNumber():null;
	    //This is not the CdPN in MO according to CAP2. But some MSC might use it 
	    // and we'll give support for that.
		CalledPartyNumber cdPN = idpArg.getCalledPartyNumber(); 
	    
		
	    if(infoEnabled()) trace(Level.INFO,"Number [",cgPN,"] calling to [",cdPBCDN != null? cdPBCDN:cdPN,']');
	    if(infoEnabled()) trace(Level.INFO,"SCCP CgPN: "+sccpCgPN);
	    
		// Check there is a CgPN
	  	if(cgPN == null || !cgPN.hasNature())
	  	{
	  		trace(Level.ERROR, "Missing mandatory IDP param: ",cgPN);
	  		releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	  		statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid);
	  		return CSScenario.ERROR_NO_SCENARIO;
	  	}		
	  	
	  	// APRI analysis
	  	boolean apriRestricted = false;
		if(cgPN.hasPresentation())
		{			
			apriRestricted = cgPN.getPresentation() == CallingPartyNumber.Presentation.RESTRICTED ||
								cgPN.getPresentation() == CallingPartyNumber.Presentation.NETWORK_RESTRICTED;
			if(apriRestricted && dbgEnabled())trace(Level.DEBUG,"CallingPN presentation "+cgPN.getPresentation());
		}
		else
		{
			if(dbgEnabled())trace(Level.DEBUG,"Could not find any APRI info in the CgPN. Unrestricted by default");
		}
		
	    //ETSI TS 101 441 V7.4.0:
	    //Information element name 		MO MF MT Description
	    //Location Number 				-  -  C See GSM 03.18 [3].
	    //CellIdOrLAI 					M  -  C See GSM 03.18 [3].
	    //Geographical Information 		C  -  C See GSM 03.18 [3].
	    //Age Of Location Information 	M  -  C See GSM 03.18 [3].
	    //VLR number 					M  -  C See GSM 03.18 [3].    
		String vlrAddressString = null;
		MAPLocationInformation locationN = argCap1 != null?argCap1.getLocationInformation():null;
		if(locationN != null)
		{
			AddressString vlrAddress = locationN.getVlr_number();
			vlrAddressString = vlrAddress != null?
					vlrAddress.getAddress(bcdStringCodecAS)
					:null;
			if(dbgEnabled())trace(Level.DEBUG,"VLR info retrieved: ", vlrAddress);
		}
		
	  	//ETSI TS 101 441 V7.4.0:
		//For MO calls, the MSC Address carries the international E.164
		//address of the serving VMSC.
		//For MT calls, the MSC Address Address carries the international
		//E.164 address of the GMSC.
	    // In both cases we are supposing noai = International
	  	AddressString mscAddress = argCap1 != null?argCap1.getMscAddress():null;	  		  
	  	if(dbgEnabled())trace(Level.DEBUG,"MSC info retrieved: ", mscAddress);
        // This msc address string probably won't contain IAC (actually, this logic relies upon this premise)
	  	String mscAddressString = mscAddress != null?
	  			mscAddress.getAddress(bcdStringCodecAS)
	  			:null;
	  		
	  	//For CS1: this should not be null, but...
		String sccpCgPNStr = sccpCgPN != null? sccpCgPN.getAddress():null; 
	  	if (mscAddressString == null && vlrAddressString == null && sccpCgPNStr == null)
	  	{
	  		trace(Level.ERROR, "Missing mandatory IDP params (MSC/VLR) or SCCPCgPN: ",mscAddress+" / "+locationN+" / "+sccpCgPN);
	  		releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	  		statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid);
	  		return null;	  			  		
	  	}
	  			
	  	//From now on, we'll use only one, with preference for MSC	  	
	  	mscAddressString = mscAddressString != null? 
	  			mscAddressString
	  			:(vlrAddressString != null? vlrAddressString:sccpCgPNStr);
	  	if(dbgEnabled())trace(Level.INFO,"Using as location reference: "+mscAddressString);
	  	 
		int originalCdPNNOAI = -1;
		String originalCdPN = null;	  	
	  	//We will use CdPBCDN if available, otherwise CdPN
	  	if(cdPBCDN == null || !cdPBCDN.hasAddress() || !cdPBCDN.hasNumberType())
	  	{
	  		trace(Level.INFO, "Missing IDP param, CalledPartyBCDNumber: ",cdPBCDN);
	  		if(cdPN == null || !cdPN.hasNature() || !cdPN.hasAddress())
	  		{
	  			trace(Level.ERROR, "Not even a CdPN in this IDP. Releasing: ",cdPN);
	  			releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	  			statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid);
	  			incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
	  			return null;
	  		}
	  		else
	  		{
	  			trace(Level.INFO, "Using CalledPartyNumber instead of CdPBCDN");
		  		originalCdPN = cdPN.getAddress(bcdStringCodecCdPN);
		  		originalCdPNNOAI = cdPN.getNature().intValue();	  
		  		statsHelper.incCounter(Constants.ct_CS_MO_No_BCD);
	  		}
	  	}
	  	else
	  	{
	  		originalCdPN = cdPBCDN.getAddress(bcdStringCodecCdPBCDN);
	  		originalCdPNNOAI = HECLogic.convertBCDNT2NOAI(cdPBCDN.getNumberType().ordinal());
	  	}
	  	//No more validations for the IDP message:
	  	statsHelper.incCounter(Constants.ct_CS_IDPS_OK);
		    ////////////////////////
		    // NETWORK VALIDATION //  Disabled
		    ////////////////////////   
			// 
			//    boolean validated = HECLogic.validateNetwork(mscAddress.getAddress(), confHelper, statsHelper);
			//    boolean exception = false;
			//    if(!validated || exception)
			//    {
			//    	releaseCall(RC_NO_NETWORK_VALIDATION, dialog, aci);
			//    	return;
			//    }
	  	
	    //////////////////////////////////////////
	  	// Check fo UCM_OUT scenario: bug 13156 //
	    //////////////////////////////////////////
	  	HECLogic.CSTriggerValidation csTriggerValidation = HECLogic.validateCS_MO_Scenario (originalCdPN, originalCdPNNOAI, confHelper, statsHelper); 
	  	if (csTriggerValidation != null && csTriggerValidation.scenario != null && csTriggerValidation.scenario.equals(HECLogic.CSScenario.UCM_OUT_ONNET_NO_INTERCONN))
	  	{
	  		//do the UCM_OUT stuff
	  		processUCM_OUT_ONNET_NO_INTERCON(csTriggerValidation, event, aci);
	  		return HECLogic.CSScenario.UCM_OUT_ONNET_NO_INTERCONN;
	  	}

  		// ELSE: do the regular mo stuff
	  	statsHelper.incCounter(Constants.ct_CS_MO_NO_RR);
	  	
	  	//////////////////////// 
	    // A-PARTY VALIDATION //
	    ////////////////////////
	  	// This calling party string contains probably an IAC....
	  	//	  	String cgPNString =
	  	CSMONormalizedANumber normalizedA = HECLogic.getCSMONormalizedANumber(cgPN.getAddress(bcdStringCodecCgPN), 
	  			cgPN.getNature().intValue(), mscAddressString, 
	  			confHelper, statsHelper);
	  	if(normalizedA == null)
	  	{
	  		if(infoEnabled())trace(Level.INFO,"Could not normalize A number "+cgPN);	  		
	  		releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	  		incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
	  		return CSScenario.MO_NO_RR;
	  	}
	  	
	  	
	    // GET 'A' NUMBER ROAMING INFO //
	    RoamingInfo roamingInfo = null;
	    boolean exception = false;
	    try
	    {
	    	//TODO maybe this should include mscAddress and the whole CgPN
	    	roamingInfo =  HECLogic.getRoamingInfo(normalizedA.normalizedANumber, mscAddressString);    	     
	    }
	    catch(Exception e)
	    {
	    	exception = true;
	    	e.printStackTrace();
	    }    
	    if(roamingInfo == null || exception)
	    {
	    	releaseCall(RC_NO_ROAMING_INFO, dialog, aci);    	
	    	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);	    	
	    	return CSScenario.MO_NO_RR;
	    }
	    if(infoEnabled())trace(Level.INFO,"Obtained roamingInfo:",roamingInfo);   
	    if(roamingInfo.isInRoaming)statsHelper.incCounter(Constants.ct_CS_MO_Roaming);
	    
	//    Hec_GroupVO hecGroupA = null;
	    Hec_ExtensionInfoVO extInfoA = null;
	    exception = false;
	    try
	    {
	    	//TODO maybe this should include mscAddress and the whole CgPN
	    	extInfoA = HECLogic.validateCSMO_A_MSISDN(normalizedA.normalizedANumber, roamingInfo);    	     
	    }
	    catch(Exception e)
	    {
	    	exception = true;
	    	e.printStackTrace();
	    }    
	    if(extInfoA == null || exception)
	    {
	    	releaseCall(RC_NO_A_VALIDATION, dialog, aci);    	
	    	return CSScenario.MO_NO_RR;
	    }    
	    if(infoEnabled())trace(Level.INFO,"#A validated. Group obtained:",extInfoA.getGroupName());//TODO a�adir PABXID
	    
	    
	    // NORMALIZE B NUMBER //
	    CSNormalizedBNumber normalizedB = null;
	    exception = false;
	    try
	    {    	
	    	normalizedB =  
	    		HECLogic.csNormalizeBNumber(originalCdPN, 
	    				originalCdPNNOAI, 
	    				roamingInfo, 
	    				extInfoA, 
	    				confHelper, statsHelper);   	     	    	
	    }
	    catch(Exception e)
	    {
	    	exception = true;
	    	e.printStackTrace();
	    }
	    // CR005: Continue when unsupported NOAI
	    if(normalizedB == null)
	    {
	    	continueCall(dialog, aci);
	    	return CSScenario.MO_NO_RR;
	    }
	    else if (exception)
	    {
	    	releaseCall(RC_CANNOT_NORMALIZE_B, dialog, aci);	    	
	    	return CSScenario.MO_NO_RR;
	    }	   
	    if(infoEnabled())trace(Level.INFO,"Obtained normalized B number:",normalizedB);    
	    

	    //1 - special -> reroute to routing number
	    //2 - PublicNumberNAT/INT -> check rerouting flag and roaming info, then:
	    //			2.1 Connect to the PN or
	    //			2.2 Create CorrelationId and connect to the encoded DRA
	    switch(normalizedB.bNumberType)
	    {
	    	//This only happens in roaming. B-party normalization ensures no SD are detected in roaming
	    	//CR009: now, this can happen both in roaming and in home network.
		    case SpecialDestination:		    		    	
		    	if(normalizedB.specialDestinationVO != null && normalizedB.specialDestinationVO.getSpecLongNumber() != null)
		    	{		    		
		    		if(infoEnabled())trace(Level.INFO,"Connecting Special destination back to CS. Using Long number");	
		    		//BUG 7883
		    		//		    		sendConnectMO(normalizedB.normalizedBNumber, HECLogic.cgPN_cdPN_noai_INT, cdPBCDN, dialog, aci);
		    		statsHelper.incCounter(Constants.ct_CS_MO_Rerouted_CS);
		    		sendConnectMO(normalizedB.specialDestinationVO.getSpecLongNumber(), HECLogic.cgPN_cdPN_noai_INT, dialog, aci);
		    	}
		    	else if(normalizedB.specialDestinationVO != null && normalizedB.specialDestinationVO.getNoaiIntFlag() == 1)
		    	{
		    		if(infoEnabled())trace(Level.INFO,"Connecting Special destination back to CS. Using NOAI international");
		    		//BUG 7883
		    		//		    		sendConnectMO(normalizedB.originalBNumber, HECLogic.cgPN_cdPN_noai_INT, cdPBCDN, dialog, aci);
		    		statsHelper.incCounter(Constants.ct_CS_MO_Rerouted_CS);
		    		// BUG 11338
		    		//		    		sendConnectMO(normalizedB.normalizedBNumber, HECLogic.cgPN_cdPN_noai_INT, originalCdPN, originalCdPNNOAI, dialog, aci);
		    		sendConnectMO(normalizedB.normalizedBNumber, HECLogic.cgPN_cdPN_noai_INT, dialog, aci);		    				    
		    	}
		    	else
		    	{
		    		if(infoEnabled())trace(Level.INFO,"Connecting Special destination back to CS. Using NOAI unknown and national format");
		    		statsHelper.incCounter(Constants.ct_CS_MO_Rerouted_CS);
		    		// BUG 11383
		    		String specialNumberNationalformat = HECLogic.stripCountryCode(normalizedB.normalizedBNumber);
		    		sendConnectMO(specialNumberNationalformat, HECLogic.cgPN_cdPN_noai_UNK, dialog, aci);
		    	}
		    	break;
		    case PNPShortNumber:
		    case NonDecimalNumber:
		    	//BUG 7787
		    	////////////////////////////////////
		        // Central IMS rerouting Function // 
		        ////////////////////////////////////
		    	if(infoEnabled())trace(Level.INFO,"Routing PNP short number/Non-decimal digits address to IMS");
		    	// BUG 11332
		    	//		        moCentralIMSRerouting(dialog, aci, normalizedA, originalCdPN, originalCdPNNOAI, normalizedB, roamingInfo, mscAddressString);
		    	moCentralIMSRerouting(dialog, aci, normalizedA, originalCdPN, HECLogic.cgPN_cdPN_noai_UNK, normalizedB, roamingInfo, mscAddressString, apriRestricted);
		    	break;
		    case PublicNumberINT:
		    case PublicNumberNAT:
		    	//BUG 7787		    	
		    	if(!extInfoA.isReroutingFlag() /*BUG 7866: && roamingInfo.isInRoaming*/)
		    	{
		    		//		    		if(infoEnabled())trace(Level.INFO,"Routing Public Number in roaming for VPN with no rerouting flag back to CS");
		    		if(infoEnabled())trace(Level.INFO,"Routing Public Number for VPN with no rerouting flag back to CS");
		    		statsHelper.incCounter(Constants.ct_CS_MO_Rerouted_CS);
		    		// BUG 11338
		    		//		    		sendConnectMO(normalizedB.originalBNumber, HECLogic.cgPN_cdPN_noai_UNK, originalCdPN, originalCdPNNOAI, dialog, aci);  		
		    		// BUG 11437
		    		// sendConnectMO(normalizedB.originalBNumber, HECLogic.cgPN_cdPN_noai_UNK, dialog, aci);
		    		sendConnectMO(normalizedB.normalizedBNumber, HECLogic.cgPN_cdPN_noai_INT, dialog, aci);
		    	}
		    	else
		    	{
		    		if(infoEnabled())trace(Level.INFO,"Routing Public Number to IMS");
		    		////////////////////////////////////
			        // Central IMS rerouting Function // 
			        ////////////////////////////////////
		    		// BUG 11332
		    		//			        moCentralIMSRerouting(dialog, aci, normalizedA, originalCdPN, originalCdPNNOAI, normalizedB, roamingInfo, mscAddressString);	  		    		
			        moCentralIMSRerouting(dialog, aci, normalizedA, normalizedB.normalizedBNumber, HECLogic.cgPN_cdPN_noai_INT, normalizedB, roamingInfo, mscAddressString, apriRestricted);
		    	}	
		    	break;
		    default: //This shoudn't ever happen 
		    	if(infoEnabled())trace(Level.INFO,"Unsupported B number type: "+normalizedB);
		    	releaseCall(RC_CANNOT_NORMALIZE_B, dialog, aci); 
		    	incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
		    	return CSScenario.MO_NO_RR;
	    }	    
	}
	catch(Exception e)
	{
		e.printStackTrace();
		trace(Level.ERROR, "Exception processing MO scenario: ",e);
		incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
		return CSScenario.ERROR_NO_SCENARIO;
	} 
	return CSScenario.MO_NO_RR;
  }
    
//  private void moCentralIMSRerouting(CCDialog dialog, ActivityContextInterface aci, 
//		  CSMONormalizedANumber normalizedA, CalledPartyBCDNumber cdPBCDN,
//		  CSNormalizedBNumber normalizedB, RoamingInfo roamingInfo, String mscAddrString)
  private void moCentralIMSRerouting(CCDialog dialog, ActivityContextInterface aci, 
		  CSMONormalizedANumber normalizedA, String originalCdPN, int originalCdPNNOAI,
		  CSNormalizedBNumber normalizedB, RoamingInfo roamingInfo, String mscAddrString,
		  boolean apriRestricted)  
  {
  
	  	statsHelper.incCounter(Constants.ct_CS_MO_Rerouted_IMS);
	  	boolean exception = false;
	    MO_NO_RR_Context context = new MO_NO_RR_Context();
	    context.setInSessionId(dialog.getDialogID());
	    //	    context.setCallingPartyNumber(cgPN != null? cgPN.getAddress(bcdStringCodecCgPN):"null");
	    context.setCallingPartyNumber(normalizedA.normalizedANumber);
	    context.setCallingAPRIRestricted(apriRestricted);
	    
	    
	    //Normalized or original?
	    //    context.setCalledPartyNumber(cdPBCDN.getAddress(bcdStringCodecCdPBCDN));
	    context.setCalledPartyNumber(normalizedB.normalizedBNumber);
	    context.setCalledPartyType(normalizedB.bNumberType);
	    
	    // This info is intended for displaying purposes only (CDR).  We've been requested to use IAC+CC+Nat# format, so:
	    try
	    {
	    	//	    	String formattedMscAddress = roamingInfo.visitingCountry.getInternationalCodes()[0]+mscAddress.getAddress(bcdStringCodecAS); 
	    	String formattedMscAddress = roamingInfo.mscCountry.getInternationalCodes()[0]+mscAddrString;
	    	context.setMSCAddress(formattedMscAddress);
	    }
	    catch(Exception e)
	    {
	    	//no default value by the moment
	    	trace(Level.WARN,"There was a problem formatting msc addres. ");
	    	e.printStackTrace();
	    }
	        
	    if(infoEnabled())trace(Level.INFO,"Context object: {",context,"}. Writting in Correlation-RA...");             	
	    // 
	    // Store the HEC-Number in the "Correlation-RA" which is used as on-fly database
	    //
	    String correlationId = null;
	    Hec_OpcoVO opCoVO = normalizedA.opco;
	    try 
	    { 
	        //For CorrelationRA
//	    	correlationId = HECLogic.recordCorrelationContext(opCoVO.getOpCoId().toString(),   // Possible bug: it's a db table key
	    	correlationId = HECLogic.recordCorrelationContext(opCoVO.getCountryCode().toString(),
	    			HECContextFactory.getSerializedPayload(context),correlationRaActivityProvider);
	    	statsHelper.incCounter(Constants.ct_CS_Correlation_Recorded);
	    }
	    catch(Exception e)
	    {
	      exception = true;
	      e.printStackTrace();
	      incrementCounter(HECUsageCounter.CORRELATION_ERROR);
	      statsHelper.incCounter(Constants.ct_CS_Correlation_Recording_Error);
	    }
	    if(opCoVO == null || correlationId == null || exception)
	    {    	    	
	        releaseCall(RC_NO_CORRELATION_AVAILABLE, dialog, aci);
	        return;
	    }      
		if(infoEnabled())trace(Level.INFO, "Obtained correlationId: "+correlationId);
		
		//
		// Obtain OPCo prefix
		//
		String opCoPrefix = null;
		try
		{
			// Modified for CR030
//			  Integer configuredPop = confHelper.getInt(HECLogic.HEC_LOCAL_POP_PARAMETER);
//			  if(configuredPop == null)
//			  {  	   
//			        releaseCall(RC_NO_RR_PREFIX_AVAILABLE, dialog, aci);
//			        return;				        
//			  }
//			  short localPOPIndex = (short)((configuredPop-1)%2);
		    //String encoded_dra = HECLogic.getEncodedDra(triggerScenario, cdPN, correlationId );
			//opCoPrefix = +<CC> <Operator Code> <Prefix 1>		
			//opCoPrefix = HECLogic.getMOReroutingPrefix(cgPN.getAddress(bcdStringCodecCgPN), confHelper, statsHelper);
			opCoPrefix=opCoVO.getCountryCode()+opCoVO.getOperatorCode()+opCoVO.getPrefix1();
		}
		catch (Exception e)
		{
			exception = true;
			e.printStackTrace();
		}

		if(opCoPrefix == null || exception)
		{
		      releaseCall(RC_NO_RR_PREFIX_AVAILABLE, dialog, aci);
		      return;		
		}
		if(infoEnabled())trace(Level.INFO, "Obtained opCoPrefix: "+opCoPrefix);
		//
		// Encode DRA: 		
		// 3.1.6 DRA=+<CC> <Operator Code> <Prefix 1> <Correlation ID>    
		//
		String encodedDRA = opCoPrefix+correlationId; 
		if(infoEnabled())trace(Level.INFO, "Encoded DRA: ", encodedDRA);	
		
	    //////////////////
	    // Send Connect // 
	    //////////////////	
//	    sendConnectMO(encodedDRA, HECLogic.cgPN_cdPN_noai_INT, cdPBCDN, dialog, aci);
		sendConnectMO(encodedDRA, HECLogic.cgPN_cdPN_noai_INT, originalCdPN, originalCdPNNOAI, dialog, aci);
	    if(infoEnabled())trace(Level.INFO, "Connect sent. Dra: "+encodedDRA);  
  }
  
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// TERMINATING       								//////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * This method processes MT traffic, 
   * corresponding to MT_NO_RR 
   * 
   * @param event
   * @param aci
   */
  private HECLogic.CSScenario processMT (CCInitialDPRequestEvent event, ActivityContextInterface aci)
  {
	  	trace(Level.INFO, "Processing MT scenario");
	  	CCDialog dialog = null;
	  	try
	  	{
	  		dialog = event.getDialog();
	  		CCInitialDPArg idpArg = event.getArgument();
//		  	CAP1InitialDPArg idpArg = (CAP1InitialDPArg)event.getArgument();
		  	//		    CallingPartyNumber cgPN = arg.getCallingPartyNumber();
		    CalledPartyNumber cdPN = idpArg.getCalledPartyNumber(); 
		  	if(cdPN == null )
		  	{
		  		trace(Level.ERROR, "Missing mandatory IDP param: CdPN");
		  		incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
			  	statsHelper.incCounter(Constants.ct_CS_IDPS_Invalid);
		  		releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
		  		return HECLogic.CSScenario.ERROR_NO_SCENARIO;
		  	}
		  	//No more validations for the IDP message:
		  	statsHelper.incCounter(Constants.ct_CS_IDPS_OK);
		  	       
			CallingPartyNumber cgPN = idpArg.getCallingPartyNumber();
		    if(infoEnabled()) trace(Level.INFO,"Number [",cgPN,"] calling to [",cdPN,']');
  	
		    /////////////////////////////
		    // SCENARIO IDENTIFICATION //
		    /////////////////////////////
		  	String cdPNAddress = cdPN.getAddress(bcdStringCodecCdPN);
		  	int noai = cdPN.getNature().intValue();
		  	CSTriggerValidation csTriggerValidation = HECLogic.validateCS_MT_Scenario(cdPNAddress, noai,  confHelper, statsHelper);
		  	//BUG 13156: this is a MO scenario
		  	/*if (csTriggerValidation != null && csTriggerValidation.scenario == CSScenario.UCM_OUT_ONNET_NO_INTERCONN)
		  	{
		  		//UCM_OUT_ONNET_NO_INTERCONN
		  		processUCM_OUT_ONNET_NO_INTERCON(csTriggerValidation, event, aci);
		  		return HECLogic.CSScenario.UCM_OUT_ONNET_NO_INTERCONN;
		  	}
		  	else*/ if(csTriggerValidation != null && csTriggerValidation.scenario == CSScenario.MT_NO_RR)
		  	{
		  		processMT_NO_RR(csTriggerValidation, event, aci);
		  		return HECLogic.CSScenario.MT_NO_RR;
		  	}
		  	//BUG 12060
		  	else if(csTriggerValidation != null && csTriggerValidation.scenario == CSScenario.UNSUPPORTED_NOAI)
		  	{
		  		trace(Level.WARN, "Could not process CdPN NOAI. Continuing call.");
		  		incrementCounter(HECUsageCounter.UNKNOWN_CALL_SCENARIO);
		  		continueCall(dialog, aci);
		  		return HECLogic.CSScenario.UNSUPPORTED_NOAI; 
		  	}		  	
		  	else
		  	{
		  		trace(Level.WARN, "Could not identify terminating scenario. Releasing call. "+csTriggerValidation);
		  		incrementCounter(HECUsageCounter.UNKNOWN_CALL_SCENARIO);
		  		releaseCall(RC_CANNOT_NORMALIZE_B, dialog, aci);
		  		return HECLogic.CSScenario.ERROR_NO_SCENARIO;
		  	}
	  	}
	  	catch(Exception e)
	  	{
	  		e.printStackTrace();
	  		trace(Level.ERROR," Exception processing MT scenario: ",e);
	  		incrementCounter(HECUsageCounter.UNKNOWN_CALL_SCENARIO);
	  		releaseCall(RC_INTERNAL_ERROR, dialog, aci);
	  		return HECLogic.CSScenario.ERROR_NO_SCENARIO;
	  	}
  }
  
  
  /**
   * 
   * @param event
   * @param aci
   */
  private void processUCM_OUT_ONNET_NO_INTERCON (CSTriggerValidation csTriggerValidation, CCInitialDPRequestEvent event, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "UCM originating scenario detected. ");
	  statsHelper.incCounter(Constants.ct_CS_MT_UCM_OUT);
	  
	  CCDialog dialog = event.getDialog();
	  //	  CAP2InitialDPArg arg = (CAP2InitialDPArg)event.getArgument();
	  //	  CallingPartyNumber cgPN = arg.getCallingPartyNumber();
	  //	  CalledPartyNumber cdPN = arg.getCalledPartyNumber(); 	  
	  
	     ////////////////////////
	    // Trigger VALIDATION //
	   //////////////////////// 
	  //Already checked:
	  if(csTriggerValidation.payload == null)
	  {
		  if(infoEnabled())trace(Level.INFO,"Could not validate get correlationId from cdPN. Releasing call");
		  incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
		  releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
		  return;
	  }
	  
//	  UCM_OUT_NO_INTERCONN_Context storedContextInfo = retrieveRecordedNumber(csTriggerValidation.opCo.getOpCoId().toString(), // Possible bug: it's a db table key
	  UCM_OUT_NO_INTERCONN_Context storedContextInfo = retrieveRecordedNumber(csTriggerValidation.opCo.getCountryCode().toString(),
			  																	csTriggerValidation.payload);
	  if(storedContextInfo == null)
	  {
		  if(infoEnabled())trace(Level.INFO,"Could not retrieve call context. Releasing");
		  releaseCall(RC_NO_CORRELATION_AVAILABLE, dialog, aci);
		  return;
	  }
	  if(infoEnabled())trace(Level.INFO,"Call context found: "+storedContextInfo.toString());
	  
	  //<CC><IN inh prefix><numB NAT>
	  String dra = csTriggerValidation.opCo.getCountryCode()+
	  				csTriggerValidation.opCo.getIn_prefix()+
	  				storedContextInfo.getCalledPartyNumberNATFormat();
	  String originalBIntFormat = csTriggerValidation.opCo.getCountryCode()+storedContextInfo.getCalledPartyNumberNATFormat();
	  
	  
	  //BUG 11608
	  //Restore the original B party
	  //	  cdPN.setAddress(originalB,bcdStringCodecCdPN);	  
	  OriginalCalledNumber originalCdPN = new OriginalCalledNumber();		    
	  originalCdPN.setAddress(originalBIntFormat, bcdStringCodecOCdPN);
	  originalCdPN.setNature(OriginalCalledNumber.Nature.INTERNATIONAL);
	  originalCdPN.setNumberingPlan(OriginalCalledNumber.NumberingPlan.ISDN);
	  originalCdPN.setPresentation(OriginalCalledNumber.Presentation.ALLOWED);	  
	  
	  statsHelper.incCounter(Constants.ct_CS_MT_Rerouted_CS);
	  sendConnectMT_UCM_OUT(dra, originalCdPN, dialog, aci);	  	  
  }
  
  /**
   * 
   * @param event
   * @param aci
   */
  private void processMT_NO_RR (CSTriggerValidation csTriggerValidation, CCInitialDPRequestEvent event, ActivityContextInterface aci)
  {
	  	if(infoEnabled())trace(Level.INFO, "CS originating MT scenario detected.");
	  	statsHelper.incCounter(Constants.ct_CS_MT_NO_RR);
	  	
	  	CCDialog dialog = event.getDialog();
	  	//	  	CAP1InitialDPArg arg = (CAP1InitialDPArg)event.getArgument();
	  	//	    CallingPartyNumber cgPN = arg.getCallingPartyNumber();
	  	//	    CalledPartyNumber originalCdPN = arg.getCalledPartyNumber(); 
	    
	  	//ETSI TS 101 441 V7.4.0:
		//For MO calls, the MSC Address carries the international E.164
		//address of the serving VMSC.
		//For MT calls, the MSC Address Address carries the international
		//E.164 address of the GMSC.
	    //	  	AddressString mscAddress = arg.getMscAddress();
	  	
	      /////////////////////
	     /// #B VALIDATION ///
	    /////////////////////   
	    // GET 'A' NUMBER ROAMING INFO:
	  	// This is to be able to strip IAC or NAC from BNumber. What if there is no CgPN? 
		//	    RoamingInfo roamingInfo = null;
		//	    boolean exception = false;
		//	    try
		//	    {
		//	    	roamingInfo =  HECLogic.getRoamingInfo(cgPN != null? cgPN.getAddress(bcdStringCodecCgPN):null, 
		//	    			mscAddress.getAddress(bcdStringCodecAS));    	     
		//	    }
		//	    catch(Exception e)
		//	    {
		//	    	exception = true;
		//	    	e.printStackTrace();
		//	    }    
		//	    if(roamingInfo == null || exception)
		//	    {
		//	    	releaseCall(RC_NO_ROAMING_INFO, dialog, aci);    	
		//	    	return;
		//	    }
    	String numBIntFormat = csTriggerValidation.opCo.getCountryCode()+csTriggerValidation.payload;	  
    	
    	// BUG 11597 (enhancement)
    	//	    Hec_GroupVO group = null;
    	Hec_ExtensionInfoVO extInfo = null;
	    boolean exception = false;
	    try
	    {
	    	//No need to format anything?
	    	//	    	group = HECLogic.validateHEC_MSISDN(numBIntFormat/*, roamingInfo*/);    	
	    	extInfo = HECLogic.validateHECMobileExtension(numBIntFormat);	    	
	    }
	    catch(Exception e)
	    {
	    	exception = true;
	    	e.printStackTrace();
	    }    
	    //	    if(group == null || exception)
	    if(extInfo == null || exception)
	    {
	    	releaseCall(RC_NO_B_VALIDATION, dialog, aci);    	
	    	return;
	    }    
	    //	    if(infoEnabled())trace(Level.INFO,"#B validated. Group obtained:",group.getGroupId());
	    
	    // BUG 11608
	    OriginalCalledNumber originalCdPN = new OriginalCalledNumber();		    
	    originalCdPN.setAddress(numBIntFormat, bcdStringCodecOCdPN);
	    originalCdPN.setNature(OriginalCalledNumber.Nature.INTERNATIONAL);
	    originalCdPN.setNumberingPlan(OriginalCalledNumber.NumberingPlan.ISDN);
	    originalCdPN.setPresentation(OriginalCalledNumber.Presentation.ALLOWED);
	    
	    ///////////////////
	    // IMS REROUTING //
	    ///////////////////	      	
//	    Hec_OpcoVO opCo = HECLogic.getSubscriberOpco(cdPN.getAddress(bcdStringCodecCdPN),confHelper, statsHelper);
	    
		//
		// Obtain DRA: CC<Nat#>
		//
	    
		String destinationRoutingAddress = null;
		try
		{
			//opCoPrefix = +<CC> <Prefix 3>	<numBNational>	
//			opCoPrefix = HECLogic.getMTReroutingPrefix(cdPN.getAddress(bcdStringCodecCdPN), confHelper, statsHelper);
			//BUG 14704
//			short primaryPOPId = csTriggerValidation.opCo.getPrimaryPOP();
//			short primaryPOPIndex = (short)((primaryPOPId-1)%2);				
			destinationRoutingAddress = csTriggerValidation.opCo.getCountryCode()+csTriggerValidation.opCo.getPrefix3()+csTriggerValidation.payload;
		}
		catch (Exception e)
		{
			exception = true;
			e.printStackTrace();
		}

		if(destinationRoutingAddress == null || exception)
		{
		      releaseCall(RC_NO_RR_PREFIX_AVAILABLE, dialog, aci);
		      return;		
		}
		if(infoEnabled())trace(Level.INFO, "Obtained opCoPrefix: "+destinationRoutingAddress);	    

		//////////////////
	    // Send Connect // 
	    //////////////////
		statsHelper.incCounter(Constants.ct_CS_MT_Rerouted_IMS);
	    sendConnectMT_UCM_OUT(destinationRoutingAddress, originalCdPN, dialog, aci);	
	    
  }
  
  
  
  /**
   * Invoked by MT(CS_NO_RR) and MO (UCM_OUT) scenario processing methods
   * 
   * @param draStr International format CC+Nat#
   * @param oCdN
   * @param dialog
   * @param aci
   */
  private void sendConnectMT_UCM_OUT(String draStr, OriginalCalledNumber oCdN , CCDialog dialog, ActivityContextInterface aci)
  {
	    try 
	    {	
			CalledPartyNumber[] draList = new CalledPartyNumber[1];		
		
			draList[0] = new CalledPartyNumber();
			draList[0].setAddress(draStr,bcdStringCodecCdPN);
			draList[0].setNature(CalledPartyNumber.Nature.INTERNATIONAL); 
			draList[0].setNumberingPlan(CalledPartyNumber.NumberingPlan.ISDN); 
			draList[0].setRoutingToInternalNetworkNumber(CalledPartyNumber.RoutingToInternalNetworkNumber.ALLOWED);        
			if(infoEnabled())trace(Level.INFO,"Sending Connect. DRA: ",draList[0]);
 
			// BUG 11608
		    //No need to send this data:
			//		    OriginalCalledNumber oCdN = new OriginalCalledNumber();
			//		    oCdN.setNature(OriginalCalledNumber.Nature.fromValue(originalCdPN.getNature().intValue()));
			//		    oCdN.setNumberingPlan(OriginalCalledNumber.NumberingPlan.values()[originalCdPN.getNumberingPlan().intValue()]);
			//		    oCdN.setAddress(originalCdPN.getAddress(bcdStringCodecCdPN));
			//		    oCdN.setPresentation(OriginalCalledNumber.Presentation.ALLOWED);		    
		    if(infoEnabled())trace(Level.INFO,"Sending Connect. OCdPN: ",oCdN);
		    
		    CCConnectArg connect = new CCConnectArg();
		    connect.setDestinationRoutingAddress(draList);
		    connect.setOriginalCalledPartyID(oCdN);
		    
		    int invokeID = dialog.sendConnect(connect);
		    dialog.sendClose(false);
		    statsHelper.incCounter(Constants.ct_CS_Connect);
		    if(infoEnabled())trace(Level.INFO,"Connect sent. InvokeID: ",invokeID);		    		    
	    }
	    catch (TooManyInvokesException e) 
	    {      
		    trace(Level.WARN, getExceptionMsg("TooManyInvokesException: ", e));
		    e.printStackTrace();
		    releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
		    return;
	    }
	    catch (ProtocolException e) 
	    {      
		    trace(Level.WARN, getExceptionMsg("ProtocolException: ", e));
		    e.printStackTrace();
		    releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
		    return;
	    }
	    catch (Exception e)
	    {
	    	trace(Level.WARN, getExceptionMsg("Exception: ", e));
	    	e.printStackTrace();
	    	releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	    	return;
	    }
	    finally
	    {	      
	      aci.detach(this.getSbbLocalObject());
	      if(infoEnabled())trace(Level.INFO,"Detached from activity");
	    }	
  }
  

  /**
   * OriginalCdPN not applicable
   * 
   * @param draStr International format CC+Nat#
   * @param dialog
   * @param aci
   */
  private void sendConnectMO(String draStr, int draNoai, CCDialog dialog, ActivityContextInterface aci)
  {
	  sendConnectMO(draStr, draNoai, null, -1, dialog, aci);
  }
  /**
   * 
   * @param draStr International format CC+Nat#
   * @param originalCdPN
   * @param dialog
   * @param aci
   */
//  private void sendConnectMO(String draStr, int draNoai, CalledPartyBCDNumber originalCdPN , CCDialog dialog, ActivityContextInterface aci)
  private void sendConnectMO(String draStr, int draNoai, String originalCdPN, int originalCdPNNOAI , CCDialog dialog, ActivityContextInterface aci)
  {
	    try 
	    {		    	
	    	
			CalledPartyNumber[] draList = new CalledPartyNumber[1];		
		
			draList[0] = new CalledPartyNumber();
			draList[0].setAddress(draStr,bcdStringCodecCdPN);
			draList[0].setNature(CalledPartyNumber.Nature.fromValue(draNoai)); 
			draList[0].setNumberingPlan(CalledPartyNumber.NumberingPlan.ISDN); //Hope this works...
			draList[0].setRoutingToInternalNetworkNumber(CalledPartyNumber.RoutingToInternalNetworkNumber.ALLOWED); //Hope this works...       
			if(infoEnabled())trace(Level.INFO,"Sending Connect. DRA: ",draList[0]);
				    					
		    CCConnectArg connect = new CCConnectArg();
		    connect.setDestinationRoutingAddress(draList);		    
			
			if (originalCdPN != null)
			{
			    //No need to send this data:
			    OriginalCalledNumber oCdN = new OriginalCalledNumber();
	
			    //BCD NT mapping to Cg/Cd/PN NOAI
//			    if(originalCdPN.hasNumberType())
//			    {
//			    	int mappedNTValue = -1;
//			    	HECLogic.convertBCDNT2NOAI(originalCdPN.getNumberType().ordinal()
//			    	switch(originalCdPN.getNumberType().ordinal())
//			    	{
//			    		case HECLogic.nt_UNK: 	mappedNTValue = HECLogic.cgPN_cdPN_noai_UNK;
//			    								break;
//			    		case HECLogic.nt_NAT: 	mappedNTValue = HECLogic.cgPN_cdPN_noai_NAT;
//			    								break;
//			    		case HECLogic.nt_INT: 	mappedNTValue = HECLogic.cgPN_cdPN_noai_INT;
//			    								break;
//			    		default:				mappedNTValue = HECLogic.cgPN_cdPN_noai_UNK;
//			    	}
//			    	oCdN.setNature(OriginalCalledNumber.Nature.fromValue(mappedNTValue));
//			    	
//			    }
//			    else
//			    {
//			    	//Not very common, we expect
//			    	oCdN.setNature(OriginalCalledNumber.Nature.UNKNOWN);
//			    }
			    
			    oCdN.setNature(OriginalCalledNumber.Nature.fromValue(originalCdPNNOAI));
			    
			    //Mapping: 	For NP values up to 7, map directly. 
			    //			For higher values (national, private and reserved_x), map to ISDN by default.    
//			    oCdN.setNumberingPlan(OriginalCalledNumber.NumberingPlan.values()[
//			        originalCdPN.hasNumberingPlan() && originalCdPN.getNumberingPlan().ordinal()
//			        < OriginalCalledNumber.NumberingPlan.values().length ? 
//			        		originalCdPN.getNumberingPlan().ordinal():
//			        		OriginalCalledNumber.NumberingPlan.ISDN.intValue()]);
			    oCdN.setNumberingPlan(OriginalCalledNumber.NumberingPlan.ISDN);
			    
//			    oCdN.setAddress(originalCdPN.getAddress(bcdStringCodecCdPBCDN));
			    oCdN.setAddress(originalCdPN,bcdStringCodecOCdPN);
			    
			    oCdN.setPresentation(OriginalCalledNumber.Presentation.ALLOWED);
			    if(infoEnabled())trace(Level.INFO,"Sending Connect. OCdPN: ",oCdN);			    
			    connect.setOriginalCalledPartyID(oCdN);
			}
		    		    
		    int invokeID = dialog.sendConnect(connect);
		    dialog.sendClose(false); 
		    statsHelper.incCounter(Constants.ct_CS_Connect);
		    if(infoEnabled())trace(Level.INFO,"Connect sent. Invoke ID: ",invokeID);		    
	    }
	    catch (TooManyInvokesException e) 
	    {      
		    trace(Level.WARN, getExceptionMsg("TooManyInvokesException: ", e));
		    e.printStackTrace();
		    releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
		    return;
	    }
	    catch (ProtocolException e) 
	    {      
		    trace(Level.WARN, getExceptionMsg("ProtocolException: ", e));
		    e.printStackTrace();
		    releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
		    return;
	    }
	    catch (Exception e)
	    {
	    	trace(Level.WARN, getExceptionMsg("Exception: ", e));
	    	e.printStackTrace();
	    	releaseCall(RC_INCONSISTENT_IDP_DATA, dialog, aci);
	    	return;
	    }
	    finally
	    {
	      aci.detach(this.getSbbLocalObject());  
	      if(infoEnabled())trace(Level.INFO,"Detached from activity");	      
	    }	
  }  
  
  private void continueCall(CCDialog dialog, ActivityContextInterface aci)
  {
      if(infoEnabled())trace(Level.INFO,"Continuing Call ");
      try 
      {
        dialog.sendContinue();
        dialog.sendClose(false);
        statsHelper.incCounter(Constants.ct_CS_Continue);
      }
      catch (TooManyInvokesException e) 
      {
        String msg = this.getExceptionMsg("TooManyInvokesException: ", e);
        trace(Level.WARN, msg);
      }
      catch (ProtocolException e) 
      {
        String msg = this.getExceptionMsg("ProtocolException: ", e);
        trace(Level.WARN, msg);
      }
      finally
      {
        aci.detach(this.getSbbLocalObject());       
      }	  
  }
  /**
   * Tool function to map internal RCC to CAP RCC and send release
   * 
   * @param releaseCause
   * @param dialog
   * @param aci
   */
  private void releaseCall(int releaseCauseCode, CCDialog dialog, ActivityContextInterface aci)
  {
	  Cause ccReleaseCause=null;
	  try
	  {
		  
		  trace(Level.WARN, "Releasing call with cause Id ",releaseCauseCode);
		  ReleaseCauseVO rc = HECLogic.getCSReleaseCause(releaseCauseCode);
		  if(rc != null)
		  {
			  trace(Level.WARN, "Release cause found: ",rc);
			  int rCC = Integer.parseInt(rc.getCauseCode());
			  ccReleaseCause = new Cause(Cause.CauseValue.fromValue(rCC));			  
		  }
		  else
		  {
			  trace(Level.WARN, "Release cause Id not found: ",releaseCauseCode);
			  ccReleaseCause = new Cause(Cause.CauseValue.fromValue(RC_DEFAULT));
		  }
	  } 
	  catch (Exception e)
	  {
		trace(Level.WARN, "Exception trying to find release cause: ",releaseCauseCode);
		e.printStackTrace();
		ccReleaseCause = new Cause(Cause.CauseValue.fromValue(RC_DEFAULT));
	  }

//	  Cause.CauseValue ccReleaseCause = mapDBReleaseCauseCode(releaseCause);
//      Cause ccReleaseCause = new Cause(Cause.CauseValue.CLASS1_CALL_REJECTED);
      CCReleaseCallArg rcArg = new CCReleaseCallArg();
      rcArg.setInitialCallSegment(ccReleaseCause);
      if(infoEnabled())trace(Level.INFO,"Releasing CGIN Call Cause: "+ccReleaseCause);
      try 
      {
        dialog.sendReleaseCall(rcArg);
        dialog.sendClose(false);
        statsHelper.incCounter(Constants.ct_CS_Release);
      }
      catch (TooManyInvokesException e) 
      {
        String msg = this.getExceptionMsg("TooManyInvokesException: ", e);
        trace(Level.WARN, msg);
      }
      catch (ProtocolException e) 
      {
        String msg = this.getExceptionMsg("ProtocolException: ", e);
        trace(Level.WARN, msg);
      }
      finally
      {
        aci.detach(this.getSbbLocalObject());       
      }
  }

  /**
   * IES to accept CAPv2 and CAPv1
   */
  public InitialEventSelector initialEventSelect(InitialEventSelector ies)
  {		  
	
    if(infoEnabled())trace(Level.INFO,"Initial event selector. ");
    Object genericEvent = ies.getEvent();
    if(genericEvent instanceof DialogOpenRequestEvent)
    {
    	 
    	String adHocPreamble = ((DialogOpenRequestEvent)genericEvent).getDialog() != null? 
    			((DialogOpenRequestEvent)genericEvent).getDialog().getDialogID():
    			"NO PREABLE";
    	if(infoEnabled())trace(adHocPreamble, Level.INFO,"CS Event ");
	    DialogOpenRequestEvent ev = (DialogOpenRequestEvent) ies.getEvent();
	    if(infoEnabled())trace(adHocPreamble, Level.INFO,"  Event: ",ev);
	    TcapApplicationContext ac = ev.getApplicationContext();
	    // This one will go below:
	    //	    if(infoEnabled())trace(adHocPreamble, Level.INFO,"  Application context: ",ac);
	    
	    if (ac == null)
	    {
	    	trace(adHocPreamble, Level.ERROR,"  Null ACN - Invalid event "+ev);
	    	ies.setInitialEvent(false);
	    	return ies;
	    }
	   
	    // Cannot use validateACN(ac) because its traces use the common preamble
	    ///// CAP v2
	    if (ac.equals(CAP2ApplicationContexts.cap_v2_gsmSSF_to_gsmSCF_AC))
	    {
	      if(infoEnabled())trace(adHocPreamble, Level.INFO,"  AC = CAP2_smSSF_to_gsmSCF_AC"); 
	      ies.setInitialEvent(true);
	    }
	    
	    /////CAP v1
	    
	    else if (ac.equals(CAP1ApplicationContexts.cap_v1_gsmSSF_to_gsmSCF_AC))
	    {
	      if(infoEnabled())trace(adHocPreamble, Level.INFO,"  AC = cap_v1_gsmSSF_to_gsmSCF_AC"); 
	      ies.setInitialEvent(true);
	    }
	    
	    ////CS1
	    else if (ac.equals(CS1ApplicationContexts.core_INAP_CS1_SSP_to_SCP_AC))
	    {
	        if(infoEnabled())trace(Level.INFO,"  AC = core_INAP_CS1_SSP_to_SCP_AC"); 
	        ies.setInitialEvent(true);
	    }	    
	    
	    else
	    {
	      if(infoEnabled())trace(adHocPreamble, Level.INFO,"  Unsupported application context: "+ac); 
	    //Accepting anyway, just to send 'Continue'
	      ies.setInitialEvent(true);
	    }
    }
    else
    {
    	if(infoEnabled())trace("", Level.INFO,"  Unsupported event type: ",genericEvent != null?genericEvent.getClass(): "null");    	
    	ies.setInitialEvent(false);
    }
    return ies;
  }

  /**
   * 
   * @param ac
   * @return
   */
  public boolean validateACN(TcapApplicationContext ac)
  {
  	
    if(infoEnabled())trace(Level.INFO," Validating Application context: ",ac);
   
    
    ///// CAP v2
    if (ac.equals(CAP2ApplicationContexts.cap_v2_gsmSSF_to_gsmSCF_AC))
    {
      if(infoEnabled())trace(Level.INFO,"  AC = CAP2_smSSF_to_gsmSCF_AC"); 
      return true;
    }
    // Do we accept these two?? No we don't
	//    else if (ac.equals(CAP2ApplicationContexts.cap_v2_gsmSRF_to_gsmSCF_AC))
	//    {
	//      if(infoEnabled())trace(Level.INFO,"  AC = CAP2_gsmSRF_to_gsmSCF_AC"); 
	//      return true;
	//    }
	//    else if (ac.equals(CAP2ApplicationContexts.cap_v2_assist_gsmSSF_to_gsmSCF_AC))
	//    {
	//      if(infoEnabled())trace(Level.INFO,"  AC = CAP2_assist_gsmSSF_to_gsmSCF_AC"); 
	//      return true;
	//    } 
    
    /////CAP v1
    
    else if (ac.equals(CAP1ApplicationContexts.cap_v1_gsmSSF_to_gsmSCF_AC))
    {
      if(infoEnabled())trace(Level.INFO,"  AC = cap_v1_gsmSSF_to_gsmSCF_AC"); 
      return true;
    }
    
    ////CS1
    else if (ac.equals(CS1ApplicationContexts.core_INAP_CS1_SSP_to_SCP_AC))
    {
        if(infoEnabled())trace(Level.INFO,"  AC = core_INAP_CS1_SSP_to_SCP_AC"); 
        return true;    	
    }
    
	return false;
  }

//	So long to this method:
//  /**
//   * JC method to compose Open Response SCCP CgPN at MTP level
//   * 
//   * @return
//   */
//  private SccpAddress computeCgSccpAddress ()
//  {
//  	SccpAddress sccpAddr = null;
//  	
////  	if (addrParamName == null) {
////  		return (null);
////  	}
//
//		String addrParamString = confHelper.getString (SET_SCCP_CALLING_ADDRESS_PARAM_NAME);
//
//      //if (infoEnabled ())
//      //    trace (Level.INFO, "parameter ", addrParamName, "=", addrParamString);
//
//		if (addrParamString == null || addrParamString.length () == 0) {
//	        if (infoEnabled ())
//	            trace (Level.INFO, "undefined parameter ", SET_SCCP_CALLING_ADDRESS_PARAM_NAME);
//
//			return (null);
//		}
//		
//      if (infoEnabled ())
//          trace (Level.INFO, "processing parameter ", addrParamString);
//
//      String[] base = addrParamString.split ("\\:");
//      if (base.length != 2) {
//      	trace (Level.INFO, "error in parameter specifcation ", SET_SCCP_CALLING_ADDRESS_PARAM_NAME);
//      	return (null);
//      }
//      else {
//	        if (infoEnabled ())
//	            trace (Level.INFO, "    parameters_base_name=", base[0]);
//      }
//
//      StringBuilder addrString = new StringBuilder (256);
//      addrString.append (SCCP_HEADER_PARAM_NAME);
//      //SccpAddress sccpAddr = new SccpAddress (SccpAddress.Type.C7);
//		String[] args = base[1].split ("\\+");
//		for (int i = 0; i < args.length; i++) {
//			if (args[i].equals(ADDRESS_PARAM_NAME)) {
//	            if (infoEnabled ())
//	                trace (Level.INFO, "    address_parameter=", base[0], args[i]);
//	    		String value = confHelper.getString (base[0]+args[i]);
//	    		if (value == null) {
//	    			return (null);
//	    		}
//	    		addrString.append (SCCP_DIGITS_PARAM_NAME);
//	    		addrString.append (value);
//	    		//sccpAddr.setAddress (value);
//			}
//			else if (args[i].equals(NAI_PARAM_NAME)) {
//	            if (infoEnabled ())
//	                trace (Level.INFO, "    nai_parameter=", base[0], args[i]);
//	    		String value = confHelper.getString (base[0]+args[i]);
//	    		if (value == null) {
//	    			return (null);
//	    		}
//	    		addrString.append (SCCP_NATURE_PARAM_NAME);
//	    		addrString.append (value);
//	    		//sccpAddr.setNatureOfAddress (Integer.valueOf(value));
//			}
//			else if (args[i].equals(TT_PARAM_NAME)) {
//	            if (infoEnabled ())
//	                trace (Level.INFO, "    tt_parameter=", base[0], args[i]);
//	    		String value = confHelper.getString (base[0]+args[i]);
//	    		if (value == null) {
//	    			return (null);
//	    		}
//	    		addrString.append (SCCP_TT_PARAM_NAME);
//	    		addrString.append (value);
//	    		//sccpAddr.setTranslationType (Integer.valueOf(value));
//			}
//			else if (args[i].equals(NP_PARAM_NAME)) {
//	            if (infoEnabled ())
//	                trace (Level.INFO, "    np_parameter=", base[0], args[i]);
//	    		String value = confHelper.getString (base[0]+args[i]);
//	    		if (value == null) {
//	    			return (null);
//	    		}
//	    		addrString.append (SCCP_NUMBERING_PARAM_NAME);
//	    		addrString.append (value);
//	    		//sccpAddr.setTranslationType (Integer.valueOf(value));
//			}
//			else if (args[i].equals(SSN_PARAM_NAME)) {
//	            if (infoEnabled ())
//	                trace (Level.INFO, "    ssn_parameter=", base[0], args[i]);
//	    		String value = confHelper.getString ((base[0]+args[i]).trim());
//	    		if (value == null) {
//		    		addrString.append (SCCP_SSN_PARAM_NAME);
//		    		Integer localSSNParamInt = confHelper.getInt (LOCAL_SSN_PARAM_NAME);		
//		    		if (localSSNParamInt == null) {
//		    	        if (infoEnabled ())
//		    	            trace (Level.INFO, "undefined parameter ", LOCAL_SSN_PARAM_NAME, " Using default");
//		    	        localSSNParamInt=LOCAL_SSN_DEF;
//		    		}		    		
//		    		addrString.append (localSSNParamInt);
//	    	    	//sccpAddr.setSSN (LOCAL_SSN);
//	    		}
//	    		else {
//		    		addrString.append (SCCP_SSN_PARAM_NAME);
//		    		addrString.append (value);
//	    			//sccpAddr.setSSN (Integer.valueOf(value));
//	    		}
//			}
//
//		}
//  	//sccpAddr.setRouteOnPC (false);
//		
//		try {
//		    sccpAddr = SccpAddressParser.parseSccpAddress (addrString.toString ());
//		}
//		catch (Exception e) {
//			trace (Level.WARN, "error building callingSccpAddress <", addrString.toString (), ">. ", e);
//		}
//		
//	    return (sccpAddr);	
//  }
  
  private SccpAddress computeCgSccpAddress (SccpAddress incomingCdSccpAddr) throws Exception
  {
  	String value = null;
  	SccpAddress sccpAddr = null;
  	String addrParamName = new String (SET_SCCP_CALLING_ADDRESS_PARAM_NAME);
  	
		String addrParamString = confHelper.getString (addrParamName);
		if (addrParamString == null || addrParamString.length () == 0) {
	        if (infoEnabled ())
	            trace (Level.INFO, "undefined parameter ", addrParamName);

			return (null);
		}
		
      if (infoEnabled ())
          trace (Level.INFO, "processing parameter ", addrParamString);

      String[] base = addrParamString.split ("\\:");
      if (base.length != 2) {
      	trace (Level.INFO, "error in parameter specifcation ", addrParamName);
      	return (null);
      }
      else {
	        if (infoEnabled ())
	            trace (Level.INFO, "    parameters_base_name=", base[0]);
      }

      StringBuilder addrString = new StringBuilder (256);
      addrString.append (SCCP_HEADER_PARAM_NAME);
      //SccpAddress sccpAddr = new SccpAddress (SccpAddress.Type.C7);
		String[] args = base[1].split ("\\+");
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals(PC_PARAM_NAME)) {
	            if (infoEnabled ())
	                trace (Level.INFO, "    pc_parameter=", base[0], args[i]);
	    		value = confHelper.getString ((base[0]+args[i]).trim());
	    		if (value == null) {
	    			trace (Level.WARN, "undefined configuration parameter ", base[0], args[i]);
	    			return (null);
	    		}
	    		addrString.append (SCCP_PC_PARAM_NAME);
	    		addrString.append (value);
			}
			else if (args[i].equals(SSN_PARAM_NAME)) {
	            if (infoEnabled ())
	                trace (Level.INFO, "    ssn_parameter=", base[0], args[i]);
	    		value = confHelper.getString ((base[0]+args[i]).trim());
//	    		if (value == null) {
//	    			trace (Level.WARN, "undefined configuration parameter ", base[0], args[i]);
//	    			return (null);
//	    		}
//	    		addrString.append (SCCP_SSN_PARAM_NAME);
//	    		addrString.append (value);
	    		if (value == null) {
	    		addrString.append (SCCP_SSN_PARAM_NAME);
//	    		Integer localSSNParamInt = confHelper.getInt (LOCAL_SSN_PARAM_NAME);		
	    		if (value == null) {
	    	        if (infoEnabled ())
	    	            trace (Level.INFO, "undefined parameter ", LOCAL_SSN_PARAM_NAME, " Using default");
	    	        value=LOCAL_SSN_DEF;
	    		}		    		
	    		addrString.append (value);
    	    	//sccpAddr.setSSN (LOCAL_SSN);
	    		}
	    		else {
		    		addrString.append (SCCP_SSN_PARAM_NAME);
		    		addrString.append (value);
	    			//sccpAddr.setSSN (Integer.valueOf(value));
	    		}	    		
			} 
			else {
				trace (Level.WARN, "unknown configuration parameter ", base[0], args[i]);
				return (null);
			}
		}
  	//sccpAddr.setRouteOnPC (false);

		// Perform SCCP GT Mapping
		StringBuilder gtMapParameter = new StringBuilder (256);
		String outgoingGt = null;
		gtMapParameter.append(SCCP_GT_PARAM_NAME);
		gtMapParameter.append(SCCP_POP);
//		gtMapParameter.append(confHelper.getInt(HECLogic.HEC_LOCAL_POP_PARAMETER));
		gtMapParameter.append(SCCP_UNDERSCORE);
		if (incomingCdSccpAddr.hasGlobalTitleIndicator()) {
			gtMapParameter.append (incomingCdSccpAddr.getAddress());
			outgoingGt = confHelper.getString (gtMapParameter.toString());
			if (outgoingGt == null) {
				trace (Level.WARN,("undefined configuration parameter " + gtMapParameter.toString()));
				outgoingGt = confHelper.getString (SCCP_GT_DEFAULT_PARAM_NAME);
				if (outgoingGt == null) {
				    throw (new Exception ("undefined configuration parameter " + SCCP_GT_DEFAULT_PARAM_NAME));
				}
			}
		}
		else {
			trace (Level.WARN, "Incoming sccp address does not contain gt indicator");
			outgoingGt = confHelper.getString (SCCP_GT_DEFAULT_PARAM_NAME);
			if (outgoingGt == null) {
				throw (new Exception ("undefined configuration parameter " + SCCP_GT_DEFAULT_PARAM_NAME));
			}
		}

      if (infoEnabled ())
          trace (Level.INFO, "processing parameter ", gtMapParameter.toString(), "=", outgoingGt);

		String[] gtArgs = outgoingGt.split (",");

		if (incomingCdSccpAddr.hasTranslationType()) {
			addrString.append (SCCP_TT_PARAM_NAME);
			addrString.append (incomingCdSccpAddr.getTranslationType());
		}
		else {
		      if (infoEnabled ())
		          trace (Level.INFO, "Setting default TT ", SCCP_TT_DEF_VALUE_NAME);			
			addrString.append (SCCP_TT_PARAM_NAME);
			addrString.append (SCCP_TT_DEF_VALUE_NAME);
		}

		if (gtArgs[0] == null) {
			trace (Level.WARN, "gt absent in parameter ", gtMapParameter.toString());
			throw (new Exception ("imcomplete configuration parameter " + gtMapParameter.toString()));
		}
		else {
			addrString.append (SCCP_DIGITS_PARAM_NAME);
			addrString.append (gtArgs[0]);
      }

		if (gtArgs.length < 2 || gtArgs[1] == null) {
			trace (Level.WARN, "np absent in parameter ", gtMapParameter.toString());
			throw (new Exception ("imcomplete configuration parameter " + gtMapParameter.toString()));
		}
		else {
			addrString.append (SCCP_NUMBERING_PARAM_NAME);
			addrString.append (gtArgs[1]);
		}

		if (gtArgs.length < 3 || gtArgs[2] == null) {
			trace (Level.WARN, "nai absent in parameter ", gtMapParameter.toString());
			throw (new Exception ("imcomplete configuration parameter " + gtMapParameter.toString()));
		}
		else {
			addrString.append (SCCP_NATURE_PARAM_NAME);
			addrString.append (gtArgs[2]);
		}

		addrString.append (SCCP_RI_PARAM_NAME);
		addrString.append (SCCP_RI_GT_VALUE_NAME);

		try {
			if (infoEnabled ())
		          trace (Level.INFO, "Composed outgoing SCCP calling address: "+addrString);			
		    sccpAddr = SccpAddressParser.parseSccpAddress (addrString.toString ());
		}
		catch (Exception e) {
			trace (Level.WARN, "error building callingSccpAddress <", addrString.toString (), ">. ", e);
		}
		
	    return (sccpAddr);	
  }
  /**
   * Checks if the initial-DP corresponds to a mobile originated trigger.
   * This is valid just for CAP2
   * 
   * @param idpArg
   * 
   * @return
   */
  private boolean isMobileOriginated(CCEventTypeBCSM eType)
  throws UnsupportedOperationException
  {
    if(eType != null)
    {
    	// If not terminating we will suppose it is originating. We also suppose it works for CAPv1 too :) anf for CS1 :X
      return (! eType.equals(CCEventTypeBCSM.termAttemptAuthorized)); //CAPv2: terminating would be 'CCEventTypeBCSM.termAttemptAuthorized'
    }
    else 
    {
      throw new UnsupportedOperationException("Unavailable BCSM event type");
    }
  }  
  
  /**
   * 
   * @param opCo
   * @param ticket
   * @return
   */
  private UCM_OUT_NO_INTERCONN_Context retrieveRecordedNumber(String opCo, String ticket)
  {
    DummyRAActivity dummyActivity = null;
    if(infoEnabled())trace(Level.INFO,"Searching for a call context with opCo+ticket: "+opCo+ticket);
    dummyActivity = this.correlationRaActivityProvider.newDummyActivity(); //WTF?
    CorrelationNumberContainer contextContainer = null;
    UCM_OUT_NO_INTERCONN_Context context = null;
	try {
		contextContainer = (CorrelationNumberContainer)dummyActivity.getDummyNumber(opCo, ticket);
		context = HECContextFactory.getUCM_OUT_NO_INTERCONN_Context(contextContainer.getPayload());		
	} catch (Exception e) {
		trace(Level.ERROR,dumpStackTrace(e));		
		e.printStackTrace();		
	}
	if(context != null)
	{
		statsHelper.incCounter(Constants.ct_CS_Correlation_Retrieved);
	}
	else
	{
		incrementCounter(HECUsageCounter.CORRELATION_ERROR);
		statsHelper.incCounter(Constants.ct_CS_Correlation_Retrieval_Error);
	}
	dummyActivity.endActivity();
	if(infoEnabled())trace(Level.INFO,"Correlation activity disposed");
    return context;
  }    
// More scenario detection; keep it commented while not used
//
//  public static boolean isMobileTerminated(MessageInfo msgInfo)
//  {           
//        if( getEventType(msgInfo) != null){
//              return (getEventType(msgInfo).equals(CCEventTypeBCSM.termAttemptAuthorized));
//        }
//        else
//              return false;
//  }
//  public static boolean isMobileOriginated(MessageInfo msgInfo) {
//      CAP2InitialDPArg idpArg = (CAP2InitialDPArg) msgInfo.getInitialDPArg();
//      CCEventTypeBCSM eventType = getEventType(msgInfo);
//      if((eventType!= null)&& (idpArg != null))
//      {
//            return ( eventType.equals(CCEventTypeBCSM.collectedInfo)
//                       || eventType.equals(CCEventTypeBCSM.origAttemptAuthorized)
//                       || eventType.equals(CCEventTypeBCSM.analyzedInformation)
//                       && (! idpArg.hasRedirectingPartyID()) );
//      }
//      else
//            return false;
//
//	}     
//	public static boolean isForwarded(MessageInfo msgInfo) {
//      CAP2InitialDPArg idpArg = (CAP2InitialDPArg) msgInfo.getInitialDPArg();
//      CCEventTypeBCSM eventType = getEventType(msgInfo) ;
//      if((eventType!= null)&& (idpArg != null))
//      {
//            return ( eventType.equals(CCEventTypeBCSM.collectedInfo)
//                       || eventType.equals(CCEventTypeBCSM.origAttemptAuthorized)
//                       || eventType.equals(CCEventTypeBCSM.analyzedInformation)
//                       && ( idpArg.hasRedirectingPartyID()) );
//      }
//      else
//            return false;
//	}     
  
  

  /**************************************************************************/
  /***** Sample counters to measure mean execution time *********************/
  /**************************************************************************/
  
  /**
   * Required by the SLEE to manage the default usage parameters (counters)
   * @return {@link HECUsageParameters}
   */
  public abstract CSReroutingUsageParameters getDefaultSbbUsageParameterSet();  
  /**
   * Required by the SLEE to manage the default usage parameters (counters)
   * @return {@link HECUsageParameters}
   */
  public abstract CSReroutingUsageParameters getSbbUsageParameterSet(String name)
  throws javax.slee.usage.UnrecognizedUsageParameterSetNameException;    
  
  protected void incrementCounter(HECUsageCounter counter)
  {
	  try
	  {
		  CSReroutingUsageParameters usage = null;
	    try
	    {
	      usage = getSbbUsageParameterSet(nodeName);
	    }
	    catch(javax.slee.usage.UnrecognizedUsageParameterSetNameException e)
	    {
	      usage = getDefaultSbbUsageParameterSet();
	    }
	    switch(counter)
	    {
	    	case INCOMING_MESSAGE:
	    		usage.incrementIncomingMessage(1);
	    		break;
	    	case INVALID_INCOMING_MESSAGE:
	    		usage.incrementIncomingMessageError(1);
	    		break;
	    	case UNKNOWN_CALL_SCENARIO:
	    		usage.incrementCallScenarioError(1);
	    		break;
	    	case CORRELATION_ERROR:
	    		usage.incrementCorrelationError(1);
	    		break;
	    	default:
	    }
	    
	  }
	  catch(Exception e)
	  {
		  if(debugEnabled)dumpStackTrace(e);
		  trace(Level.ERROR, "Cannot increment counter ",counter);
	  }
  }
  
  protected void sampleCounter(CSScenario counter, long value)
  {
	  try
	  {
		  CSReroutingUsageParameters usage = null;
	    try
	    {
	      usage = getSbbUsageParameterSet(nodeName);
	    }
	    catch(javax.slee.usage.UnrecognizedUsageParameterSetNameException e)
	    {
	      usage = getDefaultSbbUsageParameterSet();
	    }
	    if (counter != null)
	    {
		    switch(counter)
		    {
		    	case MO_NO_RR:	    	
		    		usage.sampleResponseTimeMO(value);
		    		break;
		    	case MT_NO_RR:
		    		usage.sampleResponseTimeMT(value);
		    		break;    		
		    	case UCM_OUT_ONNET_NO_INTERCONN:
		    		usage.sampleResponseTimeUCM_OUT_CORRELATION(value);
		    		break;	    		
		    	default:
		    }
	    }
	    
	  }
	  catch(Exception e)
	  {
		  if(debugEnabled)dumpStackTrace(e);
		  trace(Level.ERROR, "Cannot increment counter ",counter);
	  }
  }
    
}
