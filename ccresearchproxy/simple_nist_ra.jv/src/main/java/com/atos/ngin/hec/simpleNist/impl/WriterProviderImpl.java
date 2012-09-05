package com.atos.ngin.hec.simpleNist.impl;

import java.io.Serializable;

import javax.slee.facilities.Tracer;

import com.atos.ngin.hec.simpleNist.type.GenericCDR;
import com.atos.ngin.hec.simpleNist.type.SimpleNistProvider;



public class WriterProviderImpl implements SimpleNistProvider, Serializable
{
  private static final long serialVersionUID = -1097693772759149805L;
  
  private SimpleNISTResourceAdaptor wra = null;
  private transient Tracer log = null;
  
  public WriterProviderImpl(SimpleNISTResourceAdaptor wra)
  {
    this.wra = wra;
    this.log = wra.getRaContext().getTracer(this.getClass().getSimpleName());
  }

  public void writeCDR(GenericCDR message)
  {
    log.info("[writeMessage]: " + message);
    wra.writeCDR(message);
  }



}
