package com.atos.ngin.hec.units.hec_logic;

import java.io.Serializable;

import com.atos.ngin.hec.units.hec_logic.HECLogic.BNumberType;


public class MO_NO_RR_Context implements Serializable
{
	private static final long serialVersionUID = 1834235691929864446L;
	

	
	//	Protected attributes
	//
	/** Calling Party (A) Number. */
	protected String callingPartyNumber;
	/** Calling Party (A) Number. */
	protected boolean callingAPRIRestricted = false;
	/** Called Party (B) Number. */
	protected String calledPartyNumber;
	/** BNumberType ordinal **/
	protected int calledPartyTypeOrdinal;
	/** Location Number (MSC). */
	protected String mscAddress;
	/** IN session id **/
	protected String inSessionId;


	/**
	 * [description]
	 * 
	 * @return
	 * 
	 * @since 1.1
	 */
  public String getCalledPartyNumber()
  {
    return this.calledPartyNumber;
  }
	
	/**
	 * [description]
	 * 
	 * @return
	 * 
	 * @since 1.1
	 */
  public String getCallingPartyNumber()
  {
    return this.callingPartyNumber;
  }

	
	/**
	 * [description]
	 * 
	 * @param calledPartyNumber
	 * 
	 * @since 1.1
	 */
  public void setCalledPartyNumber(String calledPartyNumber)
  {
    this.calledPartyNumber = calledPartyNumber;
  }
	
	/**
	 * [description]
	 * 
	 * @param callingPartyNumber
	 * 
	 * @since 1.1
	 */
  public void setCallingPartyNumber(String callingPartyNumber)
  {
    this.callingPartyNumber = callingPartyNumber;
  }
	

  	/**
  	 * Location number (MSC)
  	 * @return
  	 */
	public String getMSCAddress()
	{
		return mscAddress;
	}

	public void setMSCAddress(String mscAddress)
	{
		this.mscAddress = mscAddress;
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
          .append("Calling={").append(this.callingPartyNumber);
    if(isCallingAPRIRestricted())result.append("(restricted)");
    result.append("},Called={").append(this.calledPartyNumber)
          .append("},CalledType={").append(BNumberType.values()[calledPartyTypeOrdinal])
          .append("},MSC={").append(this.mscAddress)
          .append("},IN-SessionId={").append(this.inSessionId)
          .append('}');

    return result.toString();
  }

	public BNumberType getCalledPartyType()
	{
		return BNumberType.values()[calledPartyTypeOrdinal];
	}

	public void setCalledPartyType(BNumberType calledPartyType)
	{
		this.calledPartyTypeOrdinal = calledPartyType.ordinal();
	}

	public String getInSessionId()
	{
		return inSessionId;
	}

	public void setInSessionId(String inSessionId)
	{
		this.inSessionId = inSessionId;
	}

	public boolean isCallingAPRIRestricted()
	{
		return callingAPRIRestricted;
	}

	public void setCallingAPRIRestricted(boolean callingAPRIRestricted)
	{
		this.callingAPRIRestricted = callingAPRIRestricted;
	}



  

	
} 
