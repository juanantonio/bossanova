package com.atos.ngin.hec.units.usage;

public interface CSReroutingUsageParameters extends HECUsageParameters
{  
  public abstract void sampleResponseTimeMO(long l);
  public abstract void sampleResponseTimeMT(long l);
  public abstract void sampleResponseTimeUCM_OUT_CORRELATION(long l);
}
