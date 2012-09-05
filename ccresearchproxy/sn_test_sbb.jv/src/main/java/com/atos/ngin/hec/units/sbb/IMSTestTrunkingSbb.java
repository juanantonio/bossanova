/*
 * com.atos.ngin.hec.units.sbb.main.IMSTrunkingSbb.java        March 28, 2011 
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

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.ExtensionHeaderImpl;
import gov.nist.javax.sip.header.Priority;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.ims.PAssertedIdentity;

import java.io.IOException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.ListIterator;

import javax.naming.NamingException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.Header;
import javax.sip.message.Request;
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
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_ExtensionInfoVO.ExtType;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_GroupVO;
import com.atos.ngin.hec.hec_db.hec_model_logic.Hec_OpcoVO;
import com.atos.ngin.hec.simpleNist.event.IncomingCallEvent;
import com.atos.ngin.hec.simpleNist.type.CallActivity;
import com.atos.ngin.hec.simpleNist.type.CallLeg;
import com.atos.ngin.hec.simpleNist.type.CallLeg.CallLegType;
import com.atos.ngin.hec.units.cdr.HEC_CDR;
import com.atos.ngin.hec.units.hec_logic.HECContextFactory;
import com.atos.ngin.hec.units.hec_logic.HECLogic;
import com.atos.ngin.hec.units.hec_logic.HECLogic.BNumberType;
import com.atos.ngin.hec.units.hec_logic.HECLogic.IMSBNumber;
import com.atos.ngin.hec.units.hec_logic.HECLogic.IMSScenario;
import com.atos.ngin.hec.units.hec_logic.HECLogic.IMSTrigger;
import com.atos.ngin.hec.units.hec_logic.HECLogic.IMSTriggerValidation;
import com.atos.ngin.hec.units.hec_logic.HECLogic.NameAddress;
import com.atos.ngin.hec.units.hec_logic.MO_NO_RR_Context;
import com.atos.ngin.hec.units.hec_logic.UCM_OUT_NO_INTERCONN_Context;
import com.atos.ngin.hec.units.sbb.common.Constants;
import com.atos.ngin.hec.units.sbb.common.Constants.HECUsageCounter;
import com.atos.ngin.hec.units.sbb.common.HecBaseSbb;
import com.atos.ngin.hec.units.usage.HECUsageParameters;
import com.atos.ngin.hec.units.usage.IMSTrunkingUsageParameters;
import com.atos.ngin.hec.writer_ra.writer_ra_type.WriterProvider;

/**
 * This SBB implements the main functionality of the service logic for HEC.
 * Basically, it receives IMS call attempts and tries to find the appropriate 
 * trunk context and group, SBC, etc. With this data, the service reroutes 
 * the call towards CISCO UCM.
 * 
 * It uses the AtosOrigin MPCC-RA (for SIP dialogs).
 */
public abstract class IMSTestTrunkingSbb extends HecBaseSbb
{
  //--------------------------------- Constants ----------------------------//
//	< 1, IMS, 500, Internal Error >
//	< 2, IMS, 400, Parameter error  >
//	< 3, IMS, 403, ICS  >
//	< 4, IMS, 403, OCS  >
//	< 5, IMS, 403, Originating extension inactive  >
//	< 6, IMS, 403, Originating extension inactive and the call is forwarded.  >
//	< 7, IMS, 403, Destination extension inactive  >
//	< 8, IMS, 403, No announcement in roaming  >
//	< 9, IMS, 500, No message found for announcement and language  >
//	< 11, IMS, 486, Standard busy release cause.  >
//	< 14, IMS, 404, Not Found  >
//	< 15, IMS, 483, Too Many Hops  >
//	< 22, IMS, 403, Intra Convergent calls  >
  public static final int RCID_UNSUPPORTED_REQ_URI_FORMAT 	= 1;
  public static final int RCID_NO_CALL_CONTEXT 				= 1;
  public static final int RCID_INTERNAL_ERROR 				= 1;
  public static final int RCID_404_NOT_FOUND 				= 14;
  
  public static final String RC_DEFAULT = "404"; //must be parseable as integer...
  
  //Config parameter: acts like a switch, if present the feature(PSU insertion to help routing) is on
  private static final String INSERT_PSU = "HEC_INSERT_PSU";

  private static final String USER_STR = "user";
  private static final String PHONE_STR = "phone";
  
  // CorrelationRA Binding
  protected final static String CORRELATIONRA_PROVIDER = "CorrelationRAActivityProvider";
  protected final static String CORRELATIONRA_FACTORY  = "CorrelationRAActivityContextInterfaceFactory";
  protected final static String MCIDWRITERRA_PROVIDER = "com/atos/ra/conf/api/AtosMCIDLogProvider";
  protected final static String MCIDWRITERRA_FACTORY  = "com/atos/ra/conf/api/AtosMCIDLogACIFactory";
  
  
  
  
  //--------------------------------- Variables ----------------------------//
  // DummyRA PROVIDER
  protected DummyRAActivityProvider                correlationRaActivityProvider = null; 
  protected DummyRAActivityContextInterfaceFactory correlationRaAciFactory = null;  
  protected WriterProvider						   mcidWriterProvider = null;
	  
  
  //---------------------------------- Methods -----------------------------//    
  
