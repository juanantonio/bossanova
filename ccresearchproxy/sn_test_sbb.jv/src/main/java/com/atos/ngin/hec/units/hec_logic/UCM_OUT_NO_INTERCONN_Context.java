package com.atos.ngin.hec.units.hec_logic;

import java.io.Serializable;


public class UCM_OUT_NO_INTERCONN_Context implements Serializable
{
	private static final long serialVersionUID = 1834235691929864446L;
	

	
	//	Protected attributes
	//

	/** Called Party (B) Number. */
	protected String calledPartyNumberNATFormat;
	/** IMS session id **/
	protected String imsSessionId;
	


	/**
	 * [description]
	 * 
	 * @return
	 * 
	 * @since 1.1
	 */
  public String getCalledPartyNumberNATFormat()
  {
    return this.calledPartyNumberNATFormat;
  }
	

	
	/**
	 * [description]
	 * 
	 * @param calledPartyNumber
	 * 
	 * @since 1.1
	 */
  public void setCalledPartyNumberNATFormat(String calledPartyNumber)
  {
    this.calledPartyNumberNATFormat = calledPartyNumber;
  }
	
	
	public String getImsSessionId()
	{
		return imsSessionId;
	}

	public void setImsSessionId(String imsSessionId)
	{
		this.imsSessionId = imsSessionId;
	}
	
	/**
	 * Returns a string representation of the object.
	 *
	 * @return
	 * 
	 * @since 1.1
	 */
	  public String toString()
	  {
	    StringBuilder result = new StringBuilder();
	    result.append("ContextInfo{")
	          .append("},Called(NAT format)={").append(this.calledPartyNumberNATFormat)
	          .append("},IMS-SessionId={").append(this.imsSessionId)
	          .append('}');

	    return result.toString();
	  }
  	
}