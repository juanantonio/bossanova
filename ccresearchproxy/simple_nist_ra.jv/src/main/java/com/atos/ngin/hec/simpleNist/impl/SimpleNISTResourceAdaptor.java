package com.atos.ngin.hec.simpleNist.impl;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.slee.Address;
import javax.slee.EventTypeID;
import javax.slee.UnrecognizedEventException;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.EventFlags;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;
import javax.slee.resource.SleeEndpoint;

import com.atos.ngin.hec.simpleNist.event.IncomingCallEvent;
import com.atos.ngin.hec.simpleNist.impl.alarms.WriterAlarmsHandler;
//import com.atos.ngin.hec.simpleNist.impl.callmng.CallProcessor;
import com.atos.ngin.hec.simpleNist.impl.callmng.Back2BackRouterUserAgent;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallActivityImpl;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallHistoric;
import com.atos.ngin.hec.simpleNist.impl.callmng.CallProcessorMBean;

import com.atos.ngin.hec.simpleNist.type.CallActivity;
import com.atos.ngin.hec.simpleNist.type.GenericCDR;


// import com.opencloud.rhino.alarm.AlarmManager;
// import com.opencloud.rhino.resourceadaptor.RhinoBootstrapContext;

public class SimpleNISTResourceAdaptor implements ResourceAdaptor, Serializable
{
	private static final long serialVersionUID = 3541724059797717279L;
	
	// --- Constants
	private final int ST_UNCONFIGURED               = 1;
	private final int ST_INACTIVE                   = 2;
	private final int ST_ACTIVE                     = 3;
	private final int ST_STOPPING                   = 4;
	private final String PARAM_ConfigFile           = "ConfigFile";
	private final String PARAM_CdrFolderPath	    = "CdrFolderPath";
	private final String PARAM_fileNameSuffix       = "CdrFileNameSuffix";
	private final String PARAM_ExampleB2B       	= "ExampleB2B";	
	
	// SLEE resources
	private transient SleeEndpoint           endpoint;
	private transient ResourceAdaptorContext raContext;
	private transient AlarmFacility          alarmFacility;
	private transient Tracer                 trace;
	private transient WriterProviderImpl     writerProvider;
	 
	// CDR Reader own resources
	private transient WriterAlarmsHandler    alarmsHandler;
	
	//JMX resources
	private transient MBeanServer 			 mBeanServer;
	private transient ObjectName 			 genericMBeanIdentity;
	private transient ObjectName 			 callProcessorStatsMBeanIdentity;
	//private transient CdrConsumer cdrConsumer;

	private transient SimpleNistProcessor 	callProcessor;
	private transient CDRWriter      			cdrWriter;
	private transient CDRWriter      			callHistWriter;	
	private transient int         			raState;
	
	// Configuration variables
	private String    configFilePath;
	private String 	fileNameSuffix;
	private String 	cdrFolderPath;
	private File 		workFolder;
	private Boolean     exampleB2B;
	 
	private Marshaler marshaler;
	
	// -- Events
	private static final String EV_VERSION = "1.0";  
	private static final String EV_VENDOR  = "AtoS";
	private static final String EV_PATH    = "com.atos.ngin.hec.simpleNist.event.";
	
	private static final String INCOMING_CALL_STR = "IncomingCallEvent";
	private static final String INCOMING_CALL_EV = EV_PATH + INCOMING_CALL_STR;
	  
	public transient FireableEventType                   incomingCallEvent;  
  
	//	/**
	//	* Sip stack
	//	*/
	////	private SipStack sipStack;
	//	/**
	//	* Sip factory
	//	*/
	//	private static SipFactory sipFactory;
	//	
	//	/**
	//	* Header factory
	//	*/
	//	public static HeaderFactory headerFactory;
	//	
	//	/**
	//	* Address factory
	//	*/
	//	public static AddressFactory addressFactory;
	//	
	//	/**
	//	* Message factory
	//	*/
	//	public static MessageFactory messageFactory;
	//	
	//	static
	//	{
	//		sipFactory = SipFactory.getInstance();
	//		try {
	//			headerFactory = sipFactory.createHeaderFactory();
	//		} catch (PeerUnavailableException e) {
	//			e.printStackTrace();
	//			headerFactory = null;
	//		}
	//		
	//		try {
	//			addressFactory = sipFactory.createAddressFactory();
	//		} catch (PeerUnavailableException e) {
	//			e.printStackTrace();
	//			addressFactory = null;
	//		}
	//		
	//		try {
	//			messageFactory = sipFactory.createMessageFactory();
	//		} catch (PeerUnavailableException e) {
	//			e.printStackTrace();
	//			messageFactory = null;
	//		}
	//	}
  
