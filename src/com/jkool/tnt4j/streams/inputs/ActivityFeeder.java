/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.inputs;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map.Entry;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.types.GatewayProtocolTypes;
import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.tracker.Tracker;
import org.apache.log4j.Logger;

/**
 * <p>Base class that all activity feeders must extend.  It provides some base functionality
 * useful for all activity feeders.</p>
 * <p>All activity feeders should support the following properties:
 * <ul>
 * <li>DateTime - default date/time to associate with activities</li>
 * </ul>
 * </p>
 *
 * @version $Revision: 8 $
 */
public abstract class ActivityFeeder implements Runnable
{
  private Logger logger = Logger.getLogger (ActivityFeeder.class);

  /**
   * Feeder thread running this feeder.
   */
  protected FeederThread ownerThread;

  /**
   * List of parsers being used by feeder.
   */
  protected LinkedList<ActivityParser> parsers = new LinkedList<ActivityParser> ();

  /**
   * Type of protocol used to deliver processed activity data to destination resource.
   */
  protected GatewayProtocolTypes taConnType;

  /**
   * For communication-based protocols, host name or IP address of TransactionWorks Analyzer.
   */
  protected String taHost;   //TODO: review if still needed for jKool

  /**
   * For communication-based protocols, port TransactionWorks Analyzer is accepting connections on.
   */
  protected int taPort = -1;   //TODO: review if still needed for jKool

  /**
   * For communication-based protocols, access token to use when TransactionWorks Analyzer
   * requires authenticated connections.
   */
  protected String taAccessToken;   //TODO: review if still needed for jKool

  /**
   * For {@code FILE} protocol, name of file to write processed activity data.
   */
  protected String taFileName;

  /**
   * For communication-based protocols, host name or IP address of proxy to use to connect to TransactionWorks Analyzer.
   */
  protected String taProxyHost;   //TODO: review if still needed for jKool

  /**
   * For communication-based protocols, port for proxy to use to connect to TransactionWorks Analyzer.
   */
  protected int taProxyPort = 0;     //TODO: review if still needed for jKool

  /**
   * For secure connections to TransactionWorks Analyzer, path to keystore containing Analyzer's SSL certificate
   */
  protected String taKeystore;   //TODO: review if still needed for jKool

  /**
   * For secure connections to TransactionWorks Analyzer, password for keystore containing Analyzer's SSL certificate
   */
  protected String taKeystorePwd;     //TODO: review if still needed for jKool

  /**
   * Used to deliver processed activity data to destination.
   */
  protected Tracker tracker;

  /**
   * Delay between retries to submit data package to jKool Cloud Service if some transmission failure occurs, in milliseconds.
   */
  protected int conn_retry_interval = 15000;

  /**
   * Initializes ActivityFeeder.
   */
  protected ActivityFeeder (Logger logger)
  {
    this.logger = logger;
  }

  /**
   * Get the thread owning this feeder.
   *
   * @return owner thread
   */
  public FeederThread getOwnerThread ()
  {
    return ownerThread;
  }

  /**
   * Set the thread owning this feeder.
   *
   * @param ownerThread thread owning this feeder
   */
  public void setOwnerThread (FeederThread ownerThread)
  {
    this.ownerThread = ownerThread;
  }

  /**
   * Set properties for activity feeder.  This method is invoked by the configuration loader
   * in response to the {@code property} configuration elements.  It is invoked once per
   * feeder definition, with all property names and values specified for this feeder.
   * Subclasses should generally override this method to process custom properties, and
   * invoke the base class method to handle any built-in properties.
   *
   * @param props properties to set
   *
   * @throws Throwable indicates error with properties
   */
  public void setProperties (Collection<Entry<String, String>> props) throws Throwable
  {
  }

  /**
   * Get value of specified property.  If subclasses override {@link #setProperties(Collection)},
   * they should generally override this method as well to return the value of custom properties,
   * and invoke the base class method to handle any built-in properties.
   * <p>The default implementation handles the {@value com.jkool.tnt4j.streams.configure.StreamsConfig#PROP_DATETIME}
   * property and returns the value of the {@link #getDate()} method.
   *
   * @param name name of property whose value is to be retrieved
   *
   * @return value for property, or {@code null} if property does not exist
   */
  public Object getProperty (String name)
  {
    if (StreamsConfig.PROP_DATETIME.equals (name))
    { return getDate (); }
    return null;
  }

  /**
   * Initialize the feeder.
   * <p>This method is called by default {@code run} method to perform any necessary
   * initializations before the feeder starts processing, including verifying that
   * all required properties are set.  If subclasses override this method to perform
   * any custom initializations, they must call the base class method.  If subclass
   * also overrides the {@code run} method, it must call this at start of {@code run}
   * method before entering into processing loop.
   *
   * @throws Throwable indicates that feeder is not configured properly and
   *                   cannot continue.
   */
  protected void initialize () throws Throwable
  {
    TrackerConfig simConfig = DefaultConfigFactory.getInstance ().getConfig ("com.jkool.tnt4j.streams");
    tracker = TrackingLogger.getInstance (simConfig.build ());
  }

