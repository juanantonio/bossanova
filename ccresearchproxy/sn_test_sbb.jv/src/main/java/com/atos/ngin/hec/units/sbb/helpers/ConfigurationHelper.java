package com.atos.ngin.hec.units.sbb.helpers;

import java.util.ArrayList;
import com.atos.ra.conf.api.AtosConfiguration;
import com.atos.ra.slee.common.IConfiguration;

public class ConfigurationHelper
{
  AtosConfiguration configuration = null;
  
  public ConfigurationHelper(AtosConfiguration configuration)
  {
    this.configuration = configuration;
  }
  
  public Integer getInt(String name) {
    return configuration.getInt(name);
  }

  public ArrayList getIntList(String name) {
    return configuration.getIntList(name);
  }

  public String getString(String name) {
    return configuration.getString(name);
  }

  public ArrayList getStringList(String name) {
    return configuration.getStringList(name);
  }

  public String get(String name) {
    return configuration.getString(name);
  }

  public String getStringWithoutLogError(String name) {
    return configuration.getStringWithoutLogError(name);
  }

  public IConfiguration getConfiguration() {
    return configuration;
  }
}