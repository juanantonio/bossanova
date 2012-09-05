/*
 * BaseSbb.java             Author: ILP               Created: 27-January-2011
 *
 * Copyright (c) AtosOrigin
 * Albarracin 25, 28037 Madrid
 * All Rights Reserved.
 *
 * This software is confidential and proprietary information of AtosOrigin. You
 * shall not disclose such confidential information and shall use it only in
 * accordance with the terms of the license agreement you entered with
 * AtosOrigin.
 */
package com.atos.ngin.hec.units.sbb.common;

import java.lang.reflect.Method;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.SbbLocalObject;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.atos.ra.conf.api.AtosConfigurationProvider;
import com.atos.ra.stat.api.StatisticsProvider;


public abstract class BaseSbb implements Sbb
{
  //-------------------------------- ATTRIBUTES ----------------------------//
  
  // SLEE facilities
  protected Logger                                     tracer;    
  protected ProfileFacility                            profileFacility;
  protected TimerFacility                              timerFacility;
  protected ActivityContextNamingFacility              aciNamingFacility;
  protected NullActivityFactory                        nullActivityFactory;
  protected NullActivityContextInterfaceFactory        nullACIFactory;
  protected SbbContext                                 sbbContext;
    
  // Statistics-RA provider
  protected StatisticsProvider                         statisticsProvider    = null; 
  // Configuration-RA provider
  protected AtosConfigurationProvider                  configurationProvider = null; 
  
  // Other attributes (they are not modified during the SBB life cycle)
  protected boolean                     debugEnabled  = false;
  protected boolean                     infoEnabled   = false;
  protected String                      preamble      = null;
  protected StringBuilder               traceMsg      = new StringBuilder();
  protected String                      nodeName      = null;
  protected Context                     jndiContext   = null;

  //--------------------------------- METHODS ------------------------------//

  /**
   * The SLEE invokes this method after a new instance of the SBB abstract class
   * is created. It uses this method to pass an SbbContext object to the SBB
   * object. During this method, an SBB entity has not been assigned to the SBB
   * object. In this method, the SBB object can allocate and initialize state or
   * connect to resources that are to be held by the SBB object during its
   * lifetime. Such state and resources cannot be specific to an SBB entity
   * because the SBB object might be reused during its lifetime to serve
   * multiple SBB entities. In this case, the method initialize a number of
   * references to SLEE facilities and other level properties.
   *
   * @param context the context associated with this type of SBB
   * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
   */
  public void setSbbContext(SbbContext context)
  {
    sbbContext = context;
    try
    {
      this.jndiContext = (Context) new InitialContext().lookup(Constants.JNDI_MY_ENV);
      System.err.println("BaseSbb: Initial Context: "+ jndiContext);

      tracer = Logger.getLogger(sbbContext.getSbb().getName());    
      System.err.println("BaseSbb: Tracer: "+ tracer);

      timerFacility = (TimerFacility) jndiContext.lookup(Constants.JNDI_TIMERS_STR);
      System.err.println("BaseSbb: timerFacility: "+ timerFacility);

      aciNamingFacility =
        (ActivityContextNamingFacility) jndiContext.lookup(Constants.JNDI_ACI_STR);
      System.err.println("BaseSbb: aciNamingFacility: "+ aciNamingFacility);

      nullACIFactory =
        (NullActivityContextInterfaceFactory)
        jndiContext.lookup(Constants.JNDI_ACI_FAC_STR);
      System.err.println("BaseSbb: nullACIFactory: "+ nullACIFactory);

      nullActivityFactory =
        (NullActivityFactory)jndiContext.lookup(Constants.JNDI_NA_FAC_STR);
      System.err.println("BaseSbb: nullActivityFactory: "+ nullActivityFactory);
      
      profileFacility = (ProfileFacility)jndiContext.lookup(Constants.JNDI_PROFILES_STR);
      System.err.println("BaseSbb: profileFacility: "+ profileFacility);

      statisticsProvider = (StatisticsProvider) jndiContext.lookup(Constants.STATSRA_PROVIDER);
      System.err.println("BaseSbb: Statistics provider: " + statisticsProvider);
      
      configurationProvider = (AtosConfigurationProvider) jndiContext.lookup(Constants.CONFRA_PROVIDER);
      System.err.println("BaseSbb: Configuration provider: " + configurationProvider);

      if(dbgEnabled())
        trace(Level.DEBUG,"References to facilities stored in attributes of this sbb object");
    }
    catch (NamingException ne)
    {
      System.err.println("Failed to locate SLEE Facilities");
      ne.printStackTrace();
    }
    catch (Exception ne)
    {
      System.err.println("Failed to locate SLEE Facilities");
      ne.printStackTrace();
    }
  }