  /**
   * Adds the specified parser to the list of parsers being used by this feeder.
   *
   * @param parser parser to add
   */
  public void addParser (ActivityParser parser)
  {
    parsers.add (parser);
  }

  /**
   * Get the position in the source activity data currently being processed.  For line-based
   * data sources, this is generally the line number.  Subclasses should override this
   * to provide meaningful information, if relevant.  The default implementation just
   * returns 0.
   *
   * @return current position in activity data source being processed
   */
  public int getActivityPosition ()
  {
    return 0;
  }

  /**
   * Get the next activity data item to be processed.  All subclasses must implement this.
   *
   * @return next activity data item, or {@code null} if there is no next item
   *
   * @throws Throwable if any errors occurred getting next item
   */
  public abstract Object getNextItem () throws Throwable;

  /**
   * Gets the next processed activity.
   * <p>Default implementation simply calls {@link #getNextItem()}
   * to get next activity data item and calls {@link #applyParsers(Object)} to process it.
   *
   * @return next activity item
   *
   * @throws Throwable if error getting next activity data item or processing it
   */
  protected ActivityInfo getNextActivity () throws Throwable
  {
    ActivityInfo ai = null;
    Object data = getNextItem ();
    try
    {
      if (data == null)
      {
        halt (); // no more data items to process
      }
      else
      { ai = applyParsers (data); }
    }
    catch (ParseException e)
    {
      int position = getActivityPosition ();
      ParseException pe = new ParseException ("Failed to process activity data at position " + position, position);
      pe.initCause (e);
      throw pe;
    }
    return ai;
  }

  /**
   * Applies all defined parsers for this feeder that support the format that the raw
   * activity data is in the order added until one successfully matches the specified
   * activity data item.
   *
   * @param data activity data item to process
   *
   * @return processed activity data item, or {@code null} if activity data item does not match rules for any parsers
   *
   * @throws IllegalStateException if parser fails to run
   * @throws ParseException        if any parser encounters an error parsing the activity data
   */
  protected ActivityInfo applyParsers (Object data) throws IllegalStateException, ParseException
  {
    if (data == null)
    { return null; }
    for (ActivityParser parser : parsers)
    {
      if (parser.isDataClassSupported (data))
      {
        ActivityInfo ai = parser.parse (this, data);
        if (ai != null)
        { return ai; }
      }
    }
    return null;
  }

  /**
   * Gets type of protocol being used to delivery processed activity data
   * to required destination.
   *
   * @return protocol being used to deliver processed activity data
   */
  public GatewayProtocolTypes getTaConnType ()
  {
    return taConnType;
  }

  /**
   * Sets type of protocol to delivery processed activity data to
   * required destination.
   *
   * @param taConnType the protocol to use
   */
  public void setTaConnType (GatewayProtocolTypes taConnType)
  {
    this.taConnType = taConnType;
  }

  /**
   * Gets the host name or IP address that processed activity data is
   * being sent to when using a communications-based protocol.
   *
   * @return destination host name or IP address
   */
  public String getTaHost ()
  {
    return taHost;
  }

  /**
   * Sets the host name or IP address to send processed activity data
   * when using a communications-based protocol.
   *
   * @param taHost destination host name or IP address
   */
  public void setTaHost (String taHost)
  {
    this.taHost = taHost;
  }

  /**
   * Gets the port that destination is listening on when using a
   * communications-based protocol to send processed activity data.
   *
   * @return destination port number
   */
  public int getTaPort ()
  {
    return taPort;
  }

  /**
   * Sets the port that destination is listening on when using a
   * communications-based protocol to send processed activity data.
   *
   * @param taPort destination port number
   */
  public void setTaPort (int taPort)
  {
    this.taPort = taPort;
  }

  /**
   * Gets the access token being used when establishing authenticated connections.
   *
   * @return access token for authenticating connections
   */
  public String getTaAccessToken ()
  {
    return taAccessToken;
  }

  /**
   * Sets the access token to use when establishing authenticated connections.
   *
   * @param taAccessToken access token for authenticating connections
   */
  public void setTaAccessToken (String taAccessToken)
  {
    this.taAccessToken = taAccessToken;
  }

  /**
   * Gets the host name or IP address of proxy to use to reach destination that
   * processed activity data is being sent to when using a communications-based protocol.
   *
   * @return proxy host name or IP address
   */
  public String getTaProxyHost ()
  {
    return taProxyHost;
  }

  /**
   * Sets the host name or IP address of proxy to use to reach destination that
   * processed activity data is being sent to when using a communications-based protocol
   *
   * @param taProxyHost proxy host name or IP address
   */
  public void setTaProxyHost (String taProxyHost)
  {
    this.taProxyHost = taProxyHost;
  }

