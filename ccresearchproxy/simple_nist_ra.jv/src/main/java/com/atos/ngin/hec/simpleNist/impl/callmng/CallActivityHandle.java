/*
com.atos.ngin.hec.simpleNist.impl.CallActivityHandle.java

Copyright (c) 2012 AtoS
Albarracin 25, 28037 Madrid
All Rights Reserved.

This software is confidential and proprietary information of AtosOrigin. 
You shall not disclose such confidential information and shall use it only 
in accordance with the terms of the license agreement established with 
AtosOrigin.
*/


package com.atos.ngin.hec.simpleNist.impl.callmng;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import javax.slee.resource.ActivityHandle;

/**
* CallActivityHandle class. A connection is identified by means of just one
* long fields. 
* 
* @author JACAR
* @since  03/May/2012
*/
public class CallActivityHandle 
implements /*LdapConnectionID,*/ ActivityHandle, Serializable
{
// Constants ---------------------------------------------------------------
private static final long serialVersionUID = 1723073445186201550L;

// Variables ---------------------------------------------------------------
private final String   callId;
private String      idStr = null;
private String      entityName = null;
private int         hash = 0;

// Methods -----------------------------------------------------------------

/**
 * Default constructor
 * 
 * @param id   Unique identifier
 */
CallActivityHandle(String id, String entityName)
{
  this.callId = id; 
  this.idStr = entityName+"#"+id;
  this.entityName = entityName;
  this.hash = this.idStr.hashCode();
}

/**
 * Alternate constructor
 * 
 * @param datainput Data input stream
 */
CallActivityHandle(DataInput datainput) throws IOException
{
  this.callId = datainput.readUTF();       
  this.hash = datainput.readInt();
  this.idStr = datainput.readUTF();
  this.entityName = datainput.readUTF(); 
}

/**
 * Method used by the Marshaler to get the object value as a stream
 * 
 * @param dataoutput
 * @throws IOException
 */
public void toStream(DataOutput dataoutput) throws IOException
{
  dataoutput.writeUTF(callId);      
  dataoutput.writeInt(hash);
  dataoutput.writeUTF(idStr);
  dataoutput.writeUTF(entityName);
}

/**
 * Overrides the Object.hashcode() method.
 */
public int hashCode()
{
  if(this.hash == 0) this.hash = this.toString().hashCode();

  return hash;
}


/**
 * Overrides the Object.equals() method
 */
public boolean equals(Object obj)
{

  if(obj == null) return false;
  if(obj == this) return true;
  if(obj instanceof CallActivityHandle)
  {
	  CallActivityHandle connectionid = (CallActivityHandle)obj;
	  return callId == connectionid.callId && entityName != null &&
	  				connectionid.entityName != null && entityName.equals(connectionid.entityName);
	  //  Another way:  return toString().equals(connectionid.toString());
	  //  Yet another way:  return hashCode() == connectionid.hashCode());
  }
  else return false;
}

/**
 * Return a String object representing this CallActivityHandle instance.
 * @return String.
 */
public String toString()
{
  if(this.idStr == null) this.idStr = entityName+"#"+callId;

  return this.idStr;
}

public String getCallId()
{
  return callId;
}  
}
