package com.atos.ngin.hec.units.sbb.helpers;

import org.apache.log4j.Logger;
import com.atos.ra.stat.api.StatisticsActivity;

public class StatisticsHelper
{
  private Logger             logger     = null;  
  private StatisticsActivity statistics = null;
  
  public StatisticsHelper (StatisticsActivity statistics, Logger logger)
  {
    this.statistics = statistics;
    this.logger = logger;
  }
  
  public void incCounter (String counter )
  {
    try {
      statistics.incCounter(counter);
      logger.info("[Statistics] Increment Counter # <" + counter + ">") ;
    } catch (Exception e) {
      logger.error("[Statistics] ERROR Incrementing Counter # <" + counter + "> " + e) ;
    }
  }
  
  public void incCounter(String tableGroup, String counter) 
  {
    try {
      statistics.incCounter(tableGroup,counter);
      logger.info("[Statistics] Increment Counter # <" + counter + ">") ;
    } catch (Exception e) {
      logger.error("[Statistics] ERROR Incrementing Counter # <" + counter + "> " + e) ;
    }
  }
}