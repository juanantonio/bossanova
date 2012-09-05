package com.atos.ngin.hec.simpleNist.impl.callmng;

import gov.nist.javax.sip.DialogTimeoutEvent;
import gov.nist.javax.sip.ListeningPointExt;
import gov.nist.javax.sip.SipListenerExt;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.StringMsgParser;
import gov.nist.javax.sip.stack.SIPServerTransaction;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.NotCompliantMBeanException;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.SLEEException;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.EventFlags;
import javax.slee.resource.FireEventException;
import javax.slee.resource.SleeEndpoint;
import javax.slee.resource.StartActivityException;

import org.apache.log4j.Logger;

import com.atos.ngin.hec.simpleNist.event.IncomingCallEvent;
import com.atos.ngin.hec.simpleNist.impl.SimpleNISTResourceAdaptor;
import com.atos.ngin.hec.simpleNist.impl.SimpleNistProcessor;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallHistoric.CallProcessEvent;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallProcessorStats.MessageStats.MessageCounter;
import com.atos.ngin.hec.simpleNist.type.CallActivity;
import com.atos.ngin.hec.simpleNist.type.CallActivity.CallState;
import com.atos.ngin.hec.simpleNist.type.CallLeg.CallLegType;

//import test.tck.msgflow.callflows.ProtocolObjects;

public class Back2BackRouterUserAgent implements SipListenerExt, SimpleNistProcessor/* , CallProcessorMBean */
{

	private ListeningPoint[]	listeningPoints	= new ListeningPoint[2];
	private SipProvider[]		providers		= new SipProvider[2];
	private MessageFactory		messageFactory;
	// private Hashtable<Dialog,Response> lastResponseTable = new Hashtable<Dialog,Response>();
	private HeaderFactory		headerFactory;
	private AddressFactory		addressFactory;

	static AtomicLong			cdrSN			= new AtomicLong(0);

	/**
	 * 
	 * @param provider
	 * @return
	 */
	public SipProvider getPeerProvider(SipProvider provider)
	{
		if (providers[0] == provider)
			return providers[1];
		else
			return providers[0];
	}

