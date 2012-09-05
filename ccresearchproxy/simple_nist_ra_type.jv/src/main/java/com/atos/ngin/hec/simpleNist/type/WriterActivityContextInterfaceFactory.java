package com.atos.ngin.hec.simpleNist.type;

import javax.slee.ActivityContextInterface;
import javax.slee.FactoryException;
import javax.slee.UnrecognizedActivityException;

public interface WriterActivityContextInterfaceFactory
{
  public ActivityContextInterface getActivityContextInterface(CallActivity activity) 
      throws UnrecognizedActivityException, FactoryException;
}
