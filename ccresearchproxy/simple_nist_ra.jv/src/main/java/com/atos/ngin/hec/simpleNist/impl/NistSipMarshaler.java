package com.atos.ngin.hec.simpleNist.impl;
/*
com.atosorigin.mscl.ldapra.AclMarshaler.java

Copyright (c) 2009 AtosOrigin
Albarracin 25, 28037 Madrid
All Rights Reserved.

This software is confidential and proprietary information of AtosOrigin. 
You shall not disclose such confidential information and shall use it only 
in accordance with the terms of the license agreement established with 
AtosOrigin.
*/


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ResourceAdaptorContext;


/**
* NistSipMarshaler
* 
* @author Atos Origin
*/ 
public class NistSipMarshaler implements Marshaler, Serializable 
{
//Constants ---------------------------------------------------------------
private static final long serialVersionUID = 5746434601332844320L;

//Variables ---------------------------------------------------------------  
private Tracer   log;

//Methods -----------------------------------------------------------------

/**
 * Constructor without params
 */
public NistSipMarshaler(ResourceAdaptorContext ar_context)
{
  this.log = ar_context.getTracer("NistSipMarshaler");
  if(log.isFinestEnabled())log.finest("Created new NistSipMarshaler instance");
}

/**
 * Unmarshal handle implementation
 */
public ActivityHandle unmarshalHandle(DataInput in) throws IOException
{
  if( log.isFinestEnabled() )log.finest("Unmarshaling handle...");    
//  ActivityHandle connId;
//  try{ connId = new ClientConnectionID(in); }
//  catch(Exception e) { connId = new ServerConnectionID(in); } 
//  return connId;
  return null;
}

/**
 * Marshal handle implementation
 */
public void marshalHandle(ActivityHandle handle, DataOutput out)
throws IOException
{
  if( log.isFinestEnabled() )log.finest("Marshaling handle...");  
//  ( (LdapConnectionID) handle ).toStream( out );
}

/**
 * Marshal event implementation
 */
public void marshalEvent(FireableEventType eventType, Object event, DataOutput out)
throws IOException
{
  if( log.isFinestEnabled() )log.finest("Marshaling event..."); 
//  ( (LdapBaseEvent) event ).toStream( out );
}

/**
 * Unmarshal event implementation
 */  
public Object unmarshalEvent(FireableEventType eventType, DataInput in)
throws IOException
{
  if(log.isFinestEnabled())log.finest("Unmarshaling event...");  
  return null; //TODO not implemented
  
}

/**
 * Provides an estimate of the event (in bytes) of the marshaled form of the 
 * specified event or event type. This method is used by the SLEE to help 
 * size internal bufferes used for marshaling events. It is just a best-guess
 * estimate. 
 */
public int getEstimatedEventSize( FireableEventType eventType, Object event )
{
  return 16;
}  

/**
 * Provides an estimate of the handle (in bytes) of the marshaled form of the 
 * specified event or event type. This method is used by the SLEE to help 
 * size internal buffers used for marshaling events. It is just a best-guess
 * estimate. 
 */
public int getEstimatedHandleSize(ActivityHandle arg0)
{
  return 8;
}  

public ByteBuffer getEventBuffer(FireableEventType arg0, Object arg1)
{
  // Do nothing...
  return null;
}  

public void releaseEventBuffer(FireableEventType arg0, Object arg1,
    ByteBuffer arg2)
{
  // Do nothing...
} 
}
