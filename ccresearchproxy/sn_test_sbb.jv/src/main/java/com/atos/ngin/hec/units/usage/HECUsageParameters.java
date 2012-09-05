package com.atos.ngin.hec.units.usage;

public interface HECUsageParameters
{
  public abstract void incrementIncomingMessage(long l);
  public abstract void incrementCorrelationError(long l);
  public abstract void incrementCallScenarioError(long l);
  public abstract void incrementIncomingMessageError(long l);
}