  /**
   * The SLEE invokes this method before terminating the life of the SBB object.
   * During this method, an SBB entity is not assigned to the SBB entity. In
   * this method, the SBB object can free state or resources that are held by
   * it, and that usually had been allocated by the set SbbContext method.
   *
   * @see javax.slee.Sbb#unsetSbbContext()
   */
  public void unsetSbbContext()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"unsetSbbContext");
    sbbContext = null;
  }

  /**
   * The SLEE invokes this method on an SBB object before the SLEE creates a new
   * SBB entity in response to an initial event or an invocation of the create
   * method on a ChildRelation object. This method should initialize the SBB
   * object using the CMP field get and set accessor methods, such that when
   * this method returns, the persistent representation of the SBB entity can be
   * created.
   *
   * @throws javax.slee.CreateException when there is an application level
   *           problem (rather than SLEE or system level problem). If this
   *           method throws this exception, then the SLEE will not create the
   *           SBB entity. The SLEE will also propagate the CreateException
   *           unchanged to the caller that requested the creation of the SBB
   *           entity. The caller may be the SLEE or an SBB object.
   * @see javax.slee.Sbb#sbbCreate()
   */
  public void sbbCreate() throws javax.slee.CreateException
  {
    if(dbgEnabled()) trace(Level.DEBUG,"sbbCreate");
  }

  /**
   * The SLEE invokes this method on an SBB object after the SLEE creates a new
   * SBB entity. The SLEE invokes this method after the persistent
   * representation of the SBB entity has been created and the SBB object is
   * assigned to the created SBB entity. This method gives the SBB object a
   * chance to initialize additional transient state and acquire additional
   * resources that it needs while it is in the Ready state.
   *
   * @throws javax.slee.CreateException when there is an application level
   *           problem (rather than SLEE or system level problem). If this
   *           method throws this exception, then the SLEE will not create the
   *           SBB entity. The SLEE will also propagate the CreateException
   *           unchanged to the caller that requested the creation of the SBB
   *           entity. The caller may be the SLEE or an SBB object.
   * @see javax.slee.Sbb#sbbPostCreate()
   */
  public void sbbPostCreate() throws javax.slee.CreateException
  {
    preamble = Constants.NO_PREAMBLE;
    setFinestEnabled((tracer != null) ? tracer.isDebugEnabled() : false);
    setInfoEnabled((tracer != null) ? tracer.isInfoEnabled() : false);    
    if (debugEnabled) trace(Level.DEBUG,"sbbPostCreate");
  }

  /**
   * The SLEE invokes this method on an SBB object when the SLEE picks the SBB
   * object in the pooled state and assigns it to a specific SBB entity. This
   * method gives the SBB object a chance to initialize additional transient
   * state and acquire additional resources that it needs while it is in the
   * Ready state.
   *
   * @see javax.slee.Sbb#sbbActivate()
   */
  public void sbbActivate()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"sbbActivate");
  }

  /**
   * The SLEE invokes this method on an SBB object when the SLEE decides to
   * dis-associate the SBB object from the SBB entity, and to put the SBB object
   * back into the pool of available SBB objects. This method gives the SBB
   * object the chance to release any state or resources that should not be held
   * while the SBB object is in the pool. These state and resources typically
   * had been allocated during the sbbActivate method.
   *
   * @see javax.slee.Sbb#sbbPassivate()
   */
  public void sbbPassivate()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"sbbPassivate");
  }

  /**
   * The SLEE invokes the sbbRemove method on an SBB object before the SLEE
   * removes the SBB entity assigned to the SBB object. The SLEE removes an SBB
   * entity when the SBB sub-entity tree or SBB entity tree that the SBB entity
   * belongs to is removed explicitly by an invocation of the remove method on
   * an SBB local object or implicitly by the SLEE when the attachment count of
   * root SBB entity decrements to zero. The SBB object is in the Ready state
   * when sbbRemove is invoked and it will enter the pooled state when the
   * method completes. In this method, you can implement any actions that must
   * be done before the SBB entities persistent representation is removed.
   *
   * @see javax.slee.Sbb#sbbRemove()
   */
  public void sbbRemove()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"sbbRemove");
  }

  /**
   * The SLEE calls this method to synchronize the state of an SBB object with
   * its assigned SBB entities persistent state. The SBB Developer can assume
   * that the SBB object persistent state has been loaded just before this
   * method is invoked. It is the responsibility of the SBB Developer to use
   * this method to recompute or initialize the values of any transient instance
   * variables that depend on the SBB entities persistent state. In general, any
   * transient state that depends on the persistent state of an SBB entity
   * should be recalculated in this method.
   *
   * @see javax.slee.Sbb#sbbLoad()
   */
  public void sbbLoad()
  {
    setFinestEnabled((tracer != null) ? tracer.isDebugEnabled(): false);
    setInfoEnabled((tracer != null) ? tracer.isInfoEnabled(): false);
    if (debugEnabled) trace(Level.DEBUG,"sbbLoad");
    preamble = getTracePreamble();
  }

  /**
   * The SLEE calls this method to synchronize the state of the SBB entities
   * persistent state with the state of the SBB object. The SBB Developer should
   * use this method to update the SBB object using the CMP field accessor
   * methods before its persistent state is synchronized. The SBB Developer can
   * assume that after this method returns, the persistent state is
   * synchronized.
   *
   * @see javax.slee.Sbb#sbbStore()
   */
  public void sbbStore()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"sbbStore");
  }

  /**
   * Default implementation of 'exceptional situation' handling. The SLEE
   * invokes the sbbRolledBack call-back method after a transaction used in a
   * SLEE originated invocation has rolled back.
   *
   * @param context - The RolledBackContext of this SBB
   * @see javax.slee.Sbb#sbbRolledBack(javax.slee.RolledBackContext)
   */
  @SuppressWarnings("boxing")
  public void sbbRolledBack(RolledBackContext context)
  {
    trace(Level.WARN,"sbbRolledBack '",context.getEvent(),"' on '",
          context.getActivityContextInterface().getActivity(),'\'');
  }

  /**
   * Default implementation of 'exceptional situation' handling.
   *
   * @param exception the exception thrown by one of the methods invoked by the
   *          SLEE, e.g. a life cycle method, an event handler method, or a
   *          local interface method.
   * @param event If the method that threw the exception is an event handler
   *          method, the event argument will be the event and activity
   *          arguments of the event handler method. Otherwise, the event
   *          argument will be null.
   * @param aci If the method that threw the exception is an event handler
   *          method, the activity argument will be the event and activity
   *          arguments of the event handler method. Otherwise, the activity
   *          argument will be null.
   * @see javax.slee.Sbb#sbbExceptionThrown(java.lang.Exception,
   *      java.lang.Object, javax.slee.ActivityContextInterface)
   */
  @SuppressWarnings("boxing")
  public void 
  sbbExceptionThrown(Exception exception, Object event,ActivityContextInterface aci)
  {
    if(dbgEnabled()) trace(Level.DEBUG,"sbbExceptionThrown '",event,"' on '",
                           aci.getActivity(),'\'',exception,'\'');
  }

  /**
   * This method returns an SBB local object that represents the SBB entity
   * assigned to the SBB object of the SbbContext object.
   *
   * @return an object that implements the SBB local interface of the SBB
   *         entity.
   */
  public final SbbLocalObject getSbbLocalObject()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"SbbLocalObject()");
    return sbbContext.getSbbLocalObject();
  }

  /**
   * Convenience method to retrieve the SbbContext object stored in
   * setSbbContext.
   *
   * @return this SBB's SbbContext object
   */
  public final SbbContext getSbbContext()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"getSbbContext()");
    return sbbContext;
  }

  /**
   * Return the reference to the Profile Facility stored in setSbbContext
   *
   * @return the reference to the SLEE Profile Facility
   */
  public ProfileFacility getProfileFacility()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"getProfileFacility()");
    return profileFacility;
  }

  /**
   * Return the reference to the Timer Facility stored in setSbbContext
   *
   * @return the reference to the SLEE Timer Facility
   */
  public final TimerFacility getTimerFacility()
  {
    if(dbgEnabled()) trace(Level.DEBUG,"getTimerFacility()");
    return timerFacility;
  }
  
  public void detachActivities()
  {
	  if(dbgEnabled()) trace(Level.DEBUG,"detaching activities");
	  for (ActivityContextInterface aci : sbbContext.getActivities())
	  {
		  aci.detach(this.getSbbLocalObject());
	  }
  }

  //
  // Tracing Methods
  //

  /**
   * Method to write a new trace (common case: trace with one argument)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void trace(Level ar_level, Object ar_arg)
  {
    if(tracer != null)
    {
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_arg);
      tracer.log(ar_level, traceMsg.toString());
    }
  }
 
  /**
   * Method to write a new trace (common case: trace with two arguments)
   *
   * @param ar_level
   * @param ar_arg1
   * @param ar_arg2
   */
  public final void trace(Level ar_level, Object ar_1, Object ar_2)
  {
    if(tracer != null)
    {    
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);      
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to write a new trace (common case: trace with tree arguments)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void
  trace(Level ar_level, Object ar_1, Object ar_2, Object ar_3)
  {
    if(tracer != null)
    {  
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2).append(ar_3);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to write a new trace (common case: trace with tree arguments)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void
  trace(Level ar_level, Object ar_1, Object ar_2, Object ar_3, Object ar_4)
  {
    if(tracer != null)
    {  
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2).append(ar_3).append(ar_4);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to write a new trace (common case: trace with tree arguments)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void
  trace(Level ar_level, Object ar_1, Object ar_2, Object ar_3, Object ar_4,
        Object ar_5)
  {
    if(tracer != null)
    {  
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2).append(ar_3).append(ar_4).append(ar_5);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to write a new trace (common case: trace with tree arguments)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void
  trace(Level ar_level, Object ar_1, Object ar_2, Object ar_3, Object ar_4,
        Object ar_5, Object ar_6)
  {
    if(tracer != null)
    {  
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2).append(ar_3).append(ar_4).append(ar_5).
      append(ar_6);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to write a new trace (common case: trace with tree arguments)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void
  trace(Level ar_level, Object ar_1, Object ar_2, Object ar_3, Object ar_4,
      Object ar_5, Object ar_6, Object ar_7)
  {
    if(tracer != null)
    {  
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2).append(ar_3).append(ar_4).append(ar_5).
      append(ar_6).append(ar_7);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to write a new trace (common case: trace with tree arguments)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void
  trace(Level ar_level, Object ar_1, Object ar_2, Object ar_3, Object ar_4,
        Object ar_5, Object ar_6, Object ar_7, Object ar_8)
  {
    if(tracer != null)
    {  
      traceMsg.setLength(0);
      traceMsg.append(null == preamble ? Constants.NO_PREAMBLE : preamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2).append(ar_3).append(ar_4).append(ar_5).
      append(ar_6).append(ar_7).append(ar_8);
      tracer.log(ar_level, traceMsg.toString());
    }
  }

  /**
   * Method to get the trace preamble number
   *
   * @return the trace preamble as a StringBuilder
   */
  public String getPreambleFigure()
  {
    if(null == preamble || preamble.length() == 0) return Constants.NO_PREAMBLE;
    else
    {
      StringBuilder sb = new StringBuilder();
      for(char c : preamble.toCharArray()) if(Character.isDigit(c)) sb.append(c);
      return sb.toString();
    }
  }
  
  /**
   * This method generates a unique identifier generating a hash-code from a 
   * string with the dialog identifier and the node name. It also stores the
   * generated number into the 'TracePreamble' CMP field
   *
   * @param ar_dialogId The dialogId figure
   */
  protected String generateUniqueId(String ar_dialogId)
  {
    StringBuilder p = new StringBuilder(Constants.STR_BUILDERS_INI_SIZE);
    p.append(ar_dialogId);
    p.append(Constants.AT);
    String pr = Long.toString((long)p.toString().hashCode()-(long)Integer.MIN_VALUE);
    return pr;
  }

  /**
   * This method generates, and stores as a CMP field the preamble used in the
   * service logs. For that, the unique dialog ID (the correlation id) is used.
   * (it is better to generate the preamble string once and to store it, than
   * to generate the preamble for every trace).
   *
   * @param ar_dialogId The dialogId figure as a String
   */
  protected void storeTracePreamble(String ar_dialogId)
  {
    StringBuilder p = new StringBuilder(Constants.STR_BUILDERS_INI_SIZE);
    p.append(Constants.BLANK);
    p.append(ar_dialogId);
    p.append(Constants.BLANK);
    this.preamble = p.toString();
    setTracePreamble(this.preamble);
  }

  /**
   * Method to enable debug level for traces
   *
   * @param ar_enabled
   */
  private void setFinestEnabled(final boolean ar_enabled)
  {
    debugEnabled = ar_enabled;
  }

  /**
   * Method to check if debug level was set for the traces
   *
   * @param ar_enabled
   */
  public final boolean dbgEnabled()
  {
    return debugEnabled;
  }

  /**
   * Method to enable info level for traces
   *
   * @param ar_enabled
   */
  private void setInfoEnabled(final boolean ar_enabled)
  {
    infoEnabled = ar_enabled;
  }

  /**
   * Method to check if info level was set for the traces
   *
   * @param ar_enabled
   */
  public final boolean infoEnabled()
  {
    return infoEnabled;
  }

  /**
   * Method to get the exception message in order to generate a readable trace
   *
   * @param ar_prefix Prefix added to the exception message
   * @param ae_e      The exception itself
   *
   * @return String with a exception text
   */
  public String getExceptionMsg(String ar_prefix, Throwable ar_e)
  {
    StringBuilder msg = new StringBuilder(ar_prefix);
    if(ar_e.getCause() != null)
       msg.append(" cause: ").append(ar_e.getCause().toString());
    else if(ar_e.getMessage() != null && ar_e.getMessage().length() > 0)
       msg.append(" message: ").append(ar_e.getMessage());
    else if(ar_e.getLocalizedMessage() != null &&
            ar_e.getLocalizedMessage().length()>0)
       msg.append(" localized message: ").append(ar_e.getLocalizedMessage());
    else
      msg.append(" ").append(ar_e.toString());
    return msg.toString();
  }

  /**
   * getStack(). Used to get the exception stack
   * @param e
   * @return Stack string
   */
  public static String getStack(Throwable e) 
  {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack Trace:\n");
        sb.append("- Cause: " + e.getCause() + '\n');
        for (int i = 0; i < e.getStackTrace().length; i++) 
        {
              sb.append("        " + e.getStackTrace()[i]).append('\n');
        }
        return sb.toString();
        
  }

  /**
   * Utility method
   */
  public static String strcat(String front, String middle, String back) 
  {
        StringBuilder buff = new StringBuilder();
        buff.append(front);
        buff.append(middle);
        buff.append(back);
        return buff.toString();
  }
  
  /**
   * Utility method
   */
  public static String strcat(String front,  String back) 
  {
        StringBuilder buff = new StringBuilder();
        buff.append(front);
        buff.append(back);
        return buff.toString();
  }
  
  //
  // Abstract methods for CMP common status fields
  //
  public abstract String getTracePreamble();
  public abstract void   setTracePreamble(String sb);

  //
  // This CMP stores the current ACI. 
  //
  public abstract ActivityContextInterface getActivityContextInterface();
  public abstract void setActivityContextInterface(ActivityContextInterface aci);

  // ///////////////////// DEVELOPMENT UTILITIES //////////////////////////////
  /**
   * Utility method to trace exceptions
   *
   * @param ar_enabled
   */
  public void traceException (Throwable e)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(" EXCEPTION: ");
    sb.append(e.getLocalizedMessage());
    sb.append("\n");
    sb.append(e.getMessage());
    sb.append("\n");
    sb.append(e.getCause());
    sb.append("\n");
    int counter = 0;
    int maxCount = dbgEnabled()? 10 : 1;
    for (StackTraceElement ste:e.getStackTrace())
    {
      sb.append(ste.getClassName());
      sb.append(".");
      sb.append(ste.getMethodName());
      sb.append(".");
      sb.append(ste.getLineNumber());
      sb.append("\n");
      if(counter++ >= maxCount) break;
    }
    trace(Level.ERROR,sb);
  }
  
  /**
   * Utility method to dump object contents
   * 
   * @param o
   */
  protected StringBuilder dumpObject(Object o)
  {
    try
    {    	
      StringBuilder sb = new StringBuilder();
      if(o == null) return sb.append("null");
      Class oClass = o.getClass();
      Method[] oMethods = oClass.getMethods();
      sb.append("- Dumping object: "+oClass.getSimpleName()).append("\n");
      for (Method oMethod : oMethods)
      {
        if(oMethod.getName().startsWith("get") || oMethod.getName().startsWith("is"))
        {
          try
          {
            sb.append("- "+oMethod.getName()+": "+oMethod.invoke(o, null)).append("\n");            
          }
          catch(Exception e)
          {
//            sb.append("* Exception invoking method "+oMethod.getName()).append("\n");
          }
        }
        else
        {
//          sb.append("* Method "+oMethod.getName()+" found, discarding").append("\n");
        }
        
      }
      return sb;
    }
    catch (Throwable t)
    {
      t.printStackTrace();
      return new StringBuilder("Unexpected exception dumping object: "+t);
    }
  }  
  /**
   * Utility method to trace full stack for caught exceptions
   * 
   * @param stackTraceList
   * @return
   */
  protected StringBuilder dumpStackTrace(StackTraceElement[] stackTraceList)
  {
    StringBuilder sb = new StringBuilder("Exception dump: ").append('\n');
    for (StackTraceElement ste:stackTraceList)
    {
      sb.append(ste.getClassName()).append(':').append(ste.getLineNumber()).append('\n');
    }
    return sb;
  }  
  
  public Logger getTracer() {
    return tracer;
  }
}
