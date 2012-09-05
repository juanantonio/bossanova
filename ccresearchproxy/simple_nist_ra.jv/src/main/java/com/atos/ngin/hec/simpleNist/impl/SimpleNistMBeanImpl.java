package com.atos.ngin.hec.simpleNist.impl;

import java.util.Arrays;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;



public class SimpleNistMBeanImpl extends StandardMBean implements SimpleNistMBean
{

	public SimpleNistMBeanImpl() throws NotCompliantMBeanException
	{
		super(SimpleNistMBean.class);
	}

	public String getThis()
	{
		// TODO Auto-generated method stub
		return "this has been done";
	}

	public String doThat()
	{
		// TODO Auto-generated method stub
		return "that has been done";
	}

	public String sayThat(String that)
	{
		// TODO Auto-generated method stub
		return "I say "+that;
	}
	
    protected String getDescription(MBeanInfo info) {
        return "A SimpleNistMBean is an MBean that performs things.";
    }	
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        if (info.getName().equals("This")) {
            description = 
               "This attribute represents some item obtained from a thing.";
        }
        return description;
    }    
    
    //////////////////////////////////////////////////////
    ///////////////// DESCRIPTIONS ///////////////////////
    ////////////////////////////////////////////////////
    
    protected String getDescription(MBeanOperationInfo op,
            MBeanParameterInfo param,
            int sequence) {
        if (op.getName().equals("doThat")) {
            switch (sequence) {
                // 0 is first parameter: "thingummyjig"
                case 0: return "No parameters right?";
                default : return null;
            }
        }
        if (op.getName().equals("sayThat")) {
            switch (sequence) {
                // 0 is first parameter: "that"
                case 0: return "A contraption used for that";
                default : return null;
            }
        }        
        return null;
    }
    protected String getParameterName(MBeanOperationInfo op,
            MBeanParameterInfo param,
            int sequence) {
        if (op.getName().equals("doThat")) {
            switch (sequence) {                
                default : return null;
            }
        }
        if (op.getName().equals("sayThat")) {
            switch (sequence) {
                // 0 is first parameter: "that"
                case 0: return "that";
                default : return null;
            }
        }        
        return null;
    }    
    protected String getDescription(MBeanOperationInfo info) {
        String description = null;
        MBeanParameterInfo[] params = info.getSignature();
        String[] signature = new String[params.length];
        for (int i = 0; i < params.length; i++)
            signature[i] = params[i].getType();
        String[] methodSignature;
        
        methodSignature = new String[] {
            java.lang.String.class.getName()
            };
        if (info.getName().equals("sayThat") && 
            Arrays.equals(signature, methodSignature)) {
            description = "Says wht you want it to say";
        }
        
        return description;
    }    
    
}
