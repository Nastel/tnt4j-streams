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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import org.apache.log4j.Logger;

/**
 * Sample custom parser.
 */
public class SampleParser extends ActivityParser
{
  private static final Logger logger = Logger.getLogger (SampleParser.class);

  /**
   * Defines field separator.
   */
  protected String fieldDelim = ",";

  /**
   * Sets custom properties for this parser
   *
   * @param props properties to set
   *
   * @throws Throwable indicates error with properties
   */
  @Override
  public void setProperties (Collection<Map.Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    { return; }
    super.setProperties (props);
    for (Map.Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (logger.isDebugEnabled ())
      { logger.debug ("Setting " + name + " to '" + value + "'"); }
      if (StreamsConfig.PROP_FLD_DELIM.equalsIgnoreCase (name))
      {
        fieldDelim = value;
      }
    }
  }

  /**
   * Returns whether this parser supports the given format of the activity data.
   *
   * @param data data object whose class is to be verified
   *
   * @return {@code true} if this parser can process data in the
   * specified format, {@code false} otherwise
   */
  @Override
  public boolean isDataClassSupported (Object data)
  {
    return (String.class.isInstance (data));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ActivityInfo parse (ActivityFeeder feeder, Object data) throws IllegalStateException, ParseException
  {
    if (fieldDelim == null)
    { throw new IllegalStateException ("SampleParser: field delimiter not specified or empty"); }
    if (data == null)
    { return null; }
    // Get next string to parse
    String dataStr = getNextString (data);
    if (dataStr == null || dataStr.length () == 0)
    { return null; }
    if (logger.isDebugEnabled ())
    { logger.debug ("Parsing: " + dataStr); }
    String[] fields = dataStr.split (fieldDelim);
    if (fields == null || fields.length == 0)
    {
      if (logger.isDebugEnabled ())
      { logger.debug ("Did not find any fields in input string"); }
      return null;
    }
    if (logger.isDebugEnabled ())
    { logger.debug ("Split input into " + fields.length + " fields"); }
    ActivityInfo ai = new ActivityInfo ();
    ActivityField field = null;
    Object value = null;
    try
    {
      // save entire activity string as message data
      field = new ActivityField (ActivityFieldType.ActivityData);
      applyFieldValue (ai, field, dataStr);
      // apply fields for parser
      for (Map.Entry<ActivityField, ArrayList<ActivityFieldLocator>> fieldEntry : fieldMap.entrySet ())
      {
        value = null;
        field = fieldEntry.getKey ();
        ArrayList<ActivityFieldLocator> locations = fieldEntry.getValue ();
        if (locations != null)
        {
          if (locations.size () == 1)
          {
            // field value is based on single raw data location, get the value of this location
            value = getLocatorValue (feeder, locations.get (0), fields);
          }
          else
          {
            // field value is based on contatenation of several raw data locations,
            // build array to hold data from each location
            Object[] values = new Object[locations.size ()];
            for (int l = 0; l < locations.size (); l++)
            { values[l] = getLocatorValue (feeder, locations.get (l), fields); }
            value = values;
          }
        }
        applyFieldValue (ai, field, value);
      }
    }
    catch (Exception e)
    {
      ParseException pe = new ParseException ("Failed parsing data for field " + field, 0);
      pe.initCause (e);
      throw pe;
    }
    return ai;
  }

  private Object getLocatorValue (ActivityFeeder feeder, ActivityFieldLocator locator, String[] fields) throws ParseException
  {
    Object val = null;
    if (locator != null)
    {
      String locStr = locator.getLocator ();
      if (locStr != null && locStr.length () > 0)
      {
        if (locator.getBuiltInType () == ActivityFieldLocatorType.FeederProp)
        {
          val = feeder.getProperty (locStr);
        }
        else
        {
          int loc = Integer.parseInt (locStr);
          if (loc <= fields.length)
          { val = fields[loc - 1].trim (); }
        }
      }
      val = locator.formatValue (val);
    }
    return val;
  }
}