  /**
   * Gets the port for proxy to use to reach destination that processed activity data
   * is being sent to when using a communications-based protocol.
   *
   * @return proxy port number
   */
  public int getTaProxyPort ()
  {
    return taProxyPort;
  }

  /**
   * Sets the port for proxy to use to reach destination that processed activity data
   * is being sent to when using a communications-based protocol.
   *
   * @param taProxyPort proxy port number
   */
  public void setTaProxyPort (int taProxyPort)
  {
    this.taProxyPort = taProxyPort;
  }

  /**
   * Gets the name of the file that processed activity data is being
   * written to. Valid only when protocol is {@code FILE}.
   *
   * @return name of file activity data is being written to
   */
  public String getTaFileName ()
  {
    return taFileName;
  }

  /**
   * Gets the path to the keystore containing Transaction Analyzer's SSL certificate.
   *
   * @return path to SSL certificate keystore
   */
  public String getTaKeystore ()   //TODO: review if still needed for jKool
  {
    return taKeystore;
  }

  /**
   * Sets the path to the keystore containing Transaction Analyzer's SSL certificate.
   *
   * @param taKeystore path to SSL certificate keystore
   */
  public void setTaKeystore (String taKeystore)
  {
    this.taKeystore = taKeystore;
  }

  /**
   * Gets the password being used for the keystore containing Transaction Analyzer's SSL certificate.
   *
   * @return password for SSL certificate keystore
   */
  public String getTaKeystorePwd ()
  {
    return taKeystorePwd;
  }

  /**
   * Sets the password to use for the keystore containing Transaction Analyzer's SSL certificate.
   *
   * @param taKeystorePwd password for SSL certificate keystore
   */
  public void setTaKeystorePwd (String taKeystorePwd)
  {
    this.taKeystorePwd = taKeystorePwd;
  }

  /**
   * Sets the name of the file that processed activity data is being
   * written to when using {@code FILE} protocol.
   *
   * @param taFileName name of file to write data to
   */
  public void setTaFileName (String taFileName)
  {
    this.taFileName = taFileName;
  }

  /**
   * Gets the default date/time to use for activity entries that do not contain a date.
   * Default implementation returns the current date.
   *
   * @return default date/time to use for activity entries
   */
  public Date getDate ()
  {
    return new Date ();
  }

  /**
   * Signals that this feeder should stop processing so that controlling
   * thread will terminate.
   */
  public void halt ()
  {
    ownerThread.halt ();
  }

  /**
   * Indicates whether this feeder has stopped.
   *
   * @return {@code true} if feeder has stopped processing, {@code false} otherwise
   */
  public boolean isHalted ()
  {
    return ownerThread.isStopRunning ();
  }

  /**
   * Cleanup the feeder.
   * <p>This method is called by default {@code run} method to perform any necessary
   * cleanup before the feeder stops processing, releasing any resources created
   * by {@link #initialize()} method.  If subclasses override this method to perform
   * any custom cleanup, they must call the base class method.  If subclass also
   * overrides the {@code run} method, it must call this at end of {@code run}
   * method before returning.
   */
  protected void cleanup ()
  {
    if (tracker != null)
    {
      try {tracker.close ();} catch (IOException e) {}
      tracker = null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run ()
  {
    logger.info ("Starting ...");
    if (ownerThread == null)
    { throw new IllegalStateException ("Owner thread has not been set"); }
    try
    {
      initialize ();
      while (!isHalted ())
      {
        try
        {
          while (!isHalted () && !tracker.isOpen ())
          {
            try
            {
              tracker.open ();
            }
            catch (IOException ioe)
            {
              logger.error ("Failed to connect to " + tracker, ioe);
              tracker.close ();
              logger.info ("Will retry in " + conn_retry_interval / 1000 + " seconds");
              if (!isHalted ())
              { StreamsThread.sleep (conn_retry_interval); }
            }
          }
          ActivityInfo ai = getNextActivity ();
          if (ai == null)
          {
            if (isHalted ())
            {
              logger.info ("Data stream ended ...");
            }
            else
            {
              logger.info ("No Parser accepted Message !..");
            }
            halt ();
          }
          else
          { ai.recordActivity (tracker); }
        }
        catch (IllegalStateException ise)
        {
          logger.error ("Failed to record activity at position " + getActivityPosition () + ": " + ise.getMessage (), ise);
          halt ();
        }
        catch (Exception e)
        {
          logger.error ("Failed to record activity at position " + getActivityPosition () + ": " + e.getMessage (), e);
        }
      }
    }
    catch (Throwable t)
    {
      logger.error ("Failed to record activity: " + t.getMessage (), t);
    }
    finally
    {
      cleanup ();
      logger.info ("Thread " + Thread.currentThread ().getName () + " ended");
    }
  }

  /**
   * Get the debug logger being used for this class instance.
   *
   * @return debug logger
   */
  public Logger getDbgLogger ()
  {
    return logger;
  }
}
