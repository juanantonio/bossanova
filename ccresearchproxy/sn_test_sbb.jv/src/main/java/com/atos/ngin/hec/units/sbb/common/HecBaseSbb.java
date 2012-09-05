
package com.atos.ngin.hec.units.sbb.common;

import javax.slee.ActivityContextInterface;
import javax.slee.SbbContext;

import org.apache.log4j.Level;

import com.atos.ngin.hec.units.sbb.helpers.ConfigurationHelper;
import com.atos.ngin.hec.units.sbb.helpers.StatisticsHelper;
import com.atos.ra.conf.api.AtosConfiguration;
import com.atos.ra.stat.api.StatisticsActivity;




public abstract class HecBaseSbb extends BaseSbb
{	
	  
  protected StatisticsHelper    statsHelper  = null;
  protected ConfigurationHelper confHelper   = null;
  
  public void setSbbContext(SbbContext context)
  {
    super.setSbbContext(context);
    
    nodeName = System.getenv(Constants.HEC_NODE);
    System.err.println("HECBaseSbb: nodeName: "+ nodeName);
      
    statsHelper = createStatsHelper();
    confHelper = createConfHelper();
  }
  
  public StatisticsHelper createStatsHelper()
  {
    StatisticsHelper statisticsHelper = null;
    try
    {
      StatisticsActivity statistics = 
        statisticsProvider.getStatisticsActivity(Constants.HEC_STATS_GROUP);
      statisticsHelper = new StatisticsHelper(statistics, tracer);
    }
    catch (Exception e)
    {
      tracer.error(getExceptionMsg("** Error trying to create StatsHelper: ",e));
    }
    return statisticsHelper;
  }
  
  public ConfigurationHelper createConfHelper()
  {
    ConfigurationHelper configurationHelper = null;
    try
    {
      AtosConfiguration configuration = configurationProvider.getConfiguration();
      configurationHelper = new ConfigurationHelper(configuration);
    }
    catch (Exception e)
    {
      tracer.error(getExceptionMsg("** Error trying to create ConfHelper: ",e));
    }
    return configurationHelper;
  }

  public StatisticsHelper getStatisticsHelper() {
    return statsHelper;
  }

  public ConfigurationHelper getConfigurationHelper() {
    return confHelper;
  }
  
	/**
	* Utility method to trace full stack for caught exceptions
	* 
	* @param stackTraceList
	* @return
	*/
	protected StringBuilder dumpStackTrace(Throwable e)
	{		 
		 StringBuilder sb = new StringBuilder("Exception dump: ").append(e).append('\n');
		 for (StackTraceElement ste:e.getStackTrace())
		 {
		   sb.append(ste.getClassName()).append(':').append(ste.getLineNumber()).append('\n');
		 }
		 return sb;
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
  
  	// This was moved to child classes, since not all counters are common now
	//  /**
	//   * Required by the SLEE to manage the default usage parameters (counters)
	//   * @return {@link HECUsageParameters}
	//   */
	//  public abstract HECUsageParameters getDefaultSbbUsageParameterSet();  
	//  /**
	//   * Required by the SLEE to manage the default usage parameters (counters)
	//   * @return {@link HECUsageParameters}
	//   */
	//  public abstract HECUsageParameters getSbbUsageParameterSet(String name)
	//  throws javax.slee.usage.UnrecognizedUsageParameterSetNameException;  
  
	//  protected void incrementCounter(HECUsageCounter counter)
	//  {
	//	  try
	//	  {
	//	    HECUsageParameters usage = null;
	//	    try
	//	    {
	//	      usage = getSbbUsageParameterSet(nodeName);
	//	    }
	//	    catch(javax.slee.usage.UnrecognizedUsageParameterSetNameException e)
	//	    {
	//	      usage = getDefaultSbbUsageParameterSet();
	//	    }
	//	    switch(counter)
	//	    {
	//	    	case INCOMING_MESSAGE:
	//	    		usage.incrementIncomingMessage(1);
	//	    		break;
	//	    	case INVALID_INCOMING_MESSAGE:
	//	    		usage.incrementIncomingMessageError(1);
	//	    		break;
	//	    	case UNKNOWN_CALL_SCENARIO:
	//	    		usage.incrementCallScenarioError(1);
	//	    		break;
	//	    	case CORRELATION_ERROR:
	//	    		usage.incrementCorrelationError(1);
	//	    		break;
	//	    	default:
	//	    }
	//	    
	//	  }
	//	  catch(Exception e)
	//	  {
	//		  if(debugEnabled)dumpStackTrace(e);
	//		  trace(Level.ERROR, "Cannot increment counter ",counter);
	//	  }
	//  }
  
  
  /**
   * Method to write a new trace when no preamble has been set(common case: trace with one argument)
   *
   * @param ar_level
   * @param ar_arg
   */
  public final void trace(String adhocPreamble, Level ar_level, Object ar_arg)
  {
    if(tracer != null)
    {
      traceMsg.setLength(0);
      traceMsg.append(null == adhocPreamble ? Constants.NO_PREAMBLE : adhocPreamble);  
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_arg);
      tracer.log(ar_level, traceMsg.toString());
    }
  }  
  /**
   * Method to write a new trace no preamble has been set(common case: trace with two arguments)
   *
   * @param ar_level
   * @param ar_arg1
   * @param ar_arg2
   */
  public final void trace(String adhocPreamble, Level ar_level, Object ar_1, Object ar_2)
  {
    if(tracer != null)
    {    
      traceMsg.setLength(0);
      traceMsg.append(null == adhocPreamble ? Constants.NO_PREAMBLE : adhocPreamble);      
      if (ar_level.equals(Level.ERROR)) traceMsg.append(Constants.ERROR_STR);
      traceMsg.append(ar_1).append(ar_2);
      tracer.log(ar_level, traceMsg.toString());
    }
  }  
}