  public void setSbbContext(SbbContext context)
  {
    super.setSbbContext(context);
    try
    {
      this.correlationRaActivityProvider = (DummyRAActivityProvider)this.jndiContext.lookup(CORRELATIONRA_PROVIDER);
      System.err.println("DummyRA activity provider: " + this.correlationRaActivityProvider);
      this.correlationRaAciFactory = (DummyRAActivityContextInterfaceFactory)this.jndiContext.lookup(CORRELATIONRA_FACTORY);
      System.err.println("ACI factory: " + this.correlationRaAciFactory);
      this.mcidWriterProvider = (WriterProvider)this.jndiContext.lookup(MCIDWRITERRA_PROVIDER);
      System.err.println("MCID log writer provider: " + this.mcidWriterProvider);      
      
      //      this.ccActivityProvider = (CCProvider)jndiContext.lookup(MPCCRA_PROVIDER);
      //      if(dbgEnabled())trace(Level.INFO,"MPCC Activity provider: " + this.ccActivityProvider);
      //      this.ccAciFactory = (CCActivityContextInterfaceFactory) jndiContext.lookup(MPCCRA_FACTORY);
      //      if(dbgEnabled())trace(Level.INFO,"MPCC ACI factory: " + this.ccAciFactory);      
    } 
    catch (NamingException ne) {
      System.err.println("** Error obtaining sbbContext "+ne.getMessage());
    } catch (TransactionRolledbackLocalException e) {
      System.err.println("** TransactionRolledbackLocalException " + e.getMessage());
    } catch (FacilityException e) {
    	System.err.println("** FacilityException "+ e.getMessage());
    } catch (NullPointerException e) {
    	System.err.println("** NullPointerException "+ e.getMessage());
    } catch (IllegalArgumentException e) {
    	System.err.println("** IllegalArgumentException "+ e.getMessage());
    }
    System.err.println("End setSbbcontext");    
  }
  /**
   * IES to accept only opens for protocols we understand
   */
  public InitialEventSelector initialEventSelect(InitialEventSelector ies)
  {	
	// we use the logger directly, instead of our wrapper method trace(..), so no wrong preamble is displayed
    if(dbgEnabled())tracer.log(Level.DEBUG,"Initial event selector. ");
//    Object genericEvent = ies.getEvent();
//    if(genericEvent instanceof InitialCallAttemptEvent)
//    {
//    	if(dbgEnabled())tracer.log(Level.DEBUG,"SIP Event ");
//    }
    return ies;
  }

    
  /**
   * Initial event received from the CC-RA
   * 
   * @param event
   * @param aci
   */
  public void onIncomingCall (IncomingCallEvent event, ActivityContextInterface aci)
  {
	long initTime = System.nanoTime();
    if(infoEnabled())trace(Level.INFO,"InitialCallAttempt Event received. Event: ",event);
//    if(infoEnabled())trace(Level.INFO,"Event: ",event.toStringLongFormat());
    //    if(infoEnabled())trace(Level.INFO,"Selected Services: ",event.getSelectedServices());
    
//    if(infoEnabled()&& event.getSelectedServices() != null)trace(Level.INFO,"Selected Services: ",event.getSelectedServices().getTriggerName());
    
    statsHelper.incCounter(Constants.ct_IMS_Call_Attempts);
    incrementCounter(HECUsageCounter.INCOMING_MESSAGE);
    
    int callSequenceNumber = -1;
    String callId = null;
    CallActivity mpCall = null;
    IMSTriggerValidation triggerValidation = null;
    
    try 
    {
      //      if(dbgEnabled())trace(Level.DEBUG,dumpObject(event).toString());
      Object activity = aci.getActivity();      
      if (activity != null && activity instanceof CallActivity) 
      {
    	  mpCall = (CallActivity) activity;    	  
//	      callSequenceNumber = event.getCallId();
	      callId = mpCall.getIncomingCallId();
	      this.storeTracePreamble(callId);

	      //    	  if(dbgEnabled())trace(Level.DEBUG,dumpObject(mpCall).toString());        
	      if(dbgEnabled())trace(Level.DEBUG,"CallLeg:  ",mpCall.getIncomingCallLeg());
	      //	      if(dbgEnabled())trace(Level.DEBUG,"CallLeg:  ",dumpObject(event.getCallLeg()).toString());
	      //	      if(dbgEnabled())trace(Level.DEBUG,"CallLegAdditionalData:  ",event.getCallLeg().getAdditionalData());
	      
	      Request sipProtocolData = null;	      
	      try
	      {
//	    	  sipProtocolData = (Request)event.getCallLeg().getAdditionalData().getAdditionalDataValue(CallLegParameters.INT_IN_PROTOCOL_DATA);
	    	  
	    	  // Two options:
	    	  //	    	  sipProtocolData = mpCall.getIncomingCallLeg().getInitialRequest();
	    	  sipProtocolData = event.getInitialRequest();
	      }
	      catch (Throwable e)
	      {
	    	  e.printStackTrace();
	      }
	      if (sipProtocolData != null)
	      {	  
			  //	    	  if(dbgEnabled())trace(Level.DEBUG,"sipProtocolData:  ",dumpObject(sipProtocolData).toString());
			  //	    	  if(dbgEnabled())trace(Level.DEBUG,"sipProtocolData:  ",sipProtocolData.getBody());	
			  //	    	  if(dbgEnabled())trace(Level.DEBUG,"sipProtocolData:  ",event.toStringLongFormat());
	    	  if(dbgEnabled())trace(Level.DEBUG,"REQUEST:  ",sipProtocolData/*.getRequest()*/);
//	    	  URI requestUri = sipProtocolData.getUri();
	    	  URI requestUri = sipProtocolData.getRequestURI();
	    	  HECLogic.URIUserPart rqURIUser = null;
	    	  String rqURIDomain = null; 
	    	  if (requestUri instanceof SipURI)
	    	  {
	    		  if(dbgEnabled())trace(Level.DEBUG,"Received SipURI: ",requestUri);
	    		  rqURIUser = new HECLogic.URIUserPart(((SipURI)requestUri).getUser());
	    		  rqURIDomain = ((SipURI)requestUri).getHost();
	    	  }
	    	  ///////// Drop B: support for tel URL is required /////////
  	    	  else
  	    	  if (requestUri instanceof TelURL)
  	    	  {
  	    		  if(dbgEnabled())trace(Level.DEBUG,"Received TelURL: ",requestUri);
  	    		  rqURIUser = new HECLogic.URIUserPart('+'+((TelURL)requestUri).getPhoneNumber());//Add '+'  
  	    		  //rqURIUser = ((TelURL)requestUri).getPhoneNumber();//This does not work (this method returns a number without '+'))
  	    		  rqURIDomain = null;
  	    	  }
	    	  else
	    	  {
	    		  trace(Level.WARN,"Unsupported format for Request URI: "+requestUri);
	    		  releaseCall(RCID_UNSUPPORTED_REQ_URI_FORMAT, mpCall, aci);
	    		  incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
	    		  return;
	    	  }
	    	  
	    		  
	    	  //	    	  if(infoEnabled())trace(Level.INFO,"Request URI  = ",requestUri);
	    	  if(infoEnabled())trace(Level.INFO,"Request URI user part = ",rqURIUser);

	    	  
	    	  /*   Check DiversionHeader */
//	    	  ExtensionHeaderImpl diversionHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(Diversion.NAME);			  
	    	  ExtensionHeaderImpl diversionHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader("Diversion");
			  if(diversionHeader != null)
			  {
				  if(infoEnabled())trace(Level.INFO, "DiversionHeader: ",diversionHeader);	
				  statsHelper.incCounter(Constants.ct_IMS_Diversion);
			  }
			  else
			  {
				  if(infoEnabled())trace(Level.INFO, "No diversion header found ");
			  }	    	  
			  

	    	  //////////////////////////////////////
	    	  /// CR004: Emergency call handling /// 			
	    	  //////////////////////////////////////
			  
			  Priority priorityHeader = (Priority)sipProtocolData.getHeader(Priority.NAME);	
			  boolean emergencyCall = priorityHeader != null? "emergency".equalsIgnoreCase(priorityHeader.getValue()):false;
			  if (priorityHeader != null) trace(Level.DEBUG,"Priority Header: ",priorityHeader.getValue());
			  if(dbgEnabled() && emergencyCall)trace(Level.DEBUG,"Priority emergency detected");
			  
	    	  //////////////////////////
	    	  /// Trigger Validation ///
	    	  //////////////////////////
	    	  triggerValidation = 
//	    		  HECLogic.validateTrigger(event.getSelectedServices().getTriggerName(), 
	    		  //TODO:
	    		  HECLogic.validateTrigger(event.getTrigger(),
	    				  rqURIUser, rqURIDomain, emergencyCall, 
	    				  confHelper, statsHelper);
	    	
	    	  if (triggerValidation != null)
	    	  {	    		  
	    		  switch(triggerValidation.scenario)
	    		  {
	    		  	case MO_NO_RR:
	    		  		statsHelper.incCounter(Constants.ct_IMS_MO_NO_RR);
	    		  		processIMS_MO_NO_RR(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		break;
	    		  	case MO_RR:
	    		  		statsHelper.incCounter(Constants.ct_IMS_MO_RR);
	    		  		processIMS_MO_RR(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		break;	    		  		
	    		  	case MT_NO_RR:
	    		  		statsHelper.incCounter(Constants.ct_IMS_MT_NO_RR);
	    		  		processIMS_MT_NO_RR(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		break;	    		  		
	    		  	case MT_RR:
	    		  		statsHelper.incCounter(Constants.ct_IMS_MT_RR);
	    		  		processIMS_MT_RR(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		break;	    		  		
	    		  	case UCM_IN_PSTN:
	    		  		statsHelper.incCounter(Constants.ct_IMS_UCM_IN_PSTN);
	    		  		processIMS_UCM_IN_PSTN(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		break;	    		  		
	    		  	case UCM_OUT_ONNET:
	    		  		if(diversionHeader != null)
	    		  		{
	    		  			statsHelper.incCounter(Constants.ct_IMS_UCM_FW_FORK_ONNET);
	    		  			processIMS_FW_FORK_ONNET(triggerValidation, mpCall, sipProtocolData, diversionHeader, aci);
	    		  		}
	    		  		else
	    		  		{
	    		  			statsHelper.incCounter(Constants.ct_IMS_UCM_OUT_ONNET);
	    		  			processIMS_UCM_OUT_ONNET(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		}
	    		  		break;	    		  		
	    		  	case UCM_OUT_OFFNET:
	    		  		if(diversionHeader != null)
	    		  		{
	    		  			statsHelper.incCounter(Constants.ct_IMS_UCM_FW_FORK_OFFNET);
	    		  			processIMS_FW_FORK_OFFNET(triggerValidation, mpCall, sipProtocolData, diversionHeader, aci);
	    		  		}
	    		  		else
	    		  		{
	    		  			statsHelper.incCounter(Constants.ct_IMS_UCM_OUT_OFFNET);
	    		  			processIMS_UCM_OUT_OFFNET(triggerValidation, mpCall, sipProtocolData, aci);
	    		  		}
	    		  		break;
	    		  	case UCM_OUT_OFFNET_EMERGENCY:
	    		  		//By the moment, diversion header makes no difference
    		  			statsHelper.incCounter(Constants.ct_IMS_UCM_OUT_OFFNET_EMERGENCY);
    		  			processIMS_UCM_OUT_OFFNET_EMERGENCY(triggerValidation, mpCall, sipProtocolData, aci);

	    		  		break;
	    		  		
	    		  	default: 
	    		  		//Actually, this should never happen so there's no couter for it
	    		  		trace(Level.WARN,"Unsupported scenario: ",triggerValidation.scenario);
	    		  		releaseCall(RCID_404_NOT_FOUND, mpCall, aci);
	    		  		incrementCounter(HECUsageCounter.UNKNOWN_CALL_SCENARIO);
	    		  		return;
	    		  	
	    		  }	    		     
	    		  statsHelper.incCounter(Constants.ct_IMS_Trigger_Validated);
	    	  }
	    	  else
	    	  {
	    		  trace(Level.ERROR,"Could not tell HEC scenario from Request URI: "+requestUri);
	    		  releaseCall(RCID_404_NOT_FOUND, mpCall, aci);
	    		  incrementCounter(HECUsageCounter.UNKNOWN_CALL_SCENARIO);
	    		  return;
	    	  }	    	  	    	  	    	  
	      }
	   	  if (dbgEnabled()) trace(Level.DEBUG,"[", callSequenceNumber, "] Leaving onInitialCallAttemptEvent method on sbb");
      } 
      else 
      {
        if(infoEnabled())trace(Level.INFO,"Invalid activity for event onInitialCallAttemptEvent");
      }
    } 
    catch (Throwable e) 
    {
      if(infoEnabled())trace(Level.INFO,"Exception processing InitialCallAttempt: ",e);
      if(infoEnabled())trace(Level.INFO,dumpStackTrace(e));
      incrementCounter(HECUsageCounter.INVALID_INCOMING_MESSAGE);
      e.printStackTrace();
      releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);      
      return;
    } 
    //This should be always executed:
    try
    {
    	aci.detach(this.getSbbLocalObject());
    	if(infoEnabled())trace(Level.INFO,"Successfully detached");
    	if (triggerValidation != null)
    	{	    		  
    		sampleCounter(triggerValidation.scenario, (System.nanoTime()-initTime)/1000);
		}
    }
    catch(Exception e)
    {
    	e.printStackTrace();
    	trace(Level.WARN,"Exception detaching from aci: ",aci);
    }
    if(infoEnabled())trace(Level.INFO,"Initial Call Attempt Event processed");
    
  }  
  
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////  SCENARIO PROCESSING  ////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  

  
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// TERMINATING SCENARIOS ////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * IMS part of this scenario.  
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_MT_NO_RR(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  try
	  {
		  ////////////////////////////
		  /// Forced ON-PABX func. ///
		  ////////////////////////////	  
		  
		  //nothing to do with those by the moment, right?
		  //		  FromHeader from = (FromHeader)sipProtocolData.getHeader(FromHeader.NAME);
		  //		  ToHeader to = (ToHeader)sipProtocolData.getHeader(ToHeader.NAME);
		  
		  /*
		    
		  Since B validation was performed in the CS domain and no correlation info is conveyed, we need to do it again
		  	   
		  */
		  IMSBNumber numB = triggerValidation.addressPayload;
//		  String numB = triggerValidation.opCoVO.getCountryCode()+numBNat;
		  
		  Hec_ExtensionInfoVO extInfoB = null;
			  
		  extInfoB = HECLogic.validateHECMobileExtension(numB.addressPayloadStr); 
		  if(extInfoB == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for B: "+numB);		  
			  releaseCall(RCID_404_NOT_FOUND, mpCall, aci);
			  return;
		  }
		  
		  ////////////
		  /// MCID ///
		  ////////////
		  // CR007: MCID
		  HECLogic.checkMCID(sipProtocolData, extInfoB, mcidWriterProvider, confHelper, statsHelper);
		  
		  OutgoingCallLegInfo oCLInfo = forceOnPABX(mpCall, sipProtocolData, numB, extInfoB);
	  
		  
		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////	  
		  //Check PAID as a HEC subscriber, whether mobile or geographic
		  //According to LLD, this is no longer necessary
//		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HEADER_PAID);
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHExtension(paidHeader);
//		  if (extInfoA != null && extInfoA.getVpnId() == extInfoB.getVpnId())
//		  {
//			  if(debugEnabled)trace(Level.DEBUG, "ONNetCall: "+extInfoB.getVpnId());
//			  hec_cdr.setAPartyInfo(extInfoA);
//			  hec_cdr.setOnNet();
//		  }
//		  else
//		  {
//			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall: "+extInfoA);
//			  hec_cdr.setOffNet();
//		  }		  
		  		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setBPartyInfo(extInfoB);
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_MT);
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
	  
		  
	  }
	  catch (Exception e)
	  {
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  e.printStackTrace();
		  return;
	  }		  
  }

  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_MT_RR(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  try
	  {	  
	      //////////////////
		  // #B VALIDATION //
		  ///////////////////  
		  IMSBNumber numB = triggerValidation.addressPayload;
		  		  
		  Hec_ExtensionInfoVO extInfoB = null;
			  
		  extInfoB = HECLogic.validateHECMobileExtension(numB.addressPayloadStr); 
		  if(extInfoB == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for B: "+numB);		  
			  releaseCall(RCID_404_NOT_FOUND, mpCall, aci);	
			  return;
		  }
		  
		  ////////////
		  /// MCID ///
		  ////////////
		  // CR007: MCID
		  HECLogic.checkMCID(sipProtocolData, extInfoB, mcidWriterProvider, confHelper, statsHelper);

		  
		  ////////////////////////////
		  /// Forced ON-PABX func. ///
		  ////////////////////////////
		  OutgoingCallLegInfo oCLInfo = forceOnPABX(mpCall, sipProtocolData, numB, extInfoB);
		  
		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////
		  //According to LLD, this is no longer necessary	BUG 7727	  
//		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HEADER_PAID);		  
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHExtension(paidHeader, confHelper, statsHelper);
//		  if (extInfoA != null && extInfoA.getVpnId() == extInfoB.getVpnId())
//		  {
//			  if(debugEnabled)trace(Level.DEBUG, "ONNetCall: "+extInfoB.getVpnId());
//			  hec_cdr.setAPartyInfo(extInfoA);
//			  hec_cdr.setOnNet();
//		  }
//		  else
//		  {
//			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall: "+(extInfoA!=null?extInfoA:"no extension Info for A") );
//			  hec_cdr.setOffNet();
//		  }			  
		  
		  	  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setBPartyInfo(extInfoB);
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_MT);	
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);	
	  }
	  catch (Exception e)
	  {
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  e.printStackTrace();
		  return;
	  }		  
  }

  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_UCM_IN_PSTN(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  try
	  {
		  /*
		    Some kind of B validation 
		  */
		  IMSBNumber numB = triggerValidation.addressPayload;		  
		  //This one should be geographic, right?
		  Hec_ExtensionInfoVO extInfoB = null;
				  
		  extInfoB = HECLogic.validateHECGeographicExtension(numB.addressPayloadStr, confHelper, statsHelper); 
		  if(extInfoB == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for B: "+numB);		  
			  releaseCall(RCID_404_NOT_FOUND, mpCall, aci);
			  return;
		  }
		  
		  ////////////
		  /// MCID ///
		  ////////////
		  // TODO CR007: MCID
		   HECLogic.checkMCID(sipProtocolData, extInfoB, mcidWriterProvider, confHelper, statsHelper);
		  
		  ////////////////////////////
		  /// Forced ON-PABX func. ///
		  ////////////////////////////
		  
		  OutgoingCallLegInfo oCLInfo = forceOnPABX(mpCall, sipProtocolData, numB, extInfoB);		  		  		 		  	 		  

		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////	 

		  // This type of call is always OFFNET
		  // No need to check PAID
		  //According to LLD, this filed is no longer necessary in this scenario:		  
		  //		  hec_cdr.setOffNet();
		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setBPartyInfo(extInfoB);
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_IT);	
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);	
	  }
	  catch (Exception e)
	  {
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  e.printStackTrace();
		  return;
	  }			  
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// ORIGINATING SCENARIOS ////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_MO_NO_RR(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  try
	  {
		  ////////////////////////////
		  /// Correlation Function ///
		  ////////////////////////////
//		  MO_NO_RR_Context storedContextInfo = retrieveRecordedNumber(triggerValidation.opCoVO.getOpCoId().toString(), // Possible bug: it's a db table key 
		  MO_NO_RR_Context storedContextInfo = retrieveRecordedNumber(triggerValidation.opCoVO.getCountryCode().toString(),
				  triggerValidation.addressPayload.addressPayloadStr);
		  if (storedContextInfo == null)
		  {
			  releaseCall(RCID_NO_CALL_CONTEXT, mpCall, aci);
			  trace(Level.ERROR,"Could not find call context from ticket");
			  return;
		  }
		  
		  if(infoEnabled())trace(Level.INFO,"Call context found: "+storedContextInfo.toString());
		  
		  ////////////////////////////
		  /// Forced ON-PABX func. ///
		  ////////////////////////////
		  //input: storedContextInfo: cd,cg,hecGroupIdA
		  String cdPN = storedContextInfo.getCalledPartyNumber(); // This must be already normalized in CS domain
		  BNumberType cdPT = storedContextInfo.getCalledPartyType();
		  String cgPN = storedContextInfo.getCallingPartyNumber();// E.164
		  IMSBNumber numB = new IMSBNumber(cdPT, cdPN, cdPN);
		  
//		  Hec_GroupVO hecGroupA = Hec_GroupDAO.getGroupByGroupId(hecGroupIdA);
		  Hec_ExtensionInfoVO extInfoA = HECLogic.validateHECMobileExtension(cgPN); //No need to format this?

		  if(extInfoA == null)
		  {	
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+cgPN);
			  releaseCall(RCID_404_NOT_FOUND,mpCall, aci);
			  return;
		  }
		  OutgoingCallLegInfo oCLInfo = forceOnPABX(mpCall, sipProtocolData, numB, extInfoA);
		  
		  
		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////
		  //1 B-party normalization (if not in CS). By the moment, we suppose CdPN has been already normalized in CS domain
		  //2 search Called in HEC DB
		  if(debugEnabled)trace(Level.DEBUG, " vpnIdA: "+extInfoA.getVpnId());

		  //BUG 8354: enhancement: no need to query for other types than publicNumber
		  Hec_ExtensionInfoVO extInfoB = null;
		  if (numB.bNumberType == BNumberType.PublicNumberINT || numB.bNumberType == BNumberType.PublicNumberNAT)
		  {
			  extInfoB = HECLogic.validateHECExtension(cdPN, confHelper, statsHelper);
		  }
		  if (extInfoB != null && extInfoB.getVpnId() == extInfoA.getVpnId())
		  {
			  if(debugEnabled)trace(Level.DEBUG, "ONNetCall: "+extInfoA.getVpnId());
			  hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setOnNet();
		  }
		  //BUG 7761
		  else if(extInfoB != null && extInfoB.getExtType() == ExtType.pstn)
		  {
			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall towards hec pstn extension: "+extInfoB );
			  hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setOffNet();			  
		  }
		  else if(extInfoB != null && extInfoB.getExtType() == ExtType.mobile)
		  {
			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall towards hec mobile extension: "+extInfoB );
			  hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setOffNet();			  
		  }		  		  
		  else
		  {
			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall: "+(extInfoB!=null?extInfoB.getVpnId():"no extension Info for B") );
			  //BUG 11695: 
			  switch (numB.bNumberType)
			  {
			  	case PNPShortNumber:
			  	case NonDecimalNumber:
			  		hec_cdr.setIndetermined();
			  		break;
			  	default:
			  		hec_cdr.setOffNet();
			  }
		  }	  
		  
		  ///////////////////////
		  /// CDR generation  ///
		  ///////////////////////			  	    			  
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setAPartyInfo(extInfoA);
		  hec_cdr.setField(HEC_CDR.mscAddress_Label, storedContextInfo.getMSCAddress());		
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);	
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_MO);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
		  
	  
	  }
	  catch(Exception e)
	  {
		  trace(Level.WARN,"Exception processing IMS_MO_NO_RR: ",e);
		  releaseCall(RCID_INTERNAL_ERROR,mpCall,aci);
		  e.printStackTrace();
		  return;
	  }
  }
  
  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_MO_RR(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  try
	  {
		  //////////////////////////
		  /// A-PARTY VALIDATION ///
		  //////////////////////////
//		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HECLogic.HDR_PAID);
		  PAssertedIdentity paidHeader = (PAssertedIdentity)sipProtocolData.getHeader(HECLogic.HDR_PAID);
		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHMobile(paidHeader);	  
		  if(extInfoA == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+paidHeader);			  
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }
		  
		  
		  ////////////////////////////
		  /// Forced ON-PABX func. ///
		  ////////////////////////////
		  IMSBNumber numB = triggerValidation.addressPayload;
		  //		  String numB = triggerValidation.opCoVO.getCountryCode()+numBNat;
	 
		  OutgoingCallLegInfo oCLInfo = forceOnPABX(mpCall, sipProtocolData, numB, extInfoA);
		  
		  
		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////	  
		  //Might be either mobile or geographic		  
		  if(debugEnabled)trace(Level.DEBUG, " vpnIdA: "+extInfoA.getVpnId());		  
		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECExtension(numB.addressPayloadStr, confHelper, statsHelper); 
		  if(extInfoB != null && extInfoB.getVpnId() == extInfoA.getVpnId())
		  {
			  if(debugEnabled)trace(Level.DEBUG, "ONNetCall. Destination: "+extInfoB);
			  hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setOnNet();
		  }
		  //BUG 7761
		  else if(extInfoB != null && extInfoB.getExtType() == ExtType.pstn)
		  {
			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall towards hec pstn extension: "+extInfoB );
			  hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setOffNet();			  
		  }
		  else if(extInfoB != null && extInfoB.getExtType() == ExtType.mobile)
		  {
			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall towards hec mobile extension: "+extInfoB );
			  hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setOffNet();			  
		  }
		  else
		  {
			  if(debugEnabled)trace(Level.DEBUG, "OFFNetCall: "+(extInfoB!=null?extInfoB.getVpnId():"no extension Info for B") );
			  //BUG 11695: 
			  switch (numB.bNumberType)
			  {
			  	case PNPShortNumber:
			  	case NonDecimalNumber:
			  		hec_cdr.setIndetermined();
			  		break;
			  	default:
			  		hec_cdr.setOffNet();
			  }			  
		  }			  
	  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setAPartyInfo(extInfoA);		  
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_MO);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
	  }
	  catch(Exception e)
	  {
		  trace(Level.WARN,"Exception processing IMS_MO_RR: ",e);
		  releaseCall(RCID_INTERNAL_ERROR,mpCall,aci);
		  e.printStackTrace();
		  return;
	  }
  }
  
  /**
   * Call originated in UCM towards onnet mobile destinations (no need to inspect B-Number)
   * 
   * @param triggerValidation
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_UCM_OUT_ONNET(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);

	  try
	  {
		  
		  /////////////////////////////////
		  /// A-PARTY VALIDATION (PSTN) ///
		  /////////////////////////////////
		  // Validate PAID as HEC geographical		  
//		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HECLogic.HDR_PAID);
		  PAssertedIdentity paidHeader = (PAssertedIdentity)sipProtocolData.getHeader(HECLogic.HDR_PAID);
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHGeographic(paidHeader, confHelper, statsHelper);
		  //'Specs bug'
		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHExtension(paidHeader, confHelper, statsHelper);
		  if(extInfoA == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+paidHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }			  		  
		  if(infoEnabled)trace(Level.INFO,"A party validated: "+extInfoA);
		  
		  ///////////////////////////
		  /// Scenario Validation ///
		  ///////////////////////////
		  //Validate Destination B from Request URI
		  IMSBNumber numB = triggerValidation.addressPayload;
		  //Do we really need to validate B? Yes, we need to fill B params in the CDR
		  // BUG 7607 FIXED:
		  //		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECMobileExtension(numB);
		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECExtension(numB.addressPayloadStr, confHelper, statsHelper);		  	  
		  if(extInfoB == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for B: "+numB);			  
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }
		  
		  
		  // Validate Contact 
		  ContactHeader contactHeader = (ContactHeader)sipProtocolData.getHeader(ContactHeader.NAME);
		  Hec_GroupVO contactGroupVO = HECLogic.processUCMContactHeader(contactHeader.getAddress().getURI().toString());
		  if(contactGroupVO == null )
		  {
			  if(debugEnabled)trace(Level.DEBUG, "Cannot get PABX information from Contact Header: "+contactHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;	  
		  }	  
		  if (extInfoA.getPabxId() != null && ! extInfoA.getPabxId().equals(contactGroupVO.getPabxId()))
		  {
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by PAID header: "+extInfoA.getPabxId());
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by contact header: "+contactGroupVO.getPabxId());
			  trace(Level.WARN, "Invalid PABX credentials. Releasing call. "+contactHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;		  		  
		  }		  
		  
		  ///////////////
		  /// Routing ///
		  ///////////////
		  OutgoingCallLegInfo oCLInfo = routeToOpco(mpCall, sipProtocolData, numB);
		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////		  
		  //BUG 12462: reverted  by BUG 12967 
			//		  if (extInfoA.getExtType() == Hec_ExtensionInfoVO.ExtType.pstn)
			//		  {
			  HEC_CDR hec_cdr = new HEC_CDR();
			  
			  ///////////////////////////////
			  /// Call Type Determination ///
			  ///////////////////////////////	
			  hec_cdr.setOnNet();	
			  
			  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
			  hec_cdr.setAPartyInfo(extInfoA);
			  hec_cdr.setBPartyInfo(extInfoB);
			  
			  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_IO);	
			  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
			  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
			//		  }
			//		  else
			//		  {
			//			  if(debugEnabled)trace(Level.DEBUG,"non-PSTN A party detected: Skipping CDR generation for this scenario");
			//		  }		  
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  return;
	  }
  }

  /**
   * Call originated in UCM towards offnet destinations. B-party could be HEC number belonging to a different corporate.
   * 
   * @param triggerValidation
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_UCM_OUT_OFFNET(IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  
	  /* Check diversion Header */
	  try
	  {
		  /////////////////////////////////
		  /// A-PARTY VALIDATION (PSTN) ///
		  /////////////////////////////////
		  // Validate PAID as HEC geographical
//		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HECLogic.HDR_PAID);
		  PAssertedIdentity paidHeader = (PAssertedIdentity)sipProtocolData.getHeader(HECLogic.HDR_PAID);
		  //	  Hec_GroupVO groupVO = HECLogic.processUCMPAssertedIdentityHeader(paidHeader);
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHGeographic(paidHeader, confHelper, statsHelper);
		  //'Specs bug'
		  Hec_ExtensionInfoVO extInfoA = (triggerValidation.addressPayload.bNumberType != BNumberType.SpecialDestination)
		  			|| (triggerValidation.addressPayload.bNumberType != BNumberType.EmergencyNumber)?
				  HECLogic.processEHExtension(paidHeader, confHelper, statsHelper)
				  :HECLogic.processEHGeographic(paidHeader, confHelper, statsHelper); //BUG 11446 (enhancement)
		  if(extInfoA == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+paidHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }		  		  
		  if(infoEnabled)trace(Level.INFO,"A party validated: "+extInfoA);
		  
		  ///////////////////////////
		  /// Scenario Validation ///
		  ///////////////////////////	  
		  // Get group info from Contact Header
		  ContactHeader contactHeader = (ContactHeader)sipProtocolData.getHeader(ContactHeader.NAME);
		  Hec_GroupVO contactGroupVO = HECLogic.processUCMContactHeader(contactHeader.getAddress().getURI().toString());
		  if(contactGroupVO == null )
		  {
			  if(debugEnabled)trace(Level.DEBUG, "Cannot get PABX information from Contact Header: "+contactHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;	  
		  }	  
		  if (extInfoA.getPabxId() != null && ! extInfoA.getPabxId().equals(contactGroupVO.getPabxId()))
		  {
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by PAID header: "+extInfoA.getPabxId());
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by contact header: "+contactGroupVO.getPabxId());
			  trace(Level.WARN, "Invalid PABX credentials. Releasing call");
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;		  		  
		  }
		  
		  if(infoEnabled)trace(Level.INFO,"Scenario validated: "+contactGroupVO.getPabxId());
	
		  	  
		  
		  /////////////////////////////
		  /// B-Party Normalization ///
		  /////////////////////////////	  
		  //Validate Destination B from Request URI
		  // TODO: special numbers, etc
		  IMSBNumber numB = triggerValidation.addressPayload;
		  // BUG 7607 FIXED:
		  //		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECMobileExtension(numB);		  
		  Hec_ExtensionInfoVO extInfoB = null;
		  if(numB.bNumberType != BNumberType.SpecialDestination)
		  {				  
			  extInfoB = HECLogic.validateHECExtension(numB.addressPayloadStr, confHelper, statsHelper); 
		  }
		  else
		  {
			  if(infoEnabled)trace(Level.INFO,"Skipping B validation for special number: "+numB);
		  }		
		  
		  if(debugEnabled && extInfoB != null) trace(Level.DEBUG,"Looks like destination is a HEC Subscriber");

		 
		  
		  ///////////////
		  /// Routing ///
		  ///////////////
		  OutgoingCallLegInfo oCLInfo = routeBreakoutOffnetCall(mpCall, sipProtocolData, numB);
		  		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  //BUG 12462: reverted  by BUG 12967
			//		  if (extInfoA.getExtType() == Hec_ExtensionInfoVO.ExtType.pstn)
			//		  {
			  HEC_CDR hec_cdr = new HEC_CDR();
				  ///////////////////////////////
				  /// Call Type Determination ///
				  ///////////////////////////////	
				  //BUG 11695: I do not think it applies to this scenario
					//		  switch (numB.bNumberType)
					//		  {
					//		  	case PNPShortNumber:
					//		  	case NonDecimalNumber:
					//		  		hec_cdr.setIndetermined();
					//		  		break;
					//		  	default:
					//		  		hec_cdr.setOffNet();
					//		  }
				  // So, anyway, we'll keep doing the same
			  hec_cdr.setOffNet();	
			  
			  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
			  hec_cdr.setAPartyInfo(extInfoA);
			  if(extInfoB != null) hec_cdr.setBPartyInfo(extInfoB);
			  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_IO);	
			  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
			  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
			//		  }
			//		  else
			//		  {
			//			  if(debugEnabled)trace(Level.DEBUG,"non-PSTN A party detected: Skipping CDR generation for this scenario");
			//		  }
		  
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  return;
	  }	  
  }
  
  
  /**
   * Call originated in UCM towards EMERGENCY destination. 
   * 
   * @param triggerValidation
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  private void processIMS_UCM_OUT_OFFNET_EMERGENCY (IMSTriggerValidation triggerValidation, CallActivity mpCall, Request sipProtocolData, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  /* Check diversion Header */
	  try
	  {
		  /////////////////////////////////
		  /// A-PARTY VALIDATION (PSTN) ///
		  /////////////////////////////////
		  // Validate PAID as HEC geographical
		  //		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HECLogic.HDR_PAID);
		  PAssertedIdentity paidHeader = (PAssertedIdentity)sipProtocolData.getHeader(HECLogic.HDR_PAID);

		  //BUG 11446
		  //		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHExtension(paidHeader, confHelper, statsHelper);
		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHGeographic(paidHeader, confHelper, statsHelper);
		  if(extInfoA == null)
		  {			  
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+paidHeader);
			  if(infoEnabled)trace(Level.INFO,"Routing emergency call anyway");
			  if(paidHeader != null)
			  {
				  GenericURI paidUri = HECLogic.getGenericUriFromHeaderNameAddress(paidHeader.getHeaderValue());
				  String isdn = HECLogic.getUserFromGenericURL(paidUri);
				  String sipA = new StringBuilder(HECLogic.SIP_URI_PREFIX).append(HECLogic.IAC_PLUS_SIGN).append(isdn).
							  append(HECLogic.AT_SIGN).append(confHelper.get(HECLogic.HEC_CORPORTATE_DOMAIN_PARAMETER)).toString();			  
				  hec_cdr.setAPartyInfoForEmergency(isdn, sipA);
			  }
			  else
			  {
				  hec_cdr.setAPartyInfoForEmergency(null, null);  
			  }
		  }		  
		  else
		  {
			  if(infoEnabled)trace(Level.INFO,"A party validated: "+extInfoA);
			  hec_cdr.setAPartyInfo(extInfoA);
		  }
		  
		  ///////////////////////////
		  /// Scenario Validation /// No need to validate contact info
		  ///////////////////////////	  
		  // Get group info from Contact Header
