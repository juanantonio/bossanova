package com.atos.ngin.hec.simpleNist.impl.alarms;

/*
com.atosorigin.mscl.ra.cdrreader.impl.alarms.FileSystemFailure

Copyright (c) 2009 AtosOrigin
Albarracin 25, 28037 Madrid
All Rights Reserved.

This software is confidential and proprietary information of AtosOrigin. 
You shall not disclose such confidential information and shall use it only 
in accordance with the terms of the license agreement established with 
AtosOrigin.
*/

import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.AlarmLevel;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorContext;

import com.atos.ngin.hec.simpleNist.impl.SimpleNISTResourceAdaptor;


//import org.apache.log4j.Logger;


/**
* This class represents an alarm which is raised when the filesystem is failing.
* 
* @author jacar
* @since  15/dic/2009
*/
public class FileSystemFailure implements WriterAlarm 
{
//Constants ----------------------------------------------------------------
private static final String     instanceId = "filesystem.failure";
private static final String     name  = "CdrReaderRa." + instanceId;
private static final AlarmLevel level = AlarmLevel.MAJOR;

//Variables ----------------------------------------------------------------
private String        alarmId;
private AlarmFacility alarmFacility;
private Tracer        log;
private boolean       dbg; 

//Methods ------------------------------------------------------------------

/**
 * Constructor
 */
public 
FileSystemFailure(SimpleNISTResourceAdaptor ra)
{
  ResourceAdaptorContext raContext = ra.getRaContext();
  this.alarmFacility = raContext.getAlarmFacility();                
  this.log = raContext.getTracer(this.getClass().getSimpleName()); 
}

/**
 * Method to raise alarm. If the alarm is already raised, the process is
 * skipped. 
 */
public void raise()
{
  dbg = log.isFinestEnabled();
  
  if(dbg)log.finest("Raising 'filesystem failure' alarm");
  if(this.alarmId == null)
  {
    this.alarmId = this.alarmFacility.raiseAlarm( name, instanceId, level, this.toString() );
    if(dbg)log.finest("Done: " + this.alarmId);
  }
  else
  {
    if(dbg)log.finest("Skipping. Alarm "+alarmId+" already raised");
  }
}  

/**
 * Method to clear an already raised alarm. 
 */
public void clear()
{
  dbg = log.isFinestEnabled();
  
  if(this.alarmId != null)
  {
    if(dbg)log.finest("Clearing alarm " + this.alarmId);
    this.alarmFacility.clearAlarm(this.alarmId);
    this.alarmId = null;
  }
  else if(dbg)log.finest("No alarm to Clear");
}
 

/**
 * Overrides the 'Object.toString()' method
 */
public String toString()
{
  StringBuilder sb = new StringBuilder();
  sb.append(" Filesystem error");
  return sb.toString(); 
}
}