  // --------------------------------------------------------------------------
  // ----------------- ResourceAdaptor implementation methods -----------------
  // --------------------------------------------------------------------------

  /**
   * Set the <code>ResourceAdaptorContext</code> object for the resource
   * adaptor object. The SLEE invokes this method immediately after a new
   * resource adaptor object has been created. The
   * <code>ResourceAdaptorContext</code> object passed to the resource adaptor
   * object via this method provides the resource adaptor object with access to
   * SLEE Facilities. If the resource adaptor object needs to use the
   * <code>ResourceAdaptorContext</code> object during its lifetime, it should
   * store the <code>ResourceAdaptorContext</code> object reference in an
   * instance variable.
   * <p>
   * The resource adaptor object enters the Unconfigured state after this
   * method returns.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param context the <code>ResourceAdaptorContext</code> object given to the
   *          resource adaptor object by the SLEE.
   */
  public void setResourceAdaptorContext(ResourceAdaptorContext context)
  {
    raState = ST_UNCONFIGURED;
    raContext = context;
    endpoint = context.getSleeEndpoint();
    trace = raContext.getTracer(this.getClass().getSimpleName());
    if (trace.isInfoEnabled()) trace.info("Resource adaptor context set "
        + raContext);
    
    // JMX interface management: only as an example
    try
    {
	    String identityStr = "com.atos.slee.ra:type=" + context.getEntityName();
	    if (trace.isInfoEnabled())trace.info("Identity: "+identityStr);
	
	    genericMBeanIdentity = new ObjectName( identityStr);    
	    mBeanServer = ManagementFactory.getPlatformMBeanServer();
	    SimpleNistMBeanImpl mbean = new SimpleNistMBeanImpl();
	    mBeanServer.registerMBean( mbean, genericMBeanIdentity );      
    }
    catch (MalformedObjectNameException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
    catch (NotCompliantMBeanException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
	catch (InstanceAlreadyExistsException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
	catch (MBeanRegistrationException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
  }

  /**
   * Unset the <code>ResourceAdaptorContext</code> object for the resource
   * adaptor object. If the resource adaptor object stored a reference to the
   * <code>ResourceAdaptorContext</code> object given to it in the
   * {@link #setResourceAdaptorContext setResourceAdaptorContext} method, the
   * resource adaptor object should clear that reference during this method.
   * <p>
   * This method is invoked on a resource adaptor object in the Unconfigured
   * state and is the last method invoked on a resource adaptor object before
   * it becomes a candidate for garbage collection.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   */
  public void unsetResourceAdaptorContext()
  {
    if (trace.isInfoEnabled()) trace.info("Resource adaptor context unset "
        + raContext);
    try
    {
	    mBeanServer.unregisterMBean(genericMBeanIdentity);      
    }
	catch (MBeanRegistrationException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}    
    raContext = null;
  }

  /**
   * The SLEE invokes this method on a resource adaptor object in the
   * Unconfigured state to provide the resource adaptor object with
   * configuration properties for the resource adaptor entity. This may occur
   * when a new resource adaptor entity is created, or for existing resource
   * adaptor entities during SLEE startup.
   * <p>
   * The resource adaptor object transitions to the Inactive state after this
   * method returns.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param properties the configuration properties specified for the resource
   *          adaptor entity.
   */
  public void raConfigure(ConfigProperties properties)
  {

//    if (trace.isFinestEnabled()) trace.finest("  Creating alarm manager...");
//    this.alarmsHandler = WriterAlarmsHandler.getInstance(this);
//
    if (trace.isFinestEnabled()) trace.finest("  Loading configuration...");
    loadConfig(properties);

    if (trace.isInfoEnabled()) trace.info("Resource adaptor configured "
        + properties);
    raState = ST_INACTIVE; // TODO check previous state
    marshaler = new NistSipMarshaler(raContext);
    EventTypeID evID = null;
    try
    {
    	trace.info("  Creating events...");
		EventLookupFacility elf = raContext.getEventLookupFacility(); 
		evID = new EventTypeID(INCOMING_CALL_EV, EV_VENDOR, EV_VERSION); 
		incomingCallEvent = elf.getFireableEventType(evID);       
		trace.info("  Event created "+incomingCallEvent);
    
    }
    catch (UnrecognizedEventException uee) 
    {
    	trace.severe("No event ID found for " + evID + ". Cannot initialise");
    	uee.printStackTrace();
    }
    
  }

  /**
   * The SLEE invokes this method on a resource adaptor object in the Inactive
   * state in order to unconfigure it. Typically this occurs when the
   * corresponding resource adaptor entity is being removed from the SLEE and
   * so the resource adaptor object is no longer required. The implementation
   * of this method should release any resources allocated by the resource
   * adaptor object during the {@link #raConfigure raConfigure} method.
   * <p>
   * The resource adaptor object transitions to the Unconfigured state after
   * this method returns.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   */
  public void raUnconfigure()
  {
    raState = ST_UNCONFIGURED; // TODO check previous state
    if (trace.isInfoEnabled()) trace.info("Resource adaptor unconfigured ");
  }

  /**
   * The SLEE invokes this method on a resource adaptor object when it
   * transitions from the Inactive state to the Active state. It is used to
   * notify the resource adaptor object that it may now start to generate
   * activities and events. The SLEE invokes this method when the following
   * conditions both become true:
   * <ol>
   * <li>The SLEE is in the Running state.
   * <li>The corresponding resource adaptor entity is in the Active state.
   * </ol>
   * <p>
   * The resource adaptor object transitions to the Active state before this
   * method is invoked, allowing it to start activities during this method
   * invocation if required.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   */
  public void raActive()
  {
    try
    {
      cdrWriter = new CDRWriter(this, getCDRConfiguration());
      callHistWriter = new CDRWriter(this, getCHConfiguration());

      if (exampleB2B)
      {
          trace.info("  Creating Back2BackUserAgent...");
    	  callProcessor = new Back2BackRouterUserAgent(this);
    	  callProcessorStatsResgistry(callProcessor.getCallProcessorStats(), true);
      }
      else
      {
    	  trace.info("  Creating CallProcessor...");
    	  //	      callProcessor = new CallProcessor(this);
	      callProcessor = new Back2BackRouterUserAgent(this);
	      callProcessorStatsResgistry(callProcessor.getCallProcessorStats(), true);
      }
      callProcessor.init(configFilePath, this);
      trace.info("  ...Call Processor created, registered and started");
      
      trace.info("  Creating writer provider...");
      this.writerProvider = new WriterProviderImpl(this);   
      
      cdrWriter.start();
      callHistWriter.start();
      if (trace.isFinestEnabled()) trace.finest("CDR writer started");
      raState = ST_ACTIVE; // TODO check previous state
      if (trace.isInfoEnabled()) trace.info("Resource adaptor activated ");
    }
    catch (Exception e)
    {
      // throw new ResourceException("Unable to activate RA entity", e);
      e.printStackTrace();
      trace.severe("Exception when activating resource adaptor");
    }
  }

  /**
   * The SLEE invokes this method on a resource adaptor object in the Active
   * state to notify the resource adaptor object that it must stop generating
   * new activities and begin to cleanup any existing activities. The SLEE
   * invokes this method when either of the following conditions become true:
   * <ol>
   * <li>The SLEE transitions to the Stopping state.
   * <li>The corresponding resource adaptor entity transitions to the Stopping
   * state.
   * </ol>
   * <p>
   * During this method the resource adaptor object should alter its internal
   * state so that it does not attempt to start new activities once this method
   * returns. The resource adaptor object transitions to the Stopping state
   * after this method returns.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   */
  public void raStopping()
  {
    if (trace.isInfoEnabled()) trace.info("Resource adaptor stopping ");
    raState = ST_STOPPING; // TODO check previous state
    // TODO No new activities from now on
  }

  /**
   * The SLEE invokes this method on a resource adaptor object in the Stopping
   * state once all activities owned by the resource adaptor object have ended
   * in the SLEE. The implementation of this method should release any
   * resources allocated by the resource adaptor object during the
   * {@link #raActive raActive} method.
   * <p>
   * The resource adaptor object transitions to the Inactive state after this
   * method returns.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   */
  public void raInactive()
  {

    if (cdrWriter.isAlive())
    {
      cdrWriter.stopFileProducer(); // interrupts and sets state to stopping
    }
    else
    {
      trace.info("File Producer is not alive");
    }
    if (callHistWriter.isAlive())
    {
      callHistWriter.stopFileProducer(); // interrupts and sets state to stopping
    }
    else
    {
      trace.info("File Producer is not alive");
    }    
    try
	{
		if (callProcessor != null) 
		{			
			callProcessor.shutdown();
			callProcessorStatsResgistry(null, false);
		}
		else
		{
			if (trace.isInfoEnabled()) trace.info("Stack listener was already null !? ");
		}
	} catch (Exception e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    raState = ST_INACTIVE; // TODO check previous state
    if (trace.isInfoEnabled()) trace.info("Resource adaptor deactivated ");
  }

  /**
   * This method is invoked by the SLEE whenever a new resource adaptor entity
   * is created by the Administrator, or when the Administrator attempts to
   * update the configuration properties of an existing resource adaptor
   * entity. The implementation of this method should examine the configuration
   * properties supplied and verify that the configuration properties are valid
   * for the resource adaptor.
   * <p>
   * This method may be invoked on a resource adaptor object in any valid
   * state, therefore the implementation of this method should assume nothing
   * about the internal state of the resource adaptor object.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param properties contains the proposed new values for all configuration
   *          properties specified for the resource adaptor entity.
   * @throws InvalidConfigurationException if the configuration properties are
   *           not valid for some reason.
   */
  public void raVerifyConfiguration(ConfigProperties properties)
      throws InvalidConfigurationException
  {
    if (trace.isFinestEnabled()) trace.finest("Verifying configuration "
        + properties);
    validateConfig(properties);
    if (trace.isFinestEnabled()) trace.finest("Configuration is OK");
  }

  /**
   * This method is invoked by the SLEE whenever the Administrator successfully
   * updates a resource adaptor entity with new configuration properties. The
   * implementation of this method should apply the new configuration
   * properties to its internal state.
   * <p>
   * If the <tt>supports-active-reconfiguration</tt> attribute of the
   * <tt>&lt;resource-adaptor-class&gt;</tt> element in the resource adaptor's
   * deployment descriptor has the value <tt>False</tt>, then this method will
   * only be invoked on a resource adaptor object in the Inactive state. If the
   * value of the <tt>supports-active-reconfiguration</tt> attribute is
   * <tt>True</tt>, then this method may be invoked on a resource adaptor
   * object that is in the Inactive, Active, or Stopping state.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param properties contains the new values for all configuration properties
   *          specified for the resource adaptor entity.
   */
  public void raConfigurationUpdate(ConfigProperties properties)
  {
    if (trace.isFinestEnabled()) trace.finest("  Reloading configuration...");
    loadConfig(properties);
  }

  /**
   * Get an implementation object of the specified resource adaptor interface.
   * <p>
   * Each resource adaptor type may specify a resource adaptor interface that
   * SBBs can use to interact with a resource adaptor implementing that
   * resource adaptor type. This method is used by the SLEE to obtain the
   * implementation objects for the resource adaptor interfaces specified by
   * each resource adaptor type implemented by this resource adaptor.
   * <p>
   * If none of the resource adaptor types implemented by a resource adaptor
   * specify a resource adaptor interface, this method may return a default
   * value of <code>null</code> as the SLEE will never invoke the method.
   * <p>
   * This method is invoked with an unspecified transaction context.
   * @param className the fully-qualified class name of the resource adaptor
   *          interface, as specified by the resource adaptor type, required by
   *          the SLEE.
   * @return an object that implements (and is therefore type-castable to) the
   *         specified resource adaptor interface.
   */
    public Object getResourceAdaptorInterface(String arg)
    {
      if(trace.isInfoEnabled())trace.info("Getting RA interface for "+arg);
      return writerProvider;
    }

  /**
   * Get the activity handle and event marshaler for the resource adaptor.
   * <p>
   * This method is invoked with an unspecified transaction context.
   * @return the activity handle and event marshaler.
   */
  public Marshaler getMarshaler()
  {
    return marshaler;
  }

  /**
   * The SLEE invokes this method on a resource adaptor object when a service
   * that is interested in events generated by this resource adaptor has
   * transitioned to the Active state. This method may be invoked when the
   * service is activated by the Administrator, or when the SLEE is restarted
   * and the service was previously in the Active state before the restart.
   * <p>
   * This method may be invoked by the SLEE when the resource adaptor object is
   * in the Inactive, Active, or Stopping state.
   * <p>
   * The SLEE need only provide a resource adaptor with information about the
   * event types received by the service that the resource adaptor may fire.
   * Generally this is limited to the resource adaptor types implemented by the
   * resource adaptor. However a resource adaptor may be able to fire events of
   * any type if its deployment descriptor has disabled this limitation, and in
   * such cases the SLEE should provide the resource adaptor with information
   * about all the event types that may be received by the service.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param serviceInfo information about the service that is now Active and
   *          the types of events that SBBs in the service may receive.
   */
  public void serviceActive(ReceivableService serviceInfo)
  {

  }

  /**
   * The SLEE invokes this method on a resource adaptor object when a service
   * that is interested in events generated by this resource adaptor has
   * transitioned to the Stopping state.
   * <p>
   * This method may be invoked by the SLEE when the resource adaptor object is
   * in the Inactive, Active, or Stopping state.
   * <p>
   * The SLEE need only provide a resource adaptor with information about the
   * event types received by the service that the resource adaptor may fire.
   * Generally this is limited to the resource adaptor types implemented by the
   * resource adaptor. However a resource adaptor may be able to fire events of
   * any type if its deployment descriptor has disabled this limitation, and in
   * such cases the SLEE should provide the resource adaptor with information
   * about all the event types that may be received by the service.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param serviceInfo information about the service that is now Stopping and
   *          the types of events that SBBs in the service may receive.
   */
  public void serviceStopping(ReceivableService serviceInfo)
  {

  }

  /**
   * The SLEE invokes this method on a resource adaptor object when a service
   * that is interested in events generated by this resource adaptor has
   * transitioned from the Stopping state to the Inactive state. The resource
   * adaptor may cease firing resource events that the service was interested
   * in receiving if no other services are registered to receive those events.
   * <p>
   * This method may be invoked by the SLEE when the resource adaptor object is
   * in the Inactive, Active, or Stopping state.
   * <p>
   * The SLEE need only provide a resource adaptor with information about the
   * event types received by the service that the resource adaptor may fire.
   * Generally this is limited to the resource adaptor types implemented by the
   * resource adaptor. However a resource adaptor may be able to fire events of
   * any type if its deployment descriptor has disabled this limitation, and in
   * such cases the SLEE should provide the resource adaptor with information
   * about all the event types that may be received by the service.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param serviceInfo information about the service that is now Inactive and
   *          the types of events that SBBs in the service may receive.
   */
  public void serviceInactive(ReceivableService serviceInfo)
  {
    if (trace.isFinestEnabled()) trace.finest("Service inactive. Id = "
        + serviceInfo);
  }

  /**
   * The SLEE invokes this method to ask the resource adaptor to check an
   * activity for liveness. This may occur, for example, if an activity has
   * been idle in the SLEE for a prolonged period of time.
   * <p>
   * If the resource adaptor considers the activity to still be live, it need
   * do nothing. However if the activity should have already ended, the
   * resource adaptor should end the activity using the {@link SleeEndpoint}
   * interface.
   * <p>
   * If the resource adaptor needs to query an external resource to determine
   * if the activity is still live, it should not unduly block the calling
   * thread while waiting for a response. Asynchronous queries are recommended
   * in this case.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity to check.
   */
  public void queryLiveness(ActivityHandle activityHandle)
  {
	  if (trace.isFinestEnabled()) trace.finest("Querying liveness for "+activityHandle);
  }

  /**
   * Get the activity object corresponding to the specified activity handle.
   * <p>
   * This method is generally invoked with an unspecified transaction context.
   * However if the resource adaptor starts an activity transactionally and the
   * SLEE needs to obtain the activity object for that activity during that
   * transaction, then the resource adaptor can expect, in this case, that the
   * transaction context associated with the calling thread when this method is
   * invoked to be the same as that with which the activity was created, as the
   * state for the activity may not be visible outside the transaction. The
   * resource adaptor may use the
   * {@link javax.slee.transaction.SleeTransactionManager} to detect the
   * presence or lack of any current transaction and manage that transaction
   * and any that it starts itself appropriately. The resource adaptor must
   * exit from this method with the same transaction context as it was invoked
   * (if any).
   * @param handle the activity handle of the activity.
   * @return the activity object corresponding to the activity handle.
   */
  public Object getActivity(ActivityHandle handle)
  {
    return callProcessor.findCallActivity(handle);
  }

  /**
   * Get the activity handle corresponding to the specified activity object.
   * <p>
   * The SLEE invokes this method when it needs to construct or lookup an
   * Activity Context for the specified activity. The SLEE uses this method to
   * determine the owner of an activity object. When this method is invoked the
   * resource adaptor object must first verify that the activity object was
   * generated by itself (or by a resource adaptor object of the same resource
   * adaptor entity). If the activity object does indeed belong to the resource
   * adaptor entity, the resource adaptor object must return the activity
   * handle for the activity. If the activity object does not belong to the
   * resource adaptor entity, then the resource adaptor object must return
   * <code>null</code> from this method.
   * <p>
   * This method is generally invoked with an unspecified transaction context.
   * However if the resource adaptor starts an activity transactionally and the
   * SLEE needs to obtain the activity handle for that activity during that
   * transaction, then the resource adaptor can expect, in this case, that the
   * transaction context associated with the calling thread when this method is
   * invoked to be the same as that with which the activity was created, as the
   * state for the activity may not be visible outside the transaction. The
   * resource adaptor may use the
   * {@link javax.slee.transaction.SleeTransactionManager} to detect the
   * presence or lack of any current transaction and manage that transaction
   * and any that it starts itself appropriately. The resource adaptor must
   * exit from this method with the same transaction context as it was invoked
   * (if any).
   * @param activity the activity object.
   * @return an activity handle for the activity object if the activity object
   *         was created by this resource adaptor entity, <code>null</code>
   *         otherwise. Returning a non-<code>null</code> value from this
   *         method is deemed to be claiming ownership of the activity.
   */
  public ActivityHandle getActivityHandle(Object activity)
  {
	  return ((CallActivity)activity).getCallActivityHandle();
  }

  /**
   * The SLEE invokes this method to inform the resource adaptor that the
   * activity and activity context corresponding to the specified activity
   * handle has been removed from the SLEE due to an administrative action. The
   * resource adaptor is expected to remove any internal state related to the
   * activity. Additionally, the resource adaptor may perform a protocol-level
   * operation to clean up any peer-related state.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity that has
   *          been removed from the SLEE.
   */
  public void administrativeRemove(ActivityHandle handle)
  {

  }

  /**
   * The SLEE invokes this method on a resource adaptor object when the SLEE
   * has completed activity end processing for the activity corresponding to
   * the specified activity handle. The resource adaptor is free to release any
   * internal resources held by this activity as the SLEE will not ask for it
   * again.
   * <p>
   * This method is only invoked for activities started with the
   * {@link ActivityFlags#REQUEST_ENDED_CALLBACK REQUEST_ENDED_CALLBACK}
   * activity flag.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity that has
   *          ended.
   */
  public void activityEnded(ActivityHandle handle)
  {}

  /**
   * The SLEE invokes this method on a resource adaptor object when the
   * Activity Context in the SLEE corresponding to the given activity handle is
   * no longer attached to any SBB entities, is no longer referenced by any
   * SLEE Facilities, and has no events being processed or waiting to be
   * processed on the activity. The resource adaptor can choose to subsequently
   * end the activity if it wishes.
   * <p>
   * This method is only invoked for activities started with the
   * {@link ActivityFlags#REQUEST_ACTIVITY_UNREFERENCED_CALLBACK
   * REQUEST_ACTIVITY_UNREFERENCED_CALLBACK} activity flag.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity that has
   *          become unreferenced.
   */
  public void activityUnreferenced(ActivityHandle handle)
  {
	  
  }

  /**
   * The SLEE invokes this method on a resource adaptor object to inform it
   * that an event it fired to the SLEE has been processed successfully. This
   * method is only invoked for events fired with the
   * {@link EventFlags#REQUEST_PROCESSING_SUCCESSFUL_CALLBACK
   * REQUEST_PROCESSING_SUCCESSFUL_CALLBACK} event flag set.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity on which
   *          the event was fired.
   * @param eventType the event type of the fired event.
   * @param event the event object.
   * @param address the optional default address associated with the event.
   * @param service the optional service the event was fired to.
   * @param flags the flags associated with the outcome of event processing.
   *          Currently only the {@link EventFlags#SBB_PROCESSED_EVENT
   *          SBB_PROCESSED_EVENT} flag is defined by the SLEE specification to
   *          be included in this argument.
   */
  public void eventProcessingSuccessful(ActivityHandle handle,
      FireableEventType eventType, Object event, Address address,
      ReceivableService service, int flags)
  {
	  if(trace.isInfoEnabled())trace.info("Event Processing Successful: "+handle );
	  if (eventType != null && incomingCallEvent.equals(eventType))
	  {
		  if(event != null && handle != null)
		  {
			  if ((flags & EventFlags.SBB_PROCESSED_EVENT) != 0)
			  {
				  //TODO everything's ok
			  }
			  else
			  {
				  IncomingCallEvent incomingCallEvent = (IncomingCallEvent)event;
				  CallActivityImpl callActivity = (CallActivityImpl)getActivity(handle);
				  callActivity.rejectIncomingCall();			  
			  }
		  }
		  else
		  {
			  if(trace.isSevereEnabled())trace.severe("Event Processing Successful, null event or handle!! ");
		  }
		  

	  }	  	  
  }

  /**
   * The SLEE invokes this method on a resource adaptor object to inform it
   * that an event it fired to the SLEE could not be processed successfully.
   * Event processing failure is characterized by the inability of the SLEE to
   * successfully attempt to deliver the event to all interested SBBs, not by
   * SBB processing failures (for which the SLEE has successfully made an
   * attempt to deliver the event to the failing SBB).
   * <p>
   * This method is only invoked for events fired with the
   * {@link EventFlags#REQUEST_PROCESSING_FAILED_CALLBACK
   * REQUEST_PROCESSING_FAILED_CALLBACK} event flag set.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity on which
   *          the event was fired.
   * @param eventType the event type of the fired event.
   * @param event the event object.
   * @param address the optional default address associated with the event.
   * @param service the optional service the event was fired to.
   * @param flags the flags associated with the outcome of event processing.
   *          Currently only the {@link EventFlags#SBB_PROCESSED_EVENT
   *          SBB_PROCESSED_EVENT} flag is defined by the SLEE specification to
   *          be included in this argument.
   * @param reason the reason why event processing failed.
   */
  public void eventProcessingFailed(ActivityHandle handle,
      FireableEventType eventType, Object event, Address address,
      ReceivableService service, int flags, FailureReason reason)
  {
	  trace.info("Event Processing Failed: "+handle );
	  if (eventType != null && incomingCallEvent.equals(eventType))
	  {
		  IncomingCallEvent incomingCallEvent = (IncomingCallEvent)event;
		  CallActivityImpl callActivity = (CallActivityImpl)getActivity(handle);
		  callActivity.rejectIncomingCall();
	  }	  
  }

  /**
   * The SLEE invokes this method on a resource adaptor object when the event
   * object for an event fired by the resource adaptor is no longer referenced
   * by the SLEE. Once this method has been invoked the resource adaptor can
   * safely reuse the event object if desired.
   * <p>
   * This method is only invoked for events fired with the
   * {@link EventFlags#REQUEST_EVENT_UNREFERENCED_CALLBACK
   * REQUEST_EVENT_UNREFERENCED_CALLBACK} event flag set.
   * <p>
   * This method is invoked with an unspecified transaction context. If the
   * resource adaptor needs the use of a transaction during this method then it
   * should use the {@link javax.slee.transaction.SleeTransactionManager} to
   * detect the presence or lack of any current transaction and manage that
   * transaction and any that it starts itself appropriately. The resource
   * adaptor must exit from this method with the same transaction context as it
   * was invoked (if any).
   * @param handle the activity handle corresponding to the activity on which
   *          the event was fired.
   * @param eventType the event type of the fired event.
   * @param event the event object.
   * @param address the optional default address associated with the event.
   * @param service the optional service the event was fired to.
   * @param flags the flags associated with the event. Currently no flags are
   *          defined by the SLEE specification to be included in this
   *          argument.
   */
  public void eventUnreferenced(ActivityHandle handle,
      FireableEventType eventType, Object event, Address address,
      ReceivableService service, int flags)
  {
	  trace.info("Event Processing Unreferenced: "+handle );
			  
  }

  // ////////////////////////////////////////////////////////////////////////////
  // --- Local utility methods ---
  // ////////////////////////////////////////////////////////////////////////////


  /**
   * @param cdr
   */
  public void writeCDR(GenericCDR cdr)
  {
    if (cdrWriter != null && cdr != null && cdrWriter.enqueueCDR(cdr))
    {
      if(trace.isInfoEnabled())trace.info("CDR enqueued ");
    }
    else
    {
      trace.severe("Could not enqueue message: " + cdr + " / " + cdrWriter);
    }
  }

  /**
   * @param another kind of register TODO
   */
  public void writeHistoric(CallHistoric callHistoric)
  {
    if (callHistWriter != null && callHistoric != null && callHistWriter.enqueueCDR(callHistoric))
    {
      if(trace.isInfoEnabled())trace.info("Call Historic enqueued ");
    }
    else
    {
      trace.severe("Could not enqueue message: " + callHistoric + " / " + callHistWriter);
    }
  }  

  /**
   * @return
   */
  public ResourceAdaptorContext getRaContext()
  {
    return raContext;
  }

  // ////////////////////////////////////////////////////////////////////////////
  // ----------------- Configuration Utility Methods
  // ---------------------------
  // ////////////////////////////////////////////////////////////////////////////

  /**
   * 
   */
  private void loadConfig(ConfigProperties properties)
  {

    configFilePath = (String) properties.getProperty(PARAM_ConfigFile).getValue();
    exampleB2B = (Boolean) properties.getProperty(PARAM_ExampleB2B).getValue();
    trace.info("CF: " + configFilePath);
    trace.info("ExampleB2B: " + exampleB2B);
    cdrFolderPath = (String) properties.getProperty(PARAM_CdrFolderPath).getValue();    
    trace.info("CDR Folder path " + cdrFolderPath);
    workFolder = new File(cdrFolderPath);
    fileNameSuffix = ((String) properties.getProperty(PARAM_fileNameSuffix).getValue());
    trace.info("FNS: " + fileNameSuffix);
  }

  private void validateConfig(ConfigProperties properties)
      throws InvalidConfigurationException
  {
    try
    {

	      String configFilePath = ((String) properties.getProperty(PARAM_ConfigFile).getValue());
	      if (configFilePath==null || "".equals(configFilePath.trim())) {
	        throw new InvalidConfigurationException("FileNameSuffix can not be empty"); }	
	      File configFile = new File(configFilePath);
	      if (!configFile.exists() || !configFile.isFile()
	          || !configFile.canRead() ) { throw new InvalidConfigurationException(
	          "Invalid config file: " + configFilePath); 
	      }
	      
	      String cdrFolderPath = ((String) properties.getProperty(PARAM_CdrFolderPath).getValue());
	      if (cdrFolderPath==null || "".equals(cdrFolderPath.trim())) {
	        throw new InvalidConfigurationException("CDR folder path can not be empty"); }      
	      File workFolder = new File(cdrFolderPath);
		    if (!workFolder.exists() || !workFolder.isDirectory()
		        || !workFolder.canRead() || !workFolder.canWrite() || !workFolder.canExecute()) { throw new InvalidConfigurationException(
		        "Invalid config file: " + configFilePath); 
		  }      
    }
    catch (InvalidConfigurationException e)
    {
      throw e;
    }
    catch (Throwable t)
    {
      throw new InvalidConfigurationException(
          "Unknown exception when verifying configuration: ", t);
    }
  }

  /**
   * Any exception thrown in this utility method will prevent the RA from
   * activating
   * @return
   */
  public SimpleNistConf getCDRConfiguration()
  {
    return new SimpleNistConf(configFilePath, workFolder, fileNameSuffix);
  }
  public SimpleNistConf getCHConfiguration()
  {
    return new SimpleNistConf(configFilePath, workFolder, fileNameSuffix+"CH");
  }

  public SleeEndpoint getEndpoint()
  {
	  return endpoint;
  }

  // JMX methods
  /**
   * 
   * @param callProcessorStats
   * @param register
   */
  private void callProcessorStatsResgistry(CallProcessorMBean callProcessorStats, boolean register)
  {
	    try
	    {
	    	if (register)
	    	{
			    String identityStr = "com.atos.slee.ra:type=" + raContext.getEntityName()+".CallProcessorStats";
			    if (trace.isInfoEnabled())trace.info("Identity: "+identityStr);
			
			    callProcessorStatsMBeanIdentity = new ObjectName( identityStr);    
			    if(mBeanServer == null) mBeanServer = ManagementFactory.getPlatformMBeanServer();
			    SimpleNistMBeanImpl mbean = new SimpleNistMBeanImpl();
			    mBeanServer.registerMBean( callProcessorStats, callProcessorStatsMBeanIdentity );
	    	}
	    	else //unregister
	    	{
	    		mBeanServer.unregisterMBean(callProcessorStatsMBeanIdentity);
	    	}
	    }
	    catch (MalformedObjectNameException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	    catch (NotCompliantMBeanException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (InstanceAlreadyExistsException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (MBeanRegistrationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
  }
//  public CallProcessor getStackListener()
//  {
//	  if(!exampleB2B)
//	  {
//		  return (CallProcessor)callProcessor;
//	  }
//	  return null;
//  }

  public int getCDRQueueOccupancy()
  {
	  return cdrWriter != null? cdrWriter.getQueueOccupancy():-1;
  }
}
