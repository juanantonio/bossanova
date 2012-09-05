package com.atos.ngin.hec.simpleNist.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorContext;

import com.atos.ngin.hec.simpleNist.impl.alarms.WriterAlarmsHandler;
import com.atos.ngin.hec.simpleNist.type.GenericCDR;
import com.atos.ngin.hec.simpleNist.type.Writable;




public class CDRWriter extends Thread
{  
private SimpleNISTResourceAdaptor ra;//TODO currently not in use
private final SimpleNistConf conf;
private final WriterAlarmsHandler alarmsHandler;//TODO implement alarms
private Tracer            log; 
volatile private boolean           terminationRequested = false;
private String                     currentTimestamp;
private FileWriter                 currentFileWriter;

private final int LOOP_DELAY              = 1000;
private final int INITIAL_QUEUE_SIZE      = 1000;

private LinkedBlockingQueue<Writable>        queue;

static int tId = 1;
/**
 * Constructor method
 * 
 * @param ra
 * @param conf
 */
public CDRWriter(SimpleNISTResourceAdaptor ra, SimpleNistConf configuration)
{
  super(ra.getRaContext().getEntityName()+".CdrWriter-"+(tId++));
  ResourceAdaptorContext raContext = ra.getRaContext();
  this.ra = ra;
  this.log = raContext.getTracer(this.getClass().getSimpleName());
  this.conf = configuration;
  AlarmFacility aF = raContext.getAlarmFacility();    
  this.alarmsHandler = WriterAlarmsHandler.getInstance( ra );      
  queue = new LinkedBlockingQueue<Writable>(INITIAL_QUEUE_SIZE);
}


/**
 * 
 * @param message
 * @return
 */
public boolean enqueueCDR (Writable message)
{   
  if(!terminationRequested)
  {
    boolean enqueued = queue.offer(message);
    if(log.isInfoEnabled())log.info("Enqueuing message "+message);
    if(log.isInfoEnabled())log.info("Queue size: "+queue.size());
    return enqueued;
  }
  else
  {
    log.severe("Trying to enqueue an element once the writer is stopping");
    return false;
  }
}

public int getQueueOccupancy()
{
	return queue != null? queue.size() : -1;
}
/**
 * 
 * @return
 */
public boolean isTerminationRequested()
{
  return terminationRequested;
}

/**
 *  Sets the state to 'terminationRequested' and interrupts the thread
 */
public void stopFileProducer()
{
  if(log.isFinestEnabled()) log.finest("Stopping file writer");    
  terminationRequested = true;
  interrupt(); //!!!!!!
}
 

/**
 * 
 * @return
 */
private String generateTimestamp()
{    
  SimpleDateFormat format = new SimpleDateFormat ("yyyyMMdd");
  return format.format(new Date());
}

/**
 * 
 * @return
 */
private File getSuccessfulOutputFile()
{
  String fileName = conf.getWorkPath().getAbsolutePath()+File.separator+generateTimestamp()+"_"+ conf.getFileNameSuffix();
  return new File(fileName);
}


/**
 * 
 * @return
 */
private FileWriter getSuccessfulFileWriter()
{
  String generatedTimestamp = generateTimestamp();
  File outputFile = getSuccessfulOutputFile();
  if(currentFileWriter == null 
      || currentTimestamp == null 
      || !currentTimestamp.equals(generatedTimestamp) 
      || !outputFile.exists())
  {
    if(log.isFinestEnabled())log.finest("CurrentFileWriter state: "
        +(currentFileWriter == null) +" "
        +(currentTimestamp == null)+" "
        +(!generatedTimestamp.equals(currentTimestamp))+" "
        +!outputFile.exists());
    cleanSuccessfulFW();
    currentTimestamp = generatedTimestamp;      
    FileWriter fW = null;
    try
    {
      fW = new FileWriter(outputFile, true);
      currentFileWriter = fW;
      if(log.isInfoEnabled())log.info("Created file writer for file "+outputFile);
    }
    catch (IOException e)
    {
      log.severe("Could not create file writer for "+outputFile, e);
    }      
  }
  // Reuse the former file writer
  return currentFileWriter;
}  

/**
 * Main method 
 * 
 */
public void run()
{
  if(log.isFinestEnabled()) log.finest("Starting file writer");

  while(!terminationRequested /*|| !emptyQueue*/)
  {
    try
    {

      Writable cdr = null;

      while((cdr=queue.poll()/*(POLL_DELAY, TimeUnit.MILLISECONDS)*/) != null)
      {         
        if (cdr != null)
        {
          FileWriter fW = getSuccessfulFileWriter();
          
          if(fW!=null && cdr!=null)
          {
            fW.write(cdr+"\n");
            fW.flush();
            if(log.isInfoEnabled())log.info("Storing message in "+fW);
          }            
        }
      }

      {
        if(log.isFinestEnabled()) log.finest("The queue is empty....");
        if(terminationRequested) break;
        Thread.sleep(LOOP_DELAY);
      }
    }//TODO check exception rate is not very high
    catch (InterruptedException e)
    {
      log.info("Interrupted");
    }
    catch (IOException e)
    {
      log.severe("I/O exception when dumping messages",e);        
    }
    catch (Throwable e)
    {
      log.severe("Unexpected exception in writer loop ",e);
    }
  }
  //TODO drain both queues
 
  if (drainSuccessfulQueue())
  {
    
  }
  else
  {
    
  }    
  cleanSuccessfulFW();
  if(log.isInfoEnabled())log.info("File writer stopped");
}  


/**
 * To be called only when the thread is going to stop. We expect no new elements in the queue.
 * 
 * @return
 */
public boolean drainSuccessfulQueue()
{
  try
  {
    ArrayList<Writable> remainingSuccessfulList = new ArrayList<Writable>();
    queue.drainTo(remainingSuccessfulList);
    for(Writable successfulLine : remainingSuccessfulList)
    {
      if (successfulLine != null)
      {
        FileWriter fW = getSuccessfulFileWriter();
     
        if(fW!=null && successfulLine!=null)
        {
          fW.write(successfulLine+"\n");
          fW.flush();
          if(log.isInfoEnabled())log.info("Draining message in "+fW);
        }
      }      
    }
    return true;
  }
  catch (IOException e)
  {
    log.severe("I/O exception when dumping messages",e);
    return false;
  }
  catch (Throwable e)
  {
    log.severe("Unexpected exception in queue draining ",e);
    return false;
  }                
}  

/**
 * 
 */
public void cleanSuccessfulFW()
{    
  if(currentFileWriter != null)
  {
    if(log.isInfoEnabled())log.info("Cleaning messages FileWriter "+currentFileWriter);      
    try
    {
      currentFileWriter.close();
    }
    catch (IOException e)
    {
      log.severe("Could not close previous messages FileWriter",e);        
    }
    finally
    {
      currentFileWriter = null;
    }
  }      
}
}