	/**
	 * Generic request forwarder
	 * 
	 * @param incomingRequestEvent
	 * @param incomingServerTransaction
	 * @throws SipException
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public ClientTransaction forwardRequest(String method, RequestEvent incomingRequestEvent, ServerTransaction incomingServerTransaction) throws SipException, ParseException, InvalidArgumentException
	{
		SIPRequest newRequest = null;
		try
		{
			SipProvider incomingProvider = (SipProvider) incomingRequestEvent.getSource();
			Dialog incomingDialog = incomingServerTransaction.getDialog();
			CallLegImpl incomingCallLeg = (CallLegImpl) incomingDialog.getApplicationData();
			// this was really scary!
			// Dialog peerDialog = incomingCallLeg != null && incomingCallLeg.getCurrentClientTransaction() != null?
			// incomingCallLeg.getCurrentClientTransaction().getDialog():
			// (incomingCallLeg.getCurrentServerTransaction() != null?
			// incomingCallLeg.getCurrentServerTransaction().getDialog():null);

			// ...but this is no fun either:
			CallLegImpl peerCallLeg = incomingCallLeg.getType().equals(CallLegType.EGRESS) ? incomingCallLeg.getCallActivity().getIngressCallLeg() : incomingCallLeg.getCallActivity()
					.getEgressCallLeg();
			Dialog peerDialog = peerCallLeg.getDialog();

			Request request = incomingRequestEvent.getRequest();

			if (logger.isInfoEnabled())
				logger.info("Request: Incoming Dialog " + incomingDialog);
			if (logger.isInfoEnabled())
				logger.info("Request: Peer Dialog " + peerDialog);

			SipProvider peerProvider = getPeerProvider(incomingProvider);
			if (peerDialog != null)
			{
				// this includes reINVITES: we have to clone and then update the request with some headers
				SIPRequest stackProposedRequest = (SIPRequest) peerDialog.createRequest(request.getMethod());
				newRequest = (SIPRequest) request.clone();
				newRequest.setApplicationData(((SIPRequest)request).getApplicationData()); //This is not cloned				
				newRequest.setRequestURI(stackProposedRequest.getRequestURI());
				newRequest.setFrom(stackProposedRequest.getFrom());
				newRequest.setTo(stackProposedRequest.getTo());
				newRequest.setCallId(stackProposedRequest.getCallId());
				newRequest.setVia(stackProposedRequest.getViaHeaders());
				CSeqHeader cSeqHeader = stackProposedRequest.getCSeqHeader();
				long stackProposedCSeqNum = cSeqHeader.getSeqNumber();
				newRequest.setCSeq(cSeqHeader);
				newRequest.setMaxForwards(stackProposedRequest.getMaxForwards());
				newRequest.setHeader(getStackContactHeader());
				newRequest.removeHeader(RecordRouteHeader.NAME);

				RouteList rl = stackProposedRequest.getRouteHeaders();
				if (rl != null)
				{
					newRequest.setHeader(rl);
				}

				ClientTransaction newClientTransaction = peerProvider.getNewClientTransaction(newRequest);
				// Cross connect transactions: better do it prior to sending the request or we'll run into a race condition
				newClientTransaction.setApplicationData(incomingServerTransaction);
				incomingServerTransaction.setApplicationData(newClientTransaction);
				//Code for testing: let's try this before sending the request
				if(Request.INVITE.equals(method))
				{
					//					if(peerCallLeg.setPendingAckCSeq(peerDialog.getLocalSeqNumber()) != -1);					 
					if( peerCallLeg.setPendingAckCSeq(stackProposedCSeqNum) > -1)
					{
						logger.error("Setting pending Ack for a callleg already pending ACK! ");
					}
				}
				else if (Request.BYE.equals(method))
				{
					if(peerCallLeg.getPendingAckCSeq() > 0)
					{
						logger.error("Sending BYE when Pending ACK!");
					}
				}
				//////////////////
				CallProcessEvent cPEi = incomingCallLeg.setLastEvent((SIPRequest)request);
				peerDialog.sendRequest(newClientTransaction);
				cPEi.setTS3(System.currentTimeMillis());

				
				if (logger.isDebugEnabled())
					logger.debug("Request forwarded: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + newRequest);
				
				
				return newClientTransaction;
			}
			else
			{
				logger.error("Could not find peer Dialog for " + incomingRequestEvent);
			}
		}
		catch (Exception e)
		{
			logger.error("Could not forward Request: " + newRequest);
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 
	 * @param incomingRequestEvent
	 * @param incomingServerTransaction
	 * @return
	 * @throws SipException
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	public boolean forwardAck(RequestEvent incomingRequestEvent, ServerTransaction incomingServerTransaction) throws SipException, ParseException, InvalidArgumentException
	{
		SIPRequest newRequest = null;
		try
		{
			SipProvider incomingProvider = (SipProvider) incomingRequestEvent.getSource();
			Dialog incomingDialog = incomingServerTransaction.getDialog();
			Request incomingAck = incomingRequestEvent.getRequest();
			if (incomingDialog != null)
			{
				CallLegImpl incomingCallLeg = (CallLegImpl) incomingDialog.getApplicationData();
				CallLegImpl peerCallLeg = incomingCallLeg.getType().equals(CallLegType.EGRESS) ? incomingCallLeg.getCallActivity().getIngressCallLeg() : incomingCallLeg.getCallActivity()
						.getEgressCallLeg();
				Dialog peerDialog = peerCallLeg.getDialog();

				if (logger.isInfoEnabled())
					logger.info("Request: Incoming Dialog " + incomingDialog);
				if (logger.isInfoEnabled())
					logger.info("Request: Peer Dialog " + peerDialog);
				
				SipProvider peerProvider = getPeerProvider(incomingProvider);
				
				if (peerDialog != null)
				{
					//TODO: Not so easy, this seqNumber must be the one for the invite
					//					long localSeqNumber = peerDialog.getLocalSeqNumber();
					long pendingAckSeqNumber = peerCallLeg.getPendingAckCSeq();
					
					//					CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
					//					String method = cseqHeader.getMethod();
					//					long seqno = cseqHeader.getSeqNumber();
					//					if (!(incomingCallLeg.getPendingAckCSeq() > 0))
					if (!(pendingAckSeqNumber > 0))					
					{
						logger.error("Preventing ACK retransmission !!! #"+peerDialog.getDialogId());
						getCallProcessorStats().getCounters().incrementCounter(MessageCounter.ACK_RETRANS);						
					}
					else
					{					
						SIPRequest stackProposedAck = (SIPRequest) peerDialog.createAck(pendingAckSeqNumber);
						SIPRequest newAck = (SIPRequest) incomingAck.clone();
						newAck.setApplicationData(((SIPRequest)incomingAck).getApplicationData()); //This is not cloned 
						newAck.setRequestURI(stackProposedAck.getRequestURI());
						newAck.setFrom(stackProposedAck.getFrom());
						newAck.setTo(stackProposedAck.getTo());
						newAck.setCallId(stackProposedAck.getCallId());
						newAck.setVia(stackProposedAck.getViaHeaders());
						newAck.setCSeq(stackProposedAck.getCSeqHeader());
						newAck.setMaxForwards(stackProposedAck.getMaxForwards());
						newAck.removeHeader(RecordRouteHeader.NAME);
						RouteList rl = stackProposedAck.getRouteHeaders();
	
						if (rl != null)
						{
							newAck.setHeader(rl);
						}

						CallProcessEvent cPEi = incomingCallLeg.setLastEvent((SIPRequest)incomingAck);
						//						peerCallLeg.setLastEvent(newAck);						
						peerDialog.sendAck(newAck);
						//TODO this could be done when we get the number. This way it would be atomic:						
						peerCallLeg.setPendingAckCSeq(-1);
						cPEi.setTS3(System.currentTimeMillis());
						
						getCallProcessorStats().getCounters().incrementCounter(MessageCounter.ACK_SENT);
						if (logger.isInfoEnabled())
							logger.info("ACK sent: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + newAck);
						return true;
					}
				}
				else
				{
					logger.error("Cannot find peer for Dialog: " + incomingDialog);
				}
			}
			else
			{
				// These are probably related to 503-discarded INVITES in the CongestionValve
				if (logger.isDebugEnabled())
					logger.debug("Out of dialog ACK, discarding.. "+incomingAck);
				logger.error("Discarding out od dialog ACK: " + incomingAck.getHeader(CallIdHeader.NAME));
			}			
		}
		catch (Exception e)
		{
			logger.error("Could not forward Request: " + newRequest);
			e.printStackTrace();
		}
		return false;			
	}
	
	
	public void rejectIncomingCall(int code, CallLegImpl ingressCallLeg)
	{
		try
		{
			ServerTransaction serverTx = ingressCallLeg.getInitialServerTransaction();		
			if(serverTx != null)
			{
				Request request = serverTx.getRequest();
				Response response = messageFactory.createResponse(code, request);
				serverTx.sendResponse(response);				
			}
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SipException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (InvalidArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	/**
	 * 
	 */
	public Request createEgressRequest(CallActivityImpl callActivity)
	{
		try
		{
			CallLegImpl ingressCallLeg = callActivity.getIngressCallLeg();
			SIPRequest ingressInvite = (SIPRequest) ingressCallLeg.getInitialRequest();
			CallIdHeader callidheader = getEgressSipProvider().getNewCallId();
			CSeqHeader cseqheader = getHeaderFactory().createCSeqHeader((long) 2001, Request.INVITE);
			List<Via> viaList = new ArrayList<Via>();
			viaList.add((Via) getHeaderFactory().createViaHeader(getStackAddress(), getStackPort(), getStackTransport(), Utils.getInstance().generateBranchId()));

			MaxForwardsHeader maxforwardsheader = getHeaderFactory().createMaxForwardsHeader(70);

			SIPRequest egressInvite = (SIPRequest) ingressInvite.clone();
			egressInvite.setApplicationData(ingressInvite.getApplicationData()); //This is not cloned 
			// VIA
			egressInvite.setVia(viaList);
			// MAX_FORWARDS
			egressInvite.setMaxForwards(maxforwardsheader);
			// CALLID
			egressInvite.setCallId(callidheader);
			// CSEQ
			egressInvite.setCSeq(cseqheader);
			// ROUTE
			egressInvite.removeFirst(RouteHeader.NAME);
			// RECORD-ROUTE
			egressInvite.removeHeader(RecordRouteHeader.NAME);
			// CONTACT
			egressInvite.setHeader(getStackContactHeader());

			if (logger.isDebugEnabled())
				logger.debug("Created outgoing request: " + egressInvite);
			return egressInvite;
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This method is used for the first INVITE
	 * 
	 */
	public CallLegImpl dispatchEgressCallLeg(CallActivityImpl callActivity, Request egressInvite)
	{
		callProcessorStats.getCounters().incrementCounter(MessageCounter.SERVICE_ACTION);

		CallLegImpl ingressCallLeg = callActivity.getIngressCallLeg();
		try
		{
			// TODO check To and From tags: can we use the same from tag as the ingress invite?
			// FromHeader fromHeader = (FromHeader) egressInvite.getHeader(FromHeader.NAME);
			// fromHeader.setTag(Long.toString(Math.abs(new Random().nextLong())));
			// Should we remove any tag in the To header?
			// ToHeader toHeader = (ToHeader) egressInvite.getHeader(ToHeader.NAME);
			// toHeader.removeTag();

			ClientTransaction egressClientTransaction = getEgressSipProvider().getNewClientTransaction(egressInvite);

			if (logger.isDebugEnabled())
				logger.debug("Dispatching outgoing request ");
			if (egressClientTransaction != null && egressClientTransaction.getDialog() != null)
			{

				// Create egress CallLeg: this automatically registers the call leg as egress in the call activity
				CallLegImpl egressCallLeg = new CallLegImpl(callActivity, egressClientTransaction.getDialog().getCallId().getCallId(), egressClientTransaction, ra);

				// Resgister the new call leg in the calllegs table
				String outgoingCallId = egressCallLeg.getCallId();
				registerOutgoingCallLeg(outgoingCallId, egressCallLeg);

				// Set transaction level AppData: cross-connect transactions
				ServerTransaction ingressServerTransaction = ingressCallLeg.getInitialServerTransaction();
				egressClientTransaction.setApplicationData(ingressServerTransaction);
				ingressServerTransaction.setApplicationData(egressClientTransaction);

				// Set dialog level AppData: identify the call leg pertaining to each dialog
				Dialog ingressDialog = ingressCallLeg.getDialog();
				ingressDialog.setApplicationData(ingressCallLeg); // This could have been done upon request reception

				Dialog egressDialog = egressCallLeg.getDialog();
				egressDialog.setApplicationData(egressCallLeg);
				
				long pendingAckSeqNumber = egressDialog.getLocalSeqNumber();

				// Finally, send the INVITE
				CallProcessEvent cPEi = ingressCallLeg.setLastEvent((SIPRequest)egressInvite, true);				
				egressClientTransaction.sendRequest();
				cPEi.setTS3(System.currentTimeMillis());
				
				//Code for testing TODO:check it is not already set
				egressCallLeg.setPendingAckCSeq(pendingAckSeqNumber);
				//////////////////
				
				
				// TODO: add a new register in the call historic
				if (logger.isDebugEnabled())
					logger.debug("Outgoing request dispatched: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + egressClientTransaction.getRequest());
				// TODO: maybe for UDP transports we could send 100Trying right here
				return egressCallLeg;
			}
		}
		catch (TransactionUnavailableException e)
		{
			e.printStackTrace();
		}
		catch (SipException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// TODO release the ingress call leg and the call activity
		logger.error("Error dispatching outgoing request ");
		return null;

	}

	public boolean forwardResponse (ResponseEvent incomingResponseEvent, ClientTransaction clientTransaction, CallLegImpl incomingCallLeg)
	{
		try
		{
			Object appData = clientTransaction.getApplicationData();
			if(appData != null)
			{
				ServerTransaction peerServerTransaction = (ServerTransaction) appData;
				Response incomingResponse = incomingResponseEvent.getResponse();
				Request stRequest = null;
				SIPResponse stackTemplateResponse = null;
//			if(peerServerTransaction != null)
//			{
				stRequest = peerServerTransaction.getRequest();			
				
				if (logger.isInfoEnabled())
					logger.info("Response: Peer Server Transaction " + peerServerTransaction);
				
				//TODO check for reliable responses
				
				// Create the response
				stackTemplateResponse = (SIPResponse)this.messageFactory.createResponse(incomingResponse.getStatusCode(), stRequest);

				SIPResponse newResponse = (SIPResponse)incomingResponse.clone();
				newResponse.setApplicationData(((SIPResponse)incomingResponse).getApplicationData());
				newResponse.setFrom(stackTemplateResponse.getFromHeader());
				newResponse.setTo(stackTemplateResponse.getToHeader());
				newResponse.setCallId(stackTemplateResponse.getCallIdHeader());
				newResponse.setVia(stackTemplateResponse.getViaHeaders());
				newResponse.setCSeq(stackTemplateResponse.getCSeqHeader());
				//No Max-Forwards in responses
				//			newResponse.setMaxForwards(stackProposedResponse.getMaxForwards());
				newResponse.removeHeader(RecordRouteHeader.NAME);
				//Route headers?
				RouteList rl = stackTemplateResponse.getRouteHeaders();
				if (rl != null)
				{
					newResponse.setHeader(rl);
				}			
				SipProvider provider = (SipProvider) incomingResponseEvent.getSource();
				SipProvider peerProvider = this.getPeerProvider(provider);
				ListeningPoint peerListeningPoint = peerProvider.getListeningPoint(stackTransport);
				ContactHeader peerContactHeader = ((ListeningPointExt) peerListeningPoint).createContactHeader();
				newResponse.setHeader(peerContactHeader);

				CallProcessEvent cPEi = incomingCallLeg.setLastEvent((SIPResponse)incomingResponse);
				//				CallLegImpl peerCallLeg = incomingCallLeg.getType().equals(CallLegType.EGRESS) ? incomingCallLeg.getCallActivity().getIngressCallLeg() : incomingCallLeg.getCallActivity()
				//						.getEgressCallLeg();				
				//				CallProcessEvent cPEo = peerCallLeg.setLastEvent(newResponse);
				peerServerTransaction.sendResponse(newResponse);
				cPEi.setTS3(System.currentTimeMillis());
					
				
				if (logger.isDebugEnabled())
					logger.debug("Response sent for request method " + stRequest.getMethod() + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + newResponse);			
				return true;
				
			}
			else
			{
				logger.error("Cannot get applicationData from Client Tx: "+clientTransaction.getState());
				logger.error("This is what we got: "+clientTransaction.getApplicationData());
			}
			
		}
		catch (SipException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("SipException creating a response to forward");
		}
		catch (InvalidArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("InvalidArgumentException creating a response to forward");
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("ParseException creating a response to forward");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Exception creating a response to forward. CL Tx state: "+clientTransaction.getState());
		}		
		return false;
	}
	public void processDialogTimeout(DialogTimeoutEvent timeoutEvent)
	{
		try
		{
			getCallProcessorStats().getCounters().incrementCounter(MessageCounter.DIALOG_TIMEOUT);
			ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
			//AckNotSent, for example:
			logger.error("Dialog timeout "+timeoutEvent.getReason()+" CL Tx "+clientTransaction);
			Dialog dialog = timeoutEvent.getDialog();
			CallLegImpl callLeg = (CallLegImpl)dialog.getApplicationData();
			if (clientTransaction != null)
			{
				ServerTransaction peerServerTransaction = (ServerTransaction)clientTransaction.getApplicationData();
			}
			//TODO what do we do in this case?
			//is this needed?
			// timeoutEvent.getClientTransaction().setApplicationData(null);
		}
		catch(Exception e)
		{
			logger.error("Error processing dialog timeout: "+e);
			e.printStackTrace();
		}
	}

	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent)
	{
		//No transactions here
		try
		{			
			getCallProcessorStats().getCounters().incrementCounter(MessageCounter.DIALOG_TERMINATED);
			Dialog dialog = dialogTerminatedEvent.getDialog();
			if(logger.isDebugEnabled())logger.debug("Dialog terminated "+dialog);
			
			CallLegImpl callLeg = (CallLegImpl)dialog.getApplicationData();
			//TODO some stuff
			dialog.setApplicationData(null);	
		}
		catch(Exception e)
		{
			logger.error("Error processing dialog terminated: "+e);
			e.printStackTrace();
		}
	}

	public void processIOException(IOExceptionEvent exceptionEvent)
	{
		getCallProcessorStats().getCounters().incrementCounter(MessageCounter.IOEXCEPTION);
		logger.error("IOException "+exceptionEvent.getHost()+":"+exceptionEvent.getPort()+":"+exceptionEvent.getTransport());
	}

	public void processTimeout(TimeoutEvent timeoutEvent)
	{
		try
		{
			getCallProcessorStats().getCounters().incrementCounter(MessageCounter.TIMEOUT);
			if(timeoutEvent.isServerTransaction())
			{
				ServerTransaction serverTransaction = timeoutEvent.getServerTransaction();
				logger.error(timeoutEvent.getTimeout()+" SV Tx: "+serverTransaction);
				Dialog dialog = serverTransaction.getDialog();
				CallLegImpl callLeg = (CallLegImpl)dialog.getApplicationData();
				ClientTransaction peerClientTransaction = (ClientTransaction)serverTransaction.getApplicationData();
				//TODO some stuff: this is an ACK for 3xx-6xx not being received 
				serverTransaction.setApplicationData(null);
			}
			else
			{
				ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
				logger.error(timeoutEvent.getTimeout()+" CL Tx: "+clientTransaction);
				Dialog dialog = clientTransaction.getDialog();
				CallLegImpl callLeg = (CallLegImpl)dialog.getApplicationData();
				ServerTransaction peerServerTransaction = (ServerTransaction)clientTransaction.getApplicationData();
				//TODO some stuff: i.e send an error response to the peer 
				clientTransaction.setApplicationData(null);
			}
		}
		catch(Exception e)
		{
			logger.error("Error processing transaction timeout: "+e);
			e.printStackTrace();
		}

		
	}

	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent)
	{
		try
		{					
			getCallProcessorStats().getCounters().incrementCounter(MessageCounter.TRANSACION_TERMINATED);
			if(transactionTerminatedEvent.isServerTransaction())
			{			
				ServerTransaction serverTransaction = transactionTerminatedEvent.getServerTransaction();
				if(logger.isDebugEnabled())logger.debug("Transaction terminated SV Tx: "+serverTransaction);				
				Dialog dialog = serverTransaction.getDialog();
				CallLegImpl callLeg = (CallLegImpl)dialog.getApplicationData();
				ClientTransaction peerClientTransaction = (ClientTransaction)serverTransaction.getApplicationData();	
				//TODO some stuff
				serverTransaction.setApplicationData(null);
			}
			else
			{
				ClientTransaction clientTransaction = transactionTerminatedEvent.getClientTransaction();
				if(logger.isDebugEnabled())logger.debug("Transaction terminated CL Tx: "+clientTransaction);				
				Dialog dialog = clientTransaction.getDialog();
				CallLegImpl callLeg = (CallLegImpl)dialog.getApplicationData();
				ServerTransaction peerServerTransaction = (ServerTransaction)clientTransaction.getApplicationData();		
				//TODO some stuff
				clientTransaction.setApplicationData(null);
			}
		}
		catch(Exception e)
		{
			logger.error("Error processing transaction timeout: "+e);
			e.printStackTrace();
		}		
		
	}

	/**
	 * 
	 */
	public void processRequest(RequestEvent requestEvent)
	{
		try
		{
			SIPRequest request = (SIPRequest)requestEvent.getRequest();
			Object appData = request.getApplicationData();
			if(appData != null)
			{
				((Long[])appData)[1]= System.currentTimeMillis();
			}
			if (logger.isInfoEnabled())
				logger.info("Request: " + request.getMethod());
			if (logger.isDebugEnabled())
				logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n " + request);
			SipProvider provider = (SipProvider) requestEvent.getSource();
			String method = request.getMethod(); 
			if (Request.INVITE.equals(method))
			{
				if (requestEvent.getServerTransaction() == null)
				{
					try
					{
						callProcessorStats.getCounters().incrementCounter(MessageCounter.FIRST_INVITE);						
						ServerTransaction serverTx = provider.getNewServerTransaction(request);
						// this.forwardRequest(requestEvent,serverTx);
						CallActivityImpl callActivity = null;
						if ((callActivity = processIncomingCall(serverTx, request)) == null)
						{
							if (logger.isDebugEnabled())logger.debug("Rejecting the request: no service available" );
							Response response = messageFactory.createResponse(503, request);
							serverTx.sendResponse(response);							
						}
//						else
//						{
//							// So we use app data even when the call is rejected (TODO this whole mechanism must be improved)
//							Dialog dialog = serverTx.getDialog();
//							dialog.setApplicationData(callActivity.getIngressCallLeg());
//							//let the service, if any, process the request
//						}
					}
					catch (TransactionAlreadyExistsException ex)
					{
						logger.error("Transaction exists -- ignoring");
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				else
				{
					// This must be a reINVITE
					callProcessorStats.getCounters().incrementCounter(MessageCounter.REINVITE);
					this.forwardRequest(method, requestEvent, requestEvent.getServerTransaction());
				}
			}
			else if (Request.BYE.equals(method))
			{
				ServerTransaction serverTransaction = requestEvent.getServerTransaction();
				if (serverTransaction == null)
				{
					serverTransaction = provider.getNewServerTransaction(request);
					logger.error("New transaction had to be created for a BYE");
				}
				else
				{
					if(logger.isDebugEnabled())logger.debug("BYE within transaction received");
				}
				this.forwardRequest(method, requestEvent, serverTransaction);
				getCallProcessorStats().getCounters().incrementCounter(MessageCounter.BYE);

			}
			else if (Request.ACK.equals(method))
			{
				getCallProcessorStats().getCounters().incrementCounter(MessageCounter.ACK);
				SIPServerTransaction serverTransaction = (SIPServerTransaction)requestEvent.getServerTransaction();
				if (serverTransaction == null)
				{
					logger.error("Out of transaction ACK: "+request.getHeader(CallIdHeader.NAME));
				}
				else
				{
					if(logger.isDebugEnabled())logger.debug("ACK within transaction received");
//					if(!serverTransaction.ackSeen())
//					{
					TransactionState ts = serverTransaction.getState();
				    if(!forwardAck(requestEvent, serverTransaction))
				    {
				    	logger.error("Could not forward ACK. ST state: "+ts);
				    }
					
//					}
//					else
//					{
//						//This must be a retransamission: do not forward
//						logger.error("ACK retransmission received: "+serverTransaction.getState());
//					}
				}
				
			}
			else if (Request.UPDATE.equals(method))
			{
				ServerTransaction serverTransaction = requestEvent.getServerTransaction();
				if (serverTransaction == null)
				{
					serverTransaction = provider.getNewServerTransaction(request);
					logger.error("New transaction had to be created for a BYE");
				}
				else
				{
					if(logger.isDebugEnabled())logger.debug("UPDATE within transaction received");
				}
				this.forwardRequest(method, requestEvent, serverTransaction);
				getCallProcessorStats().getCounters().incrementCounter(MessageCounter.UPDATE);

			}
			else
			{
				getCallProcessorStats().getCounters().incrementCounter(MessageCounter.OTHER);
				if(logger.isDebugEnabled())logger.debug("Unsupported method: "+method);
			}

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void processResponse(ResponseEvent responseEvent)
	{
		try
		{
			SIPResponse incomingResponse = (SIPResponse)responseEvent.getResponse();
			Object appData = incomingResponse.getApplicationData();
			if(appData != null)
			{
				((Long[])appData)[1]= System.currentTimeMillis();
			}			
			Dialog incomingDialog = responseEvent.getDialog();
			ClientTransaction incomingClientTransaction = responseEvent.getClientTransaction();
			if (logger.isInfoEnabled())
				logger.info("Response: " + incomingResponse.getStatusCode());
			if (logger.isInfoEnabled())
				logger.info("Response Dialog: " + incomingDialog);
			if (logger.isInfoEnabled())
				logger.info("Response ClientTransaction: " + incomingClientTransaction);
			if (logger.isDebugEnabled())
				logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n " + incomingResponse);
			CallLegImpl incomingCallLeg = (CallLegImpl) incomingDialog.getApplicationData();
			//We are doing this twice!
			if(incomingClientTransaction != null && incomingCallLeg != null)
			{
				//If the transaction is already finished, some of these things can be null
				//TODO, also, there must be a better way to do this detection of call ending
				//				ServerTransaction peerServerTransaction = (ServerTransaction) incomingClientTransaction.getApplicationData();
				//				Request stRequest = peerServerTransaction.getRequest();	 
				//				String method = stRequest.getMethod();

				
				if(forwardResponse(responseEvent, incomingClientTransaction, incomingCallLeg))
				{
					
					getCallProcessorStats().getCounters().incrementCounter(MessageCounter.RESPONSE);
					
					// Call state management:
					int statusCode = incomingResponse.getStatusCode();				
					if (statusCode / 100 > 1)
					{
						//we are moving this to the callback for terminating transaction
						//				responseEvent.getClientTransaction().setApplicationData(null);
					}
					CSeqHeader cseq = incomingResponse.getCSeqHeader();//(CSeqHeader)incomingResponse.getHeader(CSeqHeader.NAME);
					String method = cseq.getMethod();
		
					if (method.equals("BYE"))
					{
						cleanUp(incomingDialog);
					}
				}
				else //The response could not be forwarded
				{
					//TODO: error treatment
					logger.error("Process Response: could not forward respones");
				}
			}
			else
			{
				if(incomingCallLeg != null)
				{
					//TODO something}		
					logger.error("Process Response: no incoming CT but we have dialog and callLeg. Probably a retransmission. CallId:"
							+incomingResponse.getCallId()+" / CSeq:"+incomingResponse.getCSeqHeader()+" / LastEvent: "+incomingCallLeg.getLastEvent());
				}
				else
				{
					//TODO What?
					logger.error("Process Response: no incoming CT or CallLeg ");
				}
			}

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			logger.error("Exception processing response: "+ex);
		}
	}

	

	private static Pattern patternSIPNameAddressTrigger;
	static
	{
		patternSIPNameAddressTrigger = Pattern.compile("(?:<(?:.*);call=([^;^@^<^>]*)(?:;[^;^>]*)*>)");				
	}	
	/**
	 * Look what we had to use..
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
	public String getTrigger (String routeUri)
	{		
		Matcher sipAddressMatcher = patternSIPNameAddressTrigger.matcher(routeUri);
		try
		{
			if(sipAddressMatcher.find() && sipAddressMatcher.groupCount() >= 1)
			{
				return sipAddressMatcher.group(1);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param serverTransaction
	 * @param request
	 * @return
	 */
	private CallActivityImpl processIncomingCall(ServerTransaction serverTransaction, SIPRequest request)
	{

		try
		{
			//TODO: check trigger. Is this optimal?
			RouteList routeList = request.getRouteHeaders();
			Route firstRoute = null;
			String trigger = null;
			if(routeList != null && (firstRoute = (Route)routeList.getFirst()) != null)
			{				
				//1st option
				//				firstRoute.setMatcher(matchExpression);
				//				firstRoute.getMatcher();
				//2nd option
				trigger = getTrigger(firstRoute.getHeaderValue());

				if(logger.isDebugEnabled())logger.debug("Processing first route: "+firstRoute+ " / PV: "+trigger);

			}
			//TODO: check the trigger is within a configured list 
			if(trigger != null)
			{
				if(logger.isDebugEnabled())logger.debug("Trigger found: "+trigger);
				
				String rqCallId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
				
				CallLegImpl incomingCallLeg = new CallLegImpl(rqCallId, serverTransaction, ra);

				CallActivityImpl callActivity = new CallActivityImpl(rqCallId, incomingCallLeg, ra, this);
				incomingCallLeg.setCallActivity(callActivity);
				CallActivityHandle callActivityHandle = callActivity.getCallActivityHandle();
	
				SleeEndpoint endpoint = ra.getEndpoint();
				endpoint.startActivity(callActivityHandle, callActivity);
				/////////////
				//				Object appData = request.getApplicationData();
				//				if(appData != null && appData instanceof Long[])
				//				{
				//					if(logger.isDebugEnabled())logger.debug("Received appData TS: "+((Long[])appData)[0]);
				//				}
				//				else
				//				{
				//					logger.error("No appData!!: "+appData);
				//				}
				////////////
				IncomingCallEvent incomingCallEvent = new IncomingCallEvent(request, trigger/* , callActivity */);
				endpoint.fireEvent(callActivityHandle, ra.incomingCallEvent, incomingCallEvent, null, null, EventFlags.REQUEST_PROCESSING_SUCCESSFUL_CALLBACK
						| EventFlags.REQUEST_PROCESSING_FAILED_CALLBACK);
				callLegs.put(rqCallId, incomingCallLeg);
				calls.put(callActivityHandle.toString(), callActivity);
				callActivity.setState(CallState.PENDING_SA);
				return callActivity;
			}
		}
		catch (ActivityAlreadyExistsException e)
		{
			// TODO Auto-generated catch block
			logger.warn("ActivityAlreadyExistsException!!: ");
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			// TODO Auto-generated catch block
			logger.warn("NullPointerException!!: ");
			e.printStackTrace();
		}
		catch (IllegalStateException e)
		{
			// TODO Auto-generated catch block
			logger.warn("IllegalStateException!!: ");
			e.printStackTrace();
		}
		catch (SLEEException e)
		{
			// TODO Auto-generated catch block
			logger.warn("SLEEException!!: ");
			e.printStackTrace();
		}
		catch (StartActivityException e)
		{
			// TODO Auto-generated catch block
			logger.warn("StartActivityException!!: ");
			e.printStackTrace();
		}
		catch (FireEventException e)
		{
			// TODO Auto-generated catch block
			logger.warn("FireEventException!!: ");
			e.printStackTrace();
		}
		return null;
	}

	// TODO: instead of calling this, we should notify the CallActivity and let it clean callLegs and slee resources and dispatch the CDR
	private void cleanUp(Dialog dialog)
	{
		// if(logger.isInfoEnabled())logger.info("Should we clean?: "+getStackState());
		if (dialog != null)
		{
			// Dialog peerDialog = (Dialog)dialog.getApplicationData();
			CallLegImpl incomingCallLeg = (CallLegImpl) dialog.getApplicationData();
			CallLegImpl peerCallLeg = incomingCallLeg.getType().equals(CallLegType.EGRESS) ? incomingCallLeg.getCallActivity().getIngressCallLeg() : incomingCallLeg.getCallActivity()
					.getEgressCallLeg();
			
			if(peerCallLeg != null)
			{
				Dialog peerDialog = peerCallLeg.getDialog();				
				if (peerDialog != null)
				{
					peerDialog.setApplicationData(null);
	//				this.lastResponseTable.remove(peerDialog);
				}
			}
//			this.lastResponseTable.remove(dialog);
			dialog.setApplicationData(null);
			CallActivityImpl callActivity = incomingCallLeg.getCallActivity();
			callActivity.terminate();
		}
		getCallProcessorStats().getCounters().incrementCounter(MessageCounter.ACTIVITY_TERMINATED);
	}


	private Logger										logger				= null;
	SipStack											sipStack			= null;
	ListeningPoint										lp1					= null, lp2 = null;
	SipProvider											sp1					= null, sp2 = null;
	String												stackAddress		= "127.0.0.1";
	int													stackPortIngress	= 6060;
	int													stackPortEgress		= 6070;
	String												stackTransport		= "udp";

	private CallProcessorStats							callProcessorStats;
	private SimpleNISTResourceAdaptor					ra;

	private ConcurrentHashMap<String, CallLegImpl>		callLegs			= new ConcurrentHashMap<String, CallLegImpl>(400000);
	private ConcurrentHashMap<String, CallActivityImpl>	calls				= new ConcurrentHashMap<String, CallActivityImpl>(200000);

	public CallLegImpl removeCallLeg(String callLegKey)
	{
		return callLegs.remove(callLegKey);
	}
	public CallActivityImpl removeCall(String callKey)
	{
		return calls.remove(callKey);
	}	
	public Back2BackRouterUserAgent(SimpleNISTResourceAdaptor ra)
	{
		this.ra = ra;
		try
		{
			logger = Logger.getLogger(ra.getRaContext().getEntityName() + ".Back2BackUserAgent");
			callProcessorStats = new CallProcessorStats((SimpleNistProcessor) this);
			logger.info("Call Processor Statistics MBean created");
		}
		catch (NotCompliantMBeanException e)
		{
			// TODO Auto-generated catch block
			logger.error("Could not create MBean for the Call Processor");
			e.printStackTrace();
		}
	}

	public SipStack init(String configFilePath, SimpleNISTResourceAdaptor ra)
	{

		SipFactory sipFactory = null;

		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		Properties properties = new Properties();
		// this.protocolObjects = new ProtocolObjects("backtobackua","gov.nist","udp",true,true, false);
		// try retrieve data from file
		try
		{
			if (logger.isInfoEnabled())
				logger.info("Loading stack properties file: " + configFilePath);
			properties.load(new FileInputStream(configFilePath));

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		try
		{
			if (logger.isInfoEnabled())
				logger.info("Stack properties: " + properties.toString());

			stackAddress = properties.getProperty("com.atos.cc.IP_ADDRESS");
			stackTransport = properties.getProperty("com.atos.cc.STACK_TRANSPORT");
			stackPortIngress = Integer.parseInt(properties.getProperty("com.atos.cc.STACK_PORT"));
			stackPortEgress = stackPortIngress + 10;

			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
			sipStack = sipFactory.createSipStack(properties);
			lp1 = sipStack.createListeningPoint(stackAddress, stackPortIngress, stackTransport);
			lp2 = sipStack.createListeningPoint(stackAddress, stackPortEgress, stackTransport);
			sp1 = sipStack.createSipProvider(lp1);
			sp2 = sipStack.createSipProvider(lp2);
			this.listeningPoints[0] = lp1;
			this.listeningPoints[1] = lp2;
			this.providers[0] = sp1;
			this.providers[1] = sp2;
			sp1.addSipListener(this);
			sp2.addSipListener(this);

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return sipStack;
	}

	/**
	 * Shutdown
	 */
	public void shutdown() throws Exception
	{
		sipStack.stop();
		sp1.removeSipListener(this);
		sp2.removeSipListener(this);
		sp1.removeListeningPoint(lp1);
		sp2.removeListeningPoint(lp2);
		sipStack.deleteListeningPoint(lp1);
		sipStack.deleteListeningPoint(lp2);
		sipStack.deleteSipProvider(sp1);
		sipStack.deleteSipProvider(sp2);

		sp1 = null;
		sp2 = null;
		lp1 = null;
		lp2 = null;
		messageFactory = null;
		headerFactory = null;
		addressFactory = null;

		sipStack = null;

		// This way the stack does not remain cached in the factory, thus
		// keeping old properties when the RA is restarted.
		// This is to clean up old stacks: how does this work with several entities?
		SipFactory sipFactory = SipFactory.getInstance();
		sipFactory.resetFactory();
	}

	public String terminatePendingCalls()
	{
		int i = 0;
		Iterator<CallActivityImpl> callList = calls.values().iterator();
		while(callList.hasNext())
		{
			i++;
			try
			{
				CallActivityImpl call = callList.next();
				call.terminate();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return "Terminated "+i+" calls";
	}
	public CallProcessorStats getCallProcessorStats()
	{
		return callProcessorStats;
	}

	public void registerOutgoingCallLeg(String callId, CallLegImpl outgoingCallLeg)
	{
		callLegs.put(callId, outgoingCallLeg);
	}

	public HeaderFactory getHeaderFactory()
	{
		return headerFactory;
	}

	public MessageFactory getMessageFactory()
	{
		return messageFactory;
	}

	ContactHeader	stackContactHeader	= null;

	public ContactHeader getStackContactHeader()
	{
		if (stackContactHeader == null)
		{
			try
			{
				SipURI contactURI = null;
				contactURI = addressFactory.createSipURI("NISTStack", stackAddress);
				contactURI.setPort(stackPortIngress);
				Address contactAddress = addressFactory.createAddress(contactURI);
				contactAddress.setDisplayName("simpleNISTStack");
				stackContactHeader = headerFactory.createContactHeader(contactAddress);

			}
			catch (ParseException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return stackContactHeader;
	}

	public String getStackAddress()
	{
		return stackAddress;
	}

	public int getStackPort()
	{
		return stackPortIngress;
	}

	public String getStackTransport()
	{
		return stackTransport;
	}

	public SipStack getSipStack()
	{
		return sipStack;
	}

	public int getCPEOccupancy()
	{
		// TODO Auto-generated method stub
		return -1;
	}

	public int getSAOccupancy()
	{
		// TODO Auto-generated method stub
		return -1;
	}

	public int getCDRQueueOccupancy()
	{
		return ra.getCDRQueueOccupancy();
	}

	public AddressFactory getAddressFactory()
	{
		// TODO Auto-generated method stub
		return addressFactory;
	}

	public SipProvider getIngressSipProvider()
	{
		return sp1;
	}

	public SipProvider getEgressSipProvider()
	{
		return sp2;
	}

	static AtomicLong	sn	= new AtomicLong(0);

	public long incrementAndGetSerialNumber()
	{
		return sn.incrementAndGet();
	}

	public long getSerialNumber()
	{
		return sn.longValue();
	}

	public CallActivity findCallActivity(ActivityHandle handle)
	{
		return calls.get(handle.toString());
	}
}