//		  ContactHeader contactHeader = (ContactHeader)sipProtocolData.getHeader(ContactHeader.NAME);
//		  Hec_GroupVO contactGroupVO = HECLogic.processUCMContactHeader(contactHeader.getAddress().getURI().toString());
//		  if(contactGroupVO == null )
//		  {
//			  if(debugEnabled)trace(Level.DEBUG, "Cannot get PABX information from Contact Header: "+contactHeader);
//			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
//			  return;	  
//		  }	  
//		  if (extInfoA.getPabxId() != null && ! extInfoA.getPabxId().equals(contactGroupVO.getPabxId()))
//		  {
//			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by PAID header: "+extInfoA.getPabxId());
//			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by contact header: "+contactGroupVO.getPabxId());
//			  trace(Level.WARN, "Invalid PABX credentials. Releasing call");
//			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
//			  return;		  		  
//		  }
		  
//		  if(infoEnabled)trace(Level.INFO,"Scenario validated: "+contactGroupVO.getPabxId());


		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////	

		  hec_cdr.setEmergency();
		  
		  ///////////////
		  /// Routing ///
		  ///////////////
		  IMSBNumber numB = triggerValidation.addressPayload;		  
		  OutgoingCallLegInfo oCLInfo = routeBreakoutOffnetCall(mpCall, sipProtocolData, numB);
		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);		  
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_IO);	
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
		  
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  return;
	  }	  
  }  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////// FW&FORKING SCENARIOS ////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  

  /**
   * 
   * @param triggerValidation
   * @param mpCall
   * @param sipProtocolData
   * @param aci
   */
  //TODO separate methods for ONNET / OFFNET triggers?
  private void processIMS_FW_FORK_ONNET(IMSTriggerValidation triggerValidation, CallActivity mpCall, 
		  Request sipProtocolData, /*Diversion diversionHeader*/ ExtensionHeaderImpl diversionHeader, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario FW or FORKING onnet ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();
	  try
	  {		  
		  /////////////////////////////////
		  /// A-PARTY VALIDATION (PSTN) ///
		  /////////////////////////////////
		  // Validate Diversion as HEC extension, whether mobile or geographic		  
//		  ExtensionHeaderImpl paidHeader = (ExtensionHeaderImpl)sipProtocolData.getHeader(HEADER_PAID);
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.validateHECExtension(diversionHeader.getDiversionNumber());
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHGeographic(diversionHeader, confHelper, statsHelper);
		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHExtension(diversionHeader, confHelper, statsHelper);
//		  Hec_ExtensionInfoVO extInfoA = HECLogic.processPAIDGeographic(paidHeader);	  
		  if(extInfoA == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+diversionHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }	  
		  if(infoEnabled)trace(Level.INFO,"A party validated: "+extInfoA);
		  
		  ///////////////////////////
		  /// Scenario Validation ///
		  ///////////////////////////
		  //Validate Destination B from Request URI
		  IMSBNumber numB = triggerValidation.addressPayload;
		  //Do we really need to validate B? Yes, we need to fill B params in the CDR
		  // BUG 7607 FIXED:
		  //		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECMobileExtension(numB);		  
		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECExtension(numB.addressPayloadStr, confHelper, statsHelper);
		  if(extInfoB == null)
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for B: "+numB);			  
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }	
		  // Validate Contact 
		  ContactHeader contactHeader = (ContactHeader)sipProtocolData.getHeader(ContactHeader.NAME);
		  Hec_GroupVO contactGroupVO = HECLogic.processUCMContactHeader(contactHeader.getAddress().getURI().toString());
		  if(contactGroupVO == null )
		  {
			  if(debugEnabled)trace(Level.DEBUG, "Cannot get PABX information from Contact Header: "+contactHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;	  
		  }	  
		  if (extInfoA.getPabxId() != null && ! extInfoA.getPabxId().equals(contactGroupVO.getPabxId()))
		  {
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by PAID header: "+extInfoA.getPabxId());
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by contact header: "+contactGroupVO.getPabxId());
			  trace(Level.WARN, "Invalid PABX credentials. Releasing call");
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;		  		  
		  }		  
		  
		  /////////////////////////////
		  /// CR003: add PAID       ///
		  /////////////////////////////
		  String psuSwitch = configurationProvider.getConfiguration().getStringWithoutLogError(INSERT_PSU);
		  if(infoEnabled)trace(Level.INFO,"PSU switch " + psuSwitch!=null?psuSwitch:"OFF");
		  if ( psuSwitch != null && psuSwitch.equalsIgnoreCase("ON"))
		  {
			  Header psuHdr = sipProtocolData.getHeader(HECLogic.HDR_PSU);
			  if (psuHdr == null)
			  {
				  try
				  {
					  Header paidHdr = HECLogic.generatePAIDHeaderFromDiversion(diversionHeader, sipProtocolData);
					  sipProtocolData/*.getRequest()*/.addHeader(paidHdr);
					  if(infoEnabled)trace(Level.INFO,"P-Served-User header included: "+paidHdr);
				  }
				  catch(Exception e)
				  {
					  if(infoEnabled)trace(Level.ERROR,"Could not get P-Served-User from diversion header: "+diversionHeader );
				  }
			  }
			  else
			  {
				  if(infoEnabled)trace(Level.INFO,"Incoming PSU: "+psuHdr );
			  }
		  }
		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////	
		  //ONNET, right?
		  hec_cdr.setOnNet();
		  
		  ///////////////
		  /// Routing ///
		  ///////////////
		  OutgoingCallLegInfo oCLInfo = routeToOpco(mpCall, sipProtocolData, numB);
		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setAPartyInfo(extInfoA);
		  hec_cdr.setBPartyInfo(extInfoB);
		  
		  //BUG 11717
		  NameAddress nameAddressDiversion = HECLogic.getNameAddressFromHeaderNameAddress(diversionHeader.getHeaderValue());
		  if(nameAddressDiversion != null && nameAddressDiversion.addrSpec != null ) hec_cdr.setField(HEC_CDR.redirectingPN_Label,nameAddressDiversion.addrSpec.toString() );

		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_FW);	
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  return;
	  }		
  }
  /**
   * 
   * @param triggerValidation
   * @param mpCall
   * @param sipProtocolData
   * @param diversionHeader
   * @param aci
   */
  private void processIMS_FW_FORK_OFFNET(IMSTriggerValidation triggerValidation, CallActivity mpCall, 
		  Request sipProtocolData, /*Diversion diversionHeader*/ ExtensionHeaderImpl diversionHeader, ActivityContextInterface aci)
  {
	  if(infoEnabled())trace(Level.INFO, "Processing scenario FW or FORKING offnet ",triggerValidation.scenario);
	  HEC_CDR hec_cdr = new HEC_CDR();;
	  try
	  {
		  /////////////////////////////////
		  /// A-PARTY VALIDATION (PSTN) ///
		  /////////////////////////////////
		  // Validate Diversion as HEC extension, whether mobile or geographic		  
		  Hec_ExtensionInfoVO extInfoA = HECLogic.processEHExtension(diversionHeader, confHelper, statsHelper);	  
		  if(extInfoA == null)
			  
		  {
			  if(infoEnabled)trace(Level.INFO,"Could not find extension info for A: "+diversionHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;
		  }	  
		  if(infoEnabled)trace(Level.INFO,"A party validated: "+extInfoA);
		  
		  ///////////////////////////
		  /// Scenario Validation ///
		  ///////////////////////////	  
		  // Get group info from Contact Header
		  ContactHeader contactHeader = (ContactHeader)sipProtocolData.getHeader(ContactHeader.NAME);
		  Hec_GroupVO contactGroupVO = HECLogic.processUCMContactHeader(contactHeader.getAddress().getURI().toString());
		  if(contactGroupVO == null )
		  {
			  if(debugEnabled)trace(Level.DEBUG, "Cannot get PABX information from Contact Header: "+contactHeader);
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;	  
		  }	  
		  if (extInfoA.getPabxId() != null && ! extInfoA.getPabxId().equals(contactGroupVO.getPabxId()))
		  {
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by PAID header: "+extInfoA.getPabxId());
			  if(debugEnabled)trace(Level.DEBUG, "PABX Id by contact header: "+contactGroupVO.getPabxId());
			  trace(Level.WARN, "Invalid PABX credentials. Releasing call");
			  releaseCall(RCID_404_NOT_FOUND,mpCall,aci);
			  return;		  		  
		  }
		  
		  if(infoEnabled)trace(Level.INFO,"Scenario validated: "+contactGroupVO.getPabxId());
	
		  	  
		  /////////////////////////////
		  /// CR003: add PAID       ///
		  /////////////////////////////
		  String psuSwitch = configurationProvider.getConfiguration().getString(INSERT_PSU);
		  if(infoEnabled)trace(Level.INFO,"PSU switch" + psuSwitch);
		  if ( psuSwitch != null && psuSwitch.equalsIgnoreCase("ON"))
		  {
			  Header psuHdr = sipProtocolData.getHeader(HECLogic.HDR_PSU);
			  if (psuHdr == null)
			  {
				  try
				  {
					  Header paidHdr = HECLogic.generatePAIDHeaderFromDiversion(diversionHeader, sipProtocolData);
					  sipProtocolData/*.getRequest()*/.addHeader(paidHdr);
					  if(infoEnabled)trace(Level.INFO,"P-Served-User header included: "+paidHdr);
				  }
				  catch(Exception e)
				  {
					  if(infoEnabled)trace(Level.ERROR,"Could not get P-Served-User from diversion header: "+diversionHeader );
				  }
			  }
			  else
			  {
				  if(infoEnabled)trace(Level.INFO,"Incoming PSU: "+psuHdr );
			  }
		  }
		  /////////////////////////////
		  /// B-Party Normalization ///
		  /////////////////////////////	  
		  //Validate Destination B from Request URI
		  IMSBNumber numB = triggerValidation.addressPayload;
		  // BUG 7607 FIXED:
		  //		  Hec_ExtensionInfoVO extInfoB = HECLogic.validateHECMobileExtension(numB);
		  Hec_ExtensionInfoVO extInfoB = null;
		  if(numB.bNumberType != BNumberType.SpecialDestination)
		  {				  
			  extInfoB = HECLogic.validateHECExtension(numB.addressPayloadStr, confHelper, statsHelper); 
		  }
		  else
		  {
			  if(infoEnabled)trace(Level.INFO,"Skipping B validation for special number: "+numB);
		  }			  
		  
		  if(extInfoB != null && debugEnabled)
		  {
			  if(debugEnabled)trace(Level.DEBUG,"Looks like destination is a HEC Subscriber");
			  hec_cdr.setBPartyInfo(extInfoB);
		  }		  
		  ///////////////////////////////
		  /// Call Type Determination ///
		  ///////////////////////////////	
		  //BUG 11695: I do not think it applies to this scenario
			//		  switch (numB.bNumberType)
			//		  {
			//		  	case PNPShortNumber:
			//		  	case NonDecimalNumber:
			//		  		hec_cdr.setIndetermined();
			//		  		break;
			//		  	default:
			//		  		hec_cdr.setOffNet();
			//		  }
		  // So, anyway, we'll keep doing the same
		  hec_cdr.setOffNet();
		  ///////////////
		  /// Routing ///
		  ///////////////
		  //TODO P-Served-Identity		  
		  OutgoingCallLegInfo oCLInfo = routeBreakoutOffnetCall(mpCall, sipProtocolData, numB);
		  
		  //////////////////////
		  /// CDR Generation ///
		  //////////////////////
		  hec_cdr.setField(HEC_CDR.reqUri_Label, "sip:"+triggerValidation.addressPayload.originalBNumber);
		  hec_cdr.setAPartyInfo(extInfoA);
		  //		  hec_cdr.setField(HEC_CDR.redirectingPN_Label, diversionHeader.getHeaderValue());
		  //BUG 11717
		  NameAddress nameAddressDiversion = HECLogic.getNameAddressFromHeaderNameAddress(diversionHeader.getHeaderValue());
		  if(nameAddressDiversion != null && nameAddressDiversion.addrSpec != null ) hec_cdr.setField(HEC_CDR.redirectingPN_Label,nameAddressDiversion.addrSpec.toString() );
		  
		  hec_cdr.setField(HEC_CDR.recordType_Label, HEC_CDR.RT_FW);	
		  hec_cdr.setField(HEC_CDR.calledPN_Label, oCLInfo.requestURI);		  
		  scheduleCDRGeneration(mpCall, CallLegType.EGRESS, hec_cdr);
		  
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  releaseCall(RCID_INTERNAL_ERROR, mpCall, aci);
		  return;
	  }	  	  
  }  
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////  PROCESSING FEATURES  ////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////   
  
  /**
   * Retrieves a previously stored object 
   * 
   * @param ticket
   * @return
   */
  private MO_NO_RR_Context retrieveRecordedNumber(String opCo, String ticket)
  {
    DummyRAActivity dummyActivity = null;
    if(infoEnabled())trace(Level.INFO,"Searching for a call context with opCo+ticket: "+opCo+ticket);
    dummyActivity = this.correlationRaActivityProvider.newDummyActivity(); //WTF?
    CorrelationNumberContainer contextContainer = null;
    MO_NO_RR_Context context = null;
	try {
		contextContainer = (CorrelationNumberContainer)dummyActivity.getDummyNumber(opCo, ticket);
		context = HECContextFactory.getMO_NO_RR_Context(contextContainer.getPayload());
	} catch (Exception e) {
		trace(Level.ERROR,dumpStackTrace(e));		
		e.printStackTrace();		
	}
	if(context != null)
	{
		statsHelper.incCounter(Constants.ct_IMS_Correlation_Retrieved);
	}
	else
	{
		statsHelper.incCounter(Constants.ct_IMS_Correlation_Retrieval_Error);
		incrementCounter(HECUsageCounter.CORRELATION_ERROR);
	}	
	dummyActivity.endActivity();
	if(infoEnabled())trace(Level.INFO,"Correlation activity disposed");
    return context;
  }  
    
  
  private static class OutgoingCallLegInfo
  {
//	  CallLeg outgoingCallLeg;
	  String requestURI;
	  //...and more!
	  OutgoingCallLegInfo(/*CallLeg oCL,*/ String reqUri)
	  {
//		  outgoingCallLeg = oCL;
		  requestURI = reqUri;
	  }
  }
  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param numB
   * @throws IOException
   * @throws ParseException
   * @throws Exception
   */
  private OutgoingCallLegInfo routeBreakoutOffnetCall (CallActivity mpCall, Request sipProtocolData, IMSBNumber numB)
  throws IOException, ParseException, Exception
  {
	  statsHelper.incCounter(Constants.ct_IMS_Breakout_Offnet);
	  return routeBreakoutCall(mpCall, sipProtocolData, numB, null);
  }
  
  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param numB
   * @param opCoVO
   * @throws IOException
   * @throws ParseException
   * @throws Exception
   */
  private OutgoingCallLegInfo routeToOpco (CallActivity mpCall, Request sipProtocolData, IMSBNumber numB)
  throws IOException, ParseException, Exception
  {
	  statsHelper.incCounter(Constants.ct_IMS_Breakout_Onnet);
	  Hec_OpcoVO opCoVO = HECLogic.getServingOpCo(numB.addressPayloadStr);
	  return routeBreakoutCall(mpCall, sipProtocolData, numB, opCoVO);
  }
  /**
   * 
   * @param mpCall
   * @param sipProtocolData
   * @param numB
   * @param opCoVO
   */
  private OutgoingCallLegInfo routeBreakoutCall (CallActivity mpCall, Request sipProtocolData, IMSBNumber numB, Hec_OpcoVO opCoVO)
  throws IOException, ParseException, Exception
  {	  
	  String reqURIUserPartStr = null;
	  To toHeader = null;
	  String corporateDomain = confHelper.getString(HECLogic.HEC_CORPORTATE_DOMAIN_PARAMETER);	  
	  if(opCoVO == null) //route breakout offnet call
	  {
		  //BUG 7655
		  if(numB.bNumberType == BNumberType.SpecialDestination || numB.bNumberType == BNumberType.EmergencyNumber)
		  {
			  reqURIUserPartStr = numB.addressPayloadStr;
			  statsHelper.incCounter(Constants.ct_IMS_B_Special_Destination);
		  }
		  else
		  {
			  reqURIUserPartStr = HECLogic.IAC_PLUS_SIGN+numB.addressPayloadStr;
		  }
	  }
	  else //route to OpCo
	  {		  
		  if(! opCoVO.isTdmConnection())
		  {
			  if(infoEnabled())trace(Level.INFO,"Routing Call to OpCo through IMS: "+opCoVO.getOpCoName());
			  //Strip PSI and ONNET prefix from original request URI and add IN inh prefix	
			  //BUG 7655
			  if(numB.bNumberType == BNumberType.SpecialDestination)
			  {
				  reqURIUserPartStr = numB.addressPayloadStr;
				  statsHelper.incCounter(Constants.ct_IMS_B_Special_Destination);
			  }
			  else
			  {
				  String numBNat = numB.addressPayloadStr.substring(opCoVO.getCountryCode().toString().length()); //numB is CC+Nat# -> strip CC so we can put the  INprefix between them
				  reqURIUserPartStr = HECLogic.IAC_PLUS_SIGN+opCoVO.getCountryCode()+opCoVO.getIn_prefix()+numBNat;
			  }
		  }
		  else
		  {
			  if(infoEnabled())trace(Level.INFO,"Routing Call to OpCo through IMS + TDM connection: "+opCoVO.getOpCoName());
			  //Set Correlation Number as destination
			  UCM_OUT_NO_INTERCONN_Context context = new UCM_OUT_NO_INTERCONN_Context();
			  context.setImsSessionId(mpCall.getIncomingCallId());
			  //BUG 7655 TODO: is this a dead branch?
			  if(numB.bNumberType == BNumberType.SpecialDestination)
			  {
				  context.setCalledPartyNumberNATFormat(numB.addressPayloadStr);
				  statsHelper.incCounter(Constants.ct_IMS_B_Special_Destination);
			  }
			  else
			  {			  
				  String numBNat = numB.addressPayloadStr.substring(opCoVO.getCountryCode().toString().length()); //numB is CC+Nat# -> strip CC so we can put the  INprefix between them			  
				  context.setCalledPartyNumberNATFormat(numBNat);
				  
				  ///////////////// BUG 11611
				  String numBTo = HECLogic.IAC_PLUS_SIGN+opCoVO.getCountryCode()+opCoVO.getIn_prefix()+numBNat;
				  toHeader = new To();
				  AddressImpl toAddress = new AddressImpl();
//				  SipUri toURI = (SipUri)sipProtocolData.createUri(numBTo, corporateDomain);		
				  SipURI toURI = mpCall.getAddressFactory().createSipURI(numBTo, corporateDomain);
				  toURI.setParameter(USER_STR,PHONE_STR);
				  toAddress.setAddess(toURI);
				  toHeader.setAddress(toAddress);
				  
				  if(infoEnabled())trace(Level.INFO,"Created TO header "+toHeader);   
				  /////////////////
			  }
			  if(infoEnabled())trace(Level.INFO,"Context object: {",context,"}. Writting in Correlation-RA...");             	
			  // 
	    	  // Store the HEC-Number in the "Correlation-RA" which is used as on-the-fly database
			  //
			  String correlationId = null;		
			  // Let the exception go up.
	 	      //For CorrelationRA		 
			  try
			  {
				  //				  correlationId = HECLogic.recordCorrelationContext(opCoVO.getOpCoId().toString(), // Possible bug: it's a db table key
				  correlationId = HECLogic.recordCorrelationContext(opCoVO.getCountryCode().toString(),
	    			HECContextFactory.getSerializedPayload(context),correlationRaActivityProvider);
				  statsHelper.incCounter(Constants.ct_IMS_Correlation_Recorded);
			  }
			  catch (Exception e)
			  {
				  //Just for the counter
				  statsHelper.incCounter(Constants.ct_IMS_Correlation_Recording_Error);
				  incrementCounter(HECUsageCounter.CORRELATION_ERROR);
				  throw e;
			  }
	    	  reqURIUserPartStr=HECLogic.IAC_PLUS_SIGN+opCoVO.getCountryCode()+opCoVO.getOperatorCode()+opCoVO.getPrefix1()+correlationId;
		  }	  
	  }
	  //Restore original reqURI params, if any
	  if(numB.originalBNumber != null && numB.originalBNumber.paramsString != null)
	  {
		  reqURIUserPartStr = new StringBuilder(reqURIUserPartStr).append(';').append(numB.originalBNumber.paramsString).toString();
	  }
	  if(infoEnabled())trace(Level.INFO,"Processed B number: "+numB);	
	  if(infoEnabled())trace(Level.INFO,"Modified RequestURIStr: "+reqURIUserPartStr);	  
	  
	  if(infoEnabled())trace(Level.INFO,"Configured Corporate Domain: "+corporateDomain);	 
//	  SipUri requestURI = (SipUri)sipProtocolData.createUri(reqURIUserPartStr, corporateDomain);
	  SipUri requestURI = (SipUri)mpCall.getAddressFactory().createSipURI(reqURIUserPartStr, corporateDomain);
	  requestURI.setParameter("user", "phone"); //Check this is for all cases
	  if(infoEnabled())trace(Level.INFO,"Modified RequestURI: "+requestURI);
	  
	  // This is to prove those are useless methods
	  // We are forced by the API to do this 
	  //	  Address destinationAddress = new Address(reqURIUserPartStr, 0);
	  //	  destinationAddress.setAddressDomain("internal.domain");	  
	  
	  //
	  //Create and route Call Leg
	  //
////	  CallLegImpl newLeg = mpCall.createCallLeg(destinationAddress);
//	  CallLeg newLeg = mpCall.createEgressRequest();
////	  CallLegParameters clParams = mpCall.createCallLegParameters();
//	  
////	  Request sPD = new Request(null,null);
//	  Request sPD = newLeg.getInitialRequest();
	  		  
	  Request sPD = mpCall.createEgressRequest();
	  
	  // CR003: set P-Served-User 
	  Header psuHdr = sipProtocolData.getHeader(HECLogic.HDR_PSU);
//	  Header alreadySetPsuHdr = sPD.getHeader("P-Served-User");
	  if(psuHdr != null /*&& alreadySetPsuHdr == null*/)
	  {
		  sPD.addHeader(psuHdr);
		  if(infoEnabled())trace(Level.INFO,"Setting PSU "+psuHdr);		  
	  }
//	  else
//	  {
//		  if(infoEnabled())trace(Level.INFO,"paidHdr="+psuHdr);
//		  if(infoEnabled())trace(Level.INFO,"alreadySetPaidHdr="+alreadySetPsuHdr);
//	  }
		  
	  if(infoEnabled())trace(Level.INFO,"Setting new request URI: "+requestURI);	  
	  //	  sPD.setUriSetByCC(false);	    			  
//	  sPD.setUri(requestURI);	  
	  sPD.setRequestURI(requestURI);
	  if(toHeader != null)
	  {
		  sPD.addHeader(toHeader);
		  if(infoEnabled())trace(Level.INFO,"TO header included "+toHeader);  
	  }
//	  clParams.setAdditionalDataValue(CallLegParameters.INT_OUT_PROTOCOL_TO_USE, sPD);
//	  mpCall.createAndRouteCallLeg(newLeg, clParams);
//	 TODO:  mpCall.dispathNewLeg() or sth like that
	  mpCall.dispatchEgressRequest(sPD);
	  if(infoEnabled())trace(Level.INFO,"Call Leg created and routed");
	  return new OutgoingCallLegInfo(/*newLeg,*/ requestURI.toString());
	  
  }
  
  /**
   * Sets RequestURI and Route Header then created the new call leg.
   *  
   * @param mpCall
   * @param sipProtocolData
   * @param numB
   * @param extInfo
   * @throws Exception
   */
  private OutgoingCallLegInfo forceOnPABX (CallActivity mpCall, Request sipProtocolData, IMSBNumber numB, Hec_ExtensionInfoVO extInfo) throws Exception
  {
	  if(infoEnabled())trace(Level.INFO,"Forcing OnPABX");	
	  
	  statsHelper.incCounter(Constants.ct_IMS_Force_On_PABX);
	  //
	  // re-generate Request URI: numB is normalized <CC><Nat#> so we have to include '+'
	  // Look out! white space is not allowed in SIP URIs
	  //
	  //BUG 7655: for special destinations, the expected format is without '+'
	  String formattedNumB = null;
	  if(numB.bNumberType == BNumberType.SpecialDestination || 
			  numB.bNumberType == BNumberType.PNPShortNumber || 
			  numB.bNumberType == BNumberType.NonDecimalNumber)
	  {
		  formattedNumB = numB.addressPayloadStr;
	  }
	  else
	  {	
		  formattedNumB = HECLogic.IAC_PLUS_SIGN + numB.addressPayloadStr;
	  }
	  String reqURIStr = new StringBuilder(formattedNumB).append(";tgrp=").append(extInfo.getTrunkGroup())
			  .append(";trunk-context=").append(extInfo.getTrunkCtx())
//			  .append(";user=phone")  //Check this is for all cases
			  .toString();
			// These are no longer params but part of the 'user'
			//	  requestURI.setParameter("tgrp", "trunkgroup1");
			//	  requestURI.setParameter("trunk-context", "dom.ain");
	  //Restore original reqURI params, if any
	  if(numB.originalBNumber != null && numB.originalBNumber.paramsString != null)
	  {
		  reqURIStr = new StringBuilder(reqURIStr).append(';').append(numB.originalBNumber.paramsString).toString();
	  }	  
	  if(infoEnabled())trace(Level.INFO,"Processed B number: "+numB);		  
	  if(infoEnabled())trace(Level.INFO,"Modified RequestURIStr: "+reqURIStr);	  
	  String corporateDomain = confHelper.getString(HECLogic.HEC_CORPORTATE_DOMAIN_PARAMETER);
	  if(infoEnabled())trace(Level.INFO,"Configured Corporate Domain: "+corporateDomain);	 
//	  SipUri requestURI = (SipUri)sipProtocolData.createUri(reqURIStr, corporateDomain);
	  SipUri requestURI = (SipUri)mpCall.getAddressFactory().createSipURI(reqURIStr, corporateDomain);	  
	  requestURI.setParameter("user", "phone"); //Check this is for all cases
	  if(infoEnabled())trace(Level.INFO,"Modified RequestURI: "+requestURI);
//	  sipProtocolData.setUri(requestURI);
//	  sipProtocolData.setRequestURI(requestURI); //This is the incoming request: by doing this we are messing with the stack processing!
	  
	  //
	  // Route Header: leave provisioned value 'as is' and append '<sip:' and '>'
	  //
	  // Must 'Route' be always set when forcingONPABX?:
//	  SipUri routeURI = (SipUri)sipProtocolData.createUri(null, extInfo.getRoute());	  
	  /* How 'bout that?:
	   * (RFC3261 P74)If the route set is not empty, and its first URI does not contain 
	   * the lr parameter, the UAC MUST place the first URI from the route set into the 
	   * Request-URI, stripping any parameters that are not allowed in a Request-URI */
//	  routeURI.setParameter("lr", null);
	  	  
	  StringBuilder routeHeaderStr = new StringBuilder(HECLogic.OPEN_BRACKET).append(HECLogic.SIP_URI_PREFIX).append(extInfo.getRoute()).append(HECLogic.CLOSE_BRACKET);
//	  Header configuredRouteHeader = sipProtocolData.createHeader("Route", routeURI.toString());		  
//	  Header configuredRouteHeader = sipProtocolData.createHeader(HECLogic.HDR_ROUTE, routeHeaderStr.toString());
	  Header configuredRouteHeader = mpCall.getHeaderFactory().createHeader(HECLogic.HDR_ROUTE, routeHeaderStr.toString());

	  
	  // TODO: check this out:
	  //Parece que en triggers terminantes se usa más bien esto:
	  //	  Address destinationAddress = new Address(reqURIStr, 0);
	  //	  destinationAddress.setAddressDomain(corporateDomain);// TODO get this from DB
	  		  		
	  //
	  //Create and route Call Leg
	  //
////	  CallLegImpl newLeg = mpCall.createCallLeg(destinationAddress);
//	  CallLeg newLeg = mpCall.createEgressRequest();
////	  CallLegParameters clParams = mpCall.createCallLegParameters();
////	  Request sPD = new Request(null,null);
//	  Request sPD = newLeg.getInitialRequest();

	  Request sPD = mpCall.createEgressRequest();
	  
	  if(infoEnabled())trace(Level.INFO,"Setting new request URI: "+requestURI);
	  //	  sPD.setUriSetByCC(false);	    			  
//	  sPD.setUri(requestURI);	  
	  sPD.setRequestURI(requestURI);
	  
	  /********************************************************************************/
	  /***********  Route stack manipulation ******************************************/
	  /********************************************************************************/
	  if(infoEnabled())trace(Level.INFO,"Composing Route stack...");
	  ListIterator<Header> incomingRouteList = sipProtocolData.getHeaders("Route");
	  if (incomingRouteList != null && incomingRouteList.hasNext())
	  {
//		  if(infoEnabled())trace(Level.INFO,"Discarding first element in the list (HEC_AS): "+incomingRouteList.next());
		  int routeStackPos=1;
		  while (/*incomingRouteList != null &&*/ incomingRouteList.hasNext())
		  {
			  Header nextRoute = incomingRouteList.next();
			  if(infoEnabled())trace(Level.INFO,"Including Route "+(routeStackPos++)+": "+nextRoute);
			  sPD.addHeader(nextRoute);
		  }	  
	  }
	  else
	  {
		  if(infoEnabled())trace(Level.WARN,"No previous route set!!!");
	  }
	  if(infoEnabled())trace(Level.INFO,"Including route to configured SBC: "+configuredRouteHeader);
	  sPD.addHeader(configuredRouteHeader);	 	  
	  /********************************************************************************/
	  /*********** TEST **************/
	  try
	  {
//		  SipUri paidURI = (SipUri)sipProtocolData.createUri("123456789;invented=yes","mydomain.mo");
		  SipURI paidURI = mpCall.getAddressFactory().createSipURI("123456789;invented=yes","mydomain.mo");
		  if(infoEnabled())trace(Level.INFO,"PAID URI: "+paidURI);
		  AddressImpl paidAddrImp = new AddressImpl();
		  paidAddrImp.setAddess(paidURI);
		  if(infoEnabled())trace(Level.INFO,"PAID addr imp: "+paidAddrImp);
		  PAssertedIdentity restoredPaid = new PAssertedIdentity(paidAddrImp);
		  if(infoEnabled())trace(Level.INFO,"PAID header: "+restoredPaid);
		  sPD.addHeader(restoredPaid);
		  if(infoEnabled())trace(Level.INFO,"Included PAID: "+restoredPaid);
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  if(infoEnabled())trace(Level.INFO,"Could not include PAID");
	  }
	  //We're not touching neither 'to' or 'from' headers by the moment:
	  //	  Header toHeader = sPD.createHeader("To", "\"restoredbyrerouting\" <sip:"+storedInfo.getCalledPartyNumber()+">");
//	  Header toHeader = sPD.createHeader("To", "\"restoredbyrerouting\" <sip:918273645@meindomain.mo>");
	  Header toHeader = mpCall.getHeaderFactory().createHeader("To", "\"restoredbyrerouting\" <sip:918273645@meindomain.mo>");
	  sPD.addHeader(toHeader);
	  //	  sPD.addHeader(toHeader);	    			  
	  //	  if(infoEnabled())trace(Level.INFO,"To header: "+dumpObject(toHeader));
	  
//	  clParams.setAdditionalDataValue(CallLegParameters.INT_OUT_PROTOCOL_TO_USE, sPD);
//	  mpCall.createAndRouteCallLeg(newLeg, clParams);
	  //TODO mpcall.dispatchCallLeg() or sth like that
	  mpCall.dispatchEgressRequest(sPD);
	  if(infoEnabled())trace(Level.INFO,"Call Leg created and routed");		
	  return new OutgoingCallLegInfo(/*newLeg,*/ requestURI.toString());
  }
  
  /**
   * 
   * @param mpCall
   */
  private void scheduleCDRGeneration(CallActivity mpCall, CallLegType callLegType, HEC_CDR hec_cdr)
  {
//	  Hashtable<String, String> additionalInfoForCDR = hec_cdr.getCdrFieldsMap();
	  
//	  ArrayList<CallLeg> callLegs = mpCall.getCallLegs();
//	  mpCall.sendCDREvent(newLeg, ChargingConstants.CDR_TYPE_HEC, "outgoingLeg", additionalInfoForCDR);
//	  mpCall.sendCDREvent(event.getCallLeg(), ChargingConstants.CDR_TYPE_HEC, "incomingLeg", additionalInfoForCDR);
	 
//	  for (CallLeg callLeg : callLegs)
//	  {
		  try
		  {
//			mpCall.sendCDREvent(callLeg, ChargingConstants.CDR_TYPE_HEC, "callLeg"+callLeg.getCallLegId(), additionalInfoForCDR);
//			  mpCall.sendCDREvent(outgoingCallLeg, ChargingConstants.CDR_TYPE_HEC, "callLeg"+outgoingCallLeg.getCallLegId(), additionalInfoForCDR);			  
			  mpCall.sendCDREvent(callLegType, hec_cdr);
		  }
		  catch (Exception e)
		  {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  }
//	  }
	  if(infoEnabled())trace(Level.INFO, "CDRs scheduled");
  }
  
  /**
   * 
   * @param releaseCauseId
   * @param call
   * @param aci
   */
  private void releaseCall(int releaseCauseId, CallActivity call, ActivityContextInterface aci)
  {
	  try
	  {

		  statsHelper.incCounter(Constants.ct_IMS_Released);
		  
		  trace(Level.WARN, "Releasing call with cause Id ",releaseCauseId);
		  ReleaseCauseVO rc = HECLogic.getIMSReleaseCause(releaseCauseId);
		  if(rc != null)
		  {
			  trace(Level.WARN, "Release cause found: ",rc);
			  call.releaseCall(rc.getCauseCode());
		  }
		  else
		  {
			  trace(Level.WARN, "Release cause Id not found: ",releaseCauseId);
			  call.releaseCall(RC_DEFAULT);
		  }
	  } 
	  catch (Exception e)
	  {
		trace(Level.WARN, "Exception trying to find release cause: ",releaseCauseId);
		e.printStackTrace();
	  }
	  
  }
   
  /**************************************************************************/
  /***** Sample counters to measure mean execution time *********************/
  /**************************************************************************/
  
  /**
   * Required by the SLEE to manage the default usage parameters (counters)
   * @return {@link HECUsageParameters}
   */
  public abstract IMSTrunkingUsageParameters getDefaultSbbUsageParameterSet();  
  /**
   * Required by the SLEE to manage the default usage parameters (counters)
   * @return {@link HECUsageParameters}
   */
  public abstract IMSTrunkingUsageParameters getSbbUsageParameterSet(String name)
  throws javax.slee.usage.UnrecognizedUsageParameterSetNameException;    
  
  protected void incrementCounter(HECUsageCounter counter)
  {
	  try
	  {
		  IMSTrunkingUsageParameters usage = null;
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
  
  protected void sampleCounter(IMSScenario counter, long value)
  {
	  try
	  {
	    IMSTrunkingUsageParameters usage = null;
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
	    	case MO_NO_RR:
	    	case MO_RR:
	    		usage.sampleResponseTimeMO(value);
	    		break;
	    	case MT_NO_RR:
	    	case MT_RR:
	    		usage.sampleResponseTimeMT(value);
	    		break;
	    	case UCM_IN_PSTN:
	    		usage.sampleResponseTimeUCM_IN(value);
	    		break;	    		
	    	case UCM_OUT_OFFNET:
	    	case UCM_OUT_OFFNET_EMERGENCY:
	    		usage.sampleResponseTimeUCM_OUT_OFFNET(value);
	    		break;	    		
	    	case UCM_OUT_ONNET:
	    		usage.sampleResponseTimeUCM_OUT_ONNET(value);
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
}
