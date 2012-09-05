package com.atos.ngin.hec.simpleNist.type;

public interface SimpleNistProvider
{
  String JNDI_NAME = "com/atos/ngin/hec/simple_nist_ra/simple_nist_ra_type/simplenistprovider";

  void writeCDR(GenericCDR message); 
 // WriterActivity createWriterActivity(); 
}