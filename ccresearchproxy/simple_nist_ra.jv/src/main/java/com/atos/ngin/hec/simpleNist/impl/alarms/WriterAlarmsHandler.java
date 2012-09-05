package com.atos.ngin.hec.simpleNist.impl.alarms;

/*
com.atosorigin.mscl.ra.cdrreader.impl.alarms.CdrReaderAlarmsHandler

Copyright (c) 2009 AtosOrigin
Albarracin 25, 28037 Madrid
All Rights Reserved.

This software is confidential and proprietary information of AtosOrigin. 
You shall not disclose such confidential information and shall use it only 
in accordance with the terms of the license agreement established with 
AtosOrigin.
*/

import java.util.ArrayList;
import java.util.List;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorContext;

import com.atos.ngin.hec.simpleNist.impl.SimpleNISTResourceAdaptor;




public class WriterAlarmsHandler
{
//Constants ----------------------------------------------------------------
public static enum Type {FILESYSTEM_ERROR, FILE_CONGESTION};

//Variables ----------------------------------------------------------------
private AlarmFacility           alarmFacility;
private Tracer               log;
private List<WriterAlarm>    alarms;
private boolean                 dbg;
private SimpleNISTResourceAdaptor ra;
private static WriterAlarmsHandler instance;

//Methods ------------------------------------------------------------------

/**
 * Constructor (private to implement the singleton pattern)
 */
private WriterAlarmsHandler(SimpleNISTResourceAdaptor ra)
{
  this.ra = ra;
  ResourceAdaptorContext raContext = ra.getRaContext();
  this.alarmFacility = raContext.getAlarmFacility();                
  this.log = raContext.getTracer(this.getClass().getSimpleName()); 
  this.alarms  = new ArrayList<WriterAlarm>();
}

/**
 * Gets the single instance of this class
 */
public static WriterAlarmsHandler getInstance(SimpleNISTResourceAdaptor ra)
{
  if(instance == null) instance = new WriterAlarmsHandler(ra);
  return instance;
}

/**
 * Method to raise an alarm
 */
public void raiseAlarm(Type ar_type)
{
  final boolean dbg = log.isFinestEnabled();
  if(dbg)log.finest("Raising alarm "+ar_type);
  WriterAlarm alarm = this.getAlarm(ar_type);
  alarm.raise();
}

/**
 * Method to clear an alarm
 */
public void clearAlarm(Type ar_type)
{
  final boolean dbg = log.isFinestEnabled();
  if(dbg)log.finest("Clearing alarm "+ar_type);
  WriterAlarm alarm = this.getAlarm(ar_type);
  alarm.clear();
}  

/**
 * Method to get a connection related alarm instance by specifying alarm 
 * type, host and port. If an instance of this alarm was already created
 * that one is returned; otherwise a new instance is created. Over the
 * returned instance it is possible to raise and clear the alarm itself.
 * 
 * @param ar_type AlarmType (see 'Type' enum above)
 * @param ar_host Host generating the alarm
 * @param ar_port Port generating the alarm
 */
private synchronized WriterAlarm getAlarm(Type ar_type)
throws IllegalArgumentException
{
  this.dbg = log.isFinestEnabled();
  if(dbg)log.finest("Getting "+ar_type+" alarm");
  if(ar_type == Type.FILESYSTEM_ERROR)
  {
    WriterAlarm rv = null;
    for(WriterAlarm stored : alarms)
    {
      if(stored instanceof FileSystemFailure)
      {
        FileSystemFailure fsf = (FileSystemFailure)stored;
        if(dbg)log.finest("OK! Existing "+ar_type+" instance found");
        rv = fsf;
        break;
      }
    }
    
    if(rv == null) 
    {
      rv = new FileSystemFailure(ra);
      if(dbg)log.finest("OK! New "+ar_type+" instance created");
      this.alarms.add(rv);
    }
    return rv; 
  }

//  else if(ar_type == Type.FILE_CONGESTION)
//  {
//    WriterAlarm rv = null;
//    for(WriterAlarm stored : alarms)
//    {
//      if(stored instanceof FileCongestion)
//      {
//        FileCongestion fc = (FileCongestion)stored;
//        if(dbg)log.finest("OK! Existing "+ar_type+" instance found");
//        rv = fc;
//        break;          
//      }
//    }     
//    if(rv == null) 
//    {
//      rv = new FileCongestion(ra);
//      if(dbg)log.finest("OK! New "+ar_type+" instance created");
//      this.alarms.add(rv);
//    }      
//  return rv;  
//  }  
  
  else
  {
    throw new IllegalArgumentException("Sorry! Unknown alarm type");
  }
}
}
