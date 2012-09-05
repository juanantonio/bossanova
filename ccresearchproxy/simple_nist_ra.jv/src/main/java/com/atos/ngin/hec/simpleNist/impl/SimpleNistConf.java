package com.atos.ngin.hec.simpleNist.impl;

import java.io.File;

public class SimpleNistConf
{
	  private String configFilePath = null; 
	  private File   workDir        = null;
	  private String fileNameSuffix = null;  
	
	  public SimpleNistConf (String configFilePath, File wD, String fileNameSuffix)
	  {
		this.configFilePath = configFilePath;
	    this.workDir = wD;
	    this.fileNameSuffix = fileNameSuffix;
	  }
	  
	  public File getWorkPath() {
	    return workDir;
	  }
	  public void setWorkPath(File workDir) {
	    this.workDir = workDir;
	  }
	  
	  public String getFileNameSuffix() {
	    return fileNameSuffix;
	  }

	public String getConfigFilePath()
	{
		return configFilePath;
	}

	public void setConfigFilePath(String configFilePath)
	{
		this.configFilePath = configFilePath;
	}  
}
