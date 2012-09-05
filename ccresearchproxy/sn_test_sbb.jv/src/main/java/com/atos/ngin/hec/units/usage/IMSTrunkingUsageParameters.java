package com.atos.ngin.hec.units.usage;


public interface IMSTrunkingUsageParameters extends HECUsageParameters
{  
  public abstract void sampleResponseTimeMO(long l);
  public abstract void sampleResponseTimeMT(long l);
  public abstract void sampleResponseTimeUCM_IN(long l);
  public abstract void sampleResponseTimeUCM_OUT_OFFNET(long l);
  public abstract void sampleResponseTimeUCM_OUT_ONNET(long l);  
}
