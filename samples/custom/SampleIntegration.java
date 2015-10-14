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

package com.jkool.tnt4j.streams.samples.custom;

import java.util.HashMap;
import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import com.jkool.tnt4j.streams.inputs.FeederThread;
import org.apache.log4j.Logger;

/**
 * Sample integration of TNT4J-Streams into an application.
 *
 * @version $Revision: 2 $
 */
public final class SampleIntegration
{
  private static final Logger logger = Logger.getLogger (SampleIntegration.class);

  /**
   * Configure feeders and parsers, and run each feed in its own thread.
   */
  public static void loadConfigAndRun (String cfgFileName)
  {
    ThreadGroup feederThreads = new ThreadGroup ("Feeders");
    try
    {
      StreamsConfig cfg;
      if (cfgFileName == null || cfgFileName.length () == 0)
      { cfg = new StreamsConfig (); }
      else
      { cfg = new StreamsConfig (cfgFileName); }
      HashMap<String, ActivityFeeder> feederMap = cfg.getFeeders ();
      if (feederMap == null || feederMap.size () == 0)
      { throw new IllegalStateException ("No Activity Feeders found in configuration"); }
      for (Map.Entry<String, ActivityFeeder> f : feederMap.entrySet ())
      {
        String feederName = f.getKey ();
        ActivityFeeder feeder = f.getValue ();
        FeederThread ft = new FeederThread (feederThreads, feeder, feederName);
        ft.start ();
      }
    }
    catch (Throwable t)
    {
      logger.error (t.getMessage (), t);
    }
  }

  /**
   * The following can be used if using the default configuration file
   * with a single feeder.
   */
  public static void simpleConfigAndRun (String cfgFileName)
  {
    try
    {
      StreamsConfig cfg = new StreamsConfig ();
      ActivityFeeder feeder = cfg.getFeeder ("FeederName");
      FeederThread ft = new FeederThread (feeder);
      ft.start ();
    }
    catch (Throwable t)
    {
      logger.error (t.getMessage (), t);
    }
  }
}
