package com.atos.ngin.hec.simpleNist.impl.alarms;
/*
com.atosorigin.mscl.ra.cdrreader.impl.CDRReaderAlarm.java

Copyright (c) 2009 AtosOrigin
Albarracin 25, 28037 Madrid
All Rights Reserved.

This software is confidential and proprietary information of AtosOrigin. 
You shall not disclose such confidential information and shall use it only 
in accordance with the terms of the license agreement established with 
AtosOrigin.
*/

/**
* AclAlarm interface. Every alarm should implement the method declared here.
* 
* @author jacar
* @since  03/Dic/2009
*/
public interface WriterAlarm
{
/**
 * Method to raise a new alarm. If the alarm was already raised, it has no
 * effect.
 */
public abstract void raise();

/**
 * Method to clear an already raised alarm.
 */
public abstract void clear();

/**
 * Overrides the 'Object.toString()' method
 */
public abstract String toString();
}