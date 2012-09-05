package com.atos.ngin.hec.units.hec_logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class HECContextFactory
{

	/**
	 * This could be improved by means of specific serializing 
	 * methods in each class
	 * 
	 * @param context
	 * @return
	 * @throws IOException
	 */
	public static byte[] getSerializedPayload (MO_NO_RR_Context context) throws IOException 
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);   
		out.writeObject(context);
		return bos.toByteArray();			
	}
	
	/**
	 * This could be improved by means of specific deserializing methods
	 * in each class
	 * 
	 * @param payload
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static MO_NO_RR_Context getMO_NO_RR_Context (byte[] payload) throws IOException, ClassNotFoundException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(payload);
		ObjectInput in = new ObjectInputStream(bis);
		Object o = in.readObject();		
		return (MO_NO_RR_Context)o;
	}	
	
	/**
	 * This could be improved by means of specific serializing 
	 * methods in each class
	 * 
	 * @param context
	 * @return
	 * @throws IOException
	 */
	public static byte[] getSerializedPayload (UCM_OUT_NO_INTERCONN_Context context) throws IOException 
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);   
		out.writeObject(context);
		return bos.toByteArray();			
	}
	
	/**
	 * This could be improved by means of specific deserializing methods
	 * in each class
	 * 
	 * @param payload
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static UCM_OUT_NO_INTERCONN_Context getUCM_OUT_NO_INTERCONN_Context (byte[] payload) throws IOException, ClassNotFoundException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(payload);
		ObjectInput in = new ObjectInputStream(bis);
		Object o = in.readObject();		
		return (UCM_OUT_NO_INTERCONN_Context)o;
	}		
}
