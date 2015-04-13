/* java.util.TimeZone
   Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2007
   Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package java.util;

import gnu.classpath.SystemProperties;
import java.io.File;
import sun.util.calendar.ZoneInfoFile;

/**
 * This class represents a time zone offset and handles daylight savings.
 * 
 * You can get the default time zone with <code>getDefault</code>.
 * This represents the time zone where program is running.
 *
 * Another way to create a time zone is <code>getTimeZone</code>, where
 * you can give an identifier as parameter.  For instance, the identifier
 * of the Central European Time zone is "CET".
 *
 * With the <code>getAvailableIDs</code> method, you can get all the
 * supported time zone identifiers.
 *
 * @see Calendar
 * @see SimpleTimeZone
 * @author Jochen Hoenicke
 */
public abstract class TimeZone implements java.io.Serializable, Cloneable
{

  /**
   * Constant used to indicate that a short timezone abbreviation should
   * be returned, such as "EST"
   */
  public static final int SHORT = 0;

  /**
   * Constant used to indicate that a long timezone name should be
   * returned, such as "Eastern Standard Time".
   */
  public static final int LONG = 1;

  /**
   * The time zone identifier, e.g. PST.
   */
  private String ID;

  /**
   * The default time zone, as returned by getDefault.
   */
  private static TimeZone defaultZone0;

  /**
   * Tries to get the default TimeZone for this system if not already
   * set.  It will call <code>getDefaultTimeZone(String)</code> with
   * the result of <code>System.getProperty("user.timezone")</code>.
   * If that fails it calls <code>VMTimeZone.getDefaultTimeZoneId()</code>.
   * If that also fails GMT is returned.
   */
  private static synchronized TimeZone defaultZone()
  {
    /* Look up default timezone */
    if (defaultZone0 == null) 
      {
	  TimeZone zone = null;
		
	  // Prefer System property user.timezone.
	  String tzid = System.getProperty("user.timezone");
	  if (tzid != null && !tzid.equals(""))
	      zone = getDefaultTimeZone(tzid);
		
	  // Fall back on GMT.
	  if (zone == null)
	      zone = getTimeZone ("GMT");
		
	  defaultZone0= zone;
      }
    
    return defaultZone0; 
  }
  
  private static final long serialVersionUID = 3581463369166924961L;

  /**
   * Cached copy of getAvailableIDs().
   */
  private static String[] availableIDs = null;

  /**
   * JDK 1.1.x compatibility aliases.
   */
  private static HashMap aliases0;

  /**
   * HashMap for timezones by ID.  
   */
  private static HashMap timezones0;
  /* initialize this static field lazily to overhead if
   * it is not needed: 
   */
  // Package-private to avoid a trampoline.
  static HashMap timezones()
  {
    if (timezones0 == null) 
      {
	HashMap timezones = new HashMap();
	timezones0 = timezones;

	TimeZone tz;
	tz = new SimpleTimeZone(-11000 * 3600, "MIT");
	timezones0.put("MIT", tz);
	tz = new SimpleTimeZone(-10000 * 3600, "HST");
	timezones0.put("HST", tz);
	tz = new SimpleTimeZone
	  (-9000 * 3600, "AST",
	   Calendar.MARCH, 2, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2000 * 3600);
	timezones0.put("AST", tz);
	tz = new SimpleTimeZone
	  (-8000 * 3600, "PST",
	   Calendar.MARCH, 2, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2000 * 3600);
	timezones0.put("PST", tz);
	tz = new SimpleTimeZone
	  (-7000 * 3600, "MST",
	   Calendar.MARCH, 2, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2000 * 3600);
	timezones0.put("MST", tz);
	tz = new SimpleTimeZone
	  (-6000 * 3600, "CST",
	   Calendar.MARCH, 2, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2000 * 3600);
	timezones0.put("CST", tz);
	tz = new SimpleTimeZone
	  (-5000 * 3600, "EST",
	   Calendar.MARCH, 2, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2000 * 3600);
	timezones0.put("EST", tz);
	tz = new SimpleTimeZone(-4000 * 3600, "PRT");
	timezones0.put("PRT", tz);
	tz = new SimpleTimeZone(-3000 * 3600, "AGT");
	timezones0.put("AGT", tz);
	tz = new SimpleTimeZone(0 * 3600, "GMT");
	timezones0.put("GMT", tz);
	timezones0.put("UTC", tz);
	tz = new SimpleTimeZone
	  (0 * 3600, "WET",
	   Calendar.MARCH, -1, Calendar.SUNDAY, 1000 * 3600,
	   Calendar.OCTOBER, -1, Calendar.SUNDAY, 2000 * 3600);
	timezones0.put("WET", tz);
	tz = new SimpleTimeZone
	  (1000 * 3600, "CET",
	   Calendar.MARCH, -1, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.OCTOBER, -1, Calendar.SUNDAY, 3000 * 3600);
	timezones0.put("CET", tz);
	timezones0.put("ECT", tz);
	timezones0.put("MET", tz);
	tz = new SimpleTimeZone
	  (2000 * 3600, "ART",
	   Calendar.APRIL, -1, Calendar.FRIDAY, 0 * 3600,
	   Calendar.SEPTEMBER, -1, Calendar.THURSDAY, 24000 * 3600);
	timezones0.put("ART", tz);
	tz = new SimpleTimeZone(2000 * 3600, "CAT");
	timezones0.put("CAT", tz);
	tz = new SimpleTimeZone
	  (2000 * 3600, "EET",
	   Calendar.MARCH, -1, Calendar.SUNDAY, 3000 * 3600,
	   Calendar.OCTOBER, -1, Calendar.SUNDAY, 4000 * 3600);
	timezones0.put("EET", tz);
	tz = new SimpleTimeZone(3000 * 3600, "EAT");
	timezones0.put("EAT", tz);
	tz = new SimpleTimeZone(4000 * 3600, "NET");
	timezones0.put("NET", tz);
	tz = new SimpleTimeZone(5000 * 3600, "PLT");
	timezones0.put("PLT", tz);
	tz = new SimpleTimeZone(5500 * 3600, "BST");
	timezones0.put("BST", tz);
	timezones0.put("IST", tz);
	tz = new SimpleTimeZone(7000 * 3600, "VST");
	timezones0.put("VST", tz);
	tz = new SimpleTimeZone(8000 * 3600, "CTT");
	timezones0.put("CTT", tz);
	tz = new SimpleTimeZone(9000 * 3600, "JST");
	timezones0.put("JST", tz);
	tz = new SimpleTimeZone(9500 * 3600, "ACT");
	timezones0.put("ACT", tz);
	tz = new SimpleTimeZone
	  (10000 * 3600, "AET",
	   Calendar.OCTOBER, -1, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.MARCH, -1, Calendar.SUNDAY, 3000 * 3600);
	timezones0.put("AET", tz);
	tz = new SimpleTimeZone(11000 * 3600, "SST");
	timezones0.put("SST", tz);
	tz = new SimpleTimeZone
	  (12000 * 3600, "NST",
	   Calendar.OCTOBER, 1, Calendar.SUNDAY, 2000 * 3600,
	   Calendar.MARCH, 3, Calendar.SUNDAY, 3000 * 3600);
	timezones0.put("NST", tz);
	tz = new SimpleTimeZone(12000 * 3600, "Pacific/Fiji");
	timezones0.put("Pacific/Fiji", tz);
      }
    return timezones0;
  }

  /**
   * Maps a time zone name (with optional GMT offset and daylight time
   * zone name) to one of the known time zones.  This method called
   * with the result of <code>System.getProperty("user.timezone")</code>
   * or <code>getDefaultTimeZoneId()</code>.  Note that giving one of
   * the standard tz data names from ftp://elsie.nci.nih.gov/pub/ is
   * preferred.  
   * The time zone name can be given as follows:
   * <code>(standard zone name)[(GMT offset)[(DST zone name)[DST offset]]]
   * </code>
   * <p>
   * If only a (standard zone name) is given (no numbers in the
   * String) then it gets mapped directly to the TimeZone with that
   * name, if that fails null is returned.
   * <p>
   * Alternately, a POSIX-style TZ string can be given, defining the time zone:
   * <code>std offset dst offset,date/time,date/time</code>
   * See the glibc manual, or the man page for <code>tzset</code> for details
   * of this format.
   * <p>
   * A GMT offset is the offset to add to the local time to get GMT.
   * If a (GMT offset) is included (either in seconds or hours) then
   * an attempt is made to find a TimeZone name matching both the name
   * and the offset (that doesn't observe daylight time, if the
   * timezone observes daylight time then you must include a daylight
   * time zone name after the offset), if that fails then a TimeZone
   * with the given GMT offset is returned (whether or not the
   * TimeZone observes daylight time is ignored), if that also fails
   * the GMT TimeZone is returned.
   * <p>
   * If the String ends with (GMT offset)(daylight time zone name)
   * then an attempt is made to find a TimeZone with the given name and
   * GMT offset that also observes (the daylight time zone name is not
   * currently used in any other way), if that fails a TimeZone with
   * the given GMT offset that observes daylight time is returned, if
   * that also fails the GMT TimeZone is returned.
   * <p>
   * Examples: In Chicago, the time zone id could be "CST6CDT", but
   * the preferred name would be "America/Chicago".  In Indianapolis
   * (which does not have Daylight Savings Time) the string could be
   * "EST5", but the preferred name would be "America/Indianapolis".
   * The standard time zone name for The Netherlands is "Europe/Amsterdam",
   * but can also be given as "CET-1CEST".
   */
  static TimeZone getDefaultTimeZone(String sysTimeZoneId)
  {
    String stdName = null;
    int stdOffs;
    int dstOffs;
    try
      {
	int idLength = sysTimeZoneId.length();

	int index = 0;
	int prevIndex;
	char c;

	// get std
	do
	  c = sysTimeZoneId.charAt(index);
	while (c != '+' && c != '-' && c != ',' && c != ':'
	       && ! Character.isDigit(c) && c != '\0' && ++index < idLength);

	if (index >= idLength)
	  return getTimeZoneInternal(sysTimeZoneId);

	stdName = sysTimeZoneId.substring(0, index);
	prevIndex = index;

	// get the std offset
	do
	  c = sysTimeZoneId.charAt(index++);
	while ((c == '-' || c == '+' || c == ':' || Character.isDigit(c))
	       && index < idLength);
	if (index < idLength)
	  index--;

	{ // convert the dst string to a millis number
	    String offset = sysTimeZoneId.substring(prevIndex, index);
	    prevIndex = index;

	    if (offset.charAt(0) == '+' || offset.charAt(0) == '-')
	      stdOffs = parseTime(offset.substring(1));
	    else
	      stdOffs = parseTime(offset);

	    if (offset.charAt(0) == '-')
	      stdOffs = -stdOffs;

	    // TZ timezone offsets are positive when WEST of the meridian.
	    stdOffs = -stdOffs;
	}

	// Done yet? (Format: std offset)
	if (index >= idLength)
	  {
	    // Do we have an existing timezone with that name and offset?
	    TimeZone tz = getTimeZoneInternal(stdName);
	    if (tz != null)
	      if (tz.getRawOffset() == stdOffs)
		return tz;

	    // Custom then.
	    return new SimpleTimeZone(stdOffs, stdName);
	  }

	// get dst
	do
	  c = sysTimeZoneId.charAt(index);
	while (c != '+' && c != '-' && c != ',' && c != ':'
	       && ! Character.isDigit(c) && c != '\0' && ++index < idLength);

	// Done yet? (Format: std offset dst)
	if (index >= idLength)
	  {
	    // Do we have an existing timezone with that name and offset 
	    // which has DST?
	    TimeZone tz = getTimeZoneInternal(stdName);
	    if (tz != null)
	      if (tz.getRawOffset() == stdOffs && tz.useDaylightTime())
		return tz;

	    // Custom then.
	    return new SimpleTimeZone(stdOffs, stdName);
	  }

	// get the dst offset
	prevIndex = index;
	do
	  c = sysTimeZoneId.charAt(index++);
	while ((c == '-' || c == '+' || c == ':' || Character.isDigit(c))
	       && index < idLength);
	if (index < idLength)
	  index--;

	if (index == prevIndex && (c == ',' || c == ';'))
	  {
	    // Missing dst offset defaults to one hour ahead of standard
	    // time.  
	    dstOffs = stdOffs + 60 * 60 * 1000;
	  }
	else
	  { // convert the dst string to a millis number
	    String offset = sysTimeZoneId.substring(prevIndex, index);
	    prevIndex = index;

	    if (offset.charAt(0) == '+' || offset.charAt(0) == '-')
	      dstOffs = parseTime(offset.substring(1));
	    else
	      dstOffs = parseTime(offset);

	    if (offset.charAt(0) == '-')
	      dstOffs = -dstOffs;

	    // TZ timezone offsets are positive when WEST of the meridian.
	    dstOffs = -dstOffs;
	  }

	// Done yet? (Format: std offset dst offset)
	// FIXME: We don't support DST without a rule given. Should we?
	if (index >= idLength)
	  {
	    // Time Zone existing with same name, dst and offsets?
	    TimeZone tz = getTimeZoneInternal(stdName);
	    if (tz != null)
	      if (tz.getRawOffset() == stdOffs && tz.useDaylightTime()
	          && tz.getDSTSavings() == (dstOffs - stdOffs))
		return tz;

	    return new SimpleTimeZone(stdOffs, stdName);
	  }

	// get the DST rule
	if (sysTimeZoneId.charAt(index) == ','
	    || sysTimeZoneId.charAt(index) == ';')
	  {
	    index++;
	    int offs = index;
	    while (sysTimeZoneId.charAt(index) != ','
	           && sysTimeZoneId.charAt(index) != ';')
	      index++;
	    String startTime = sysTimeZoneId.substring(offs, index);
	    index++;
	    String endTime = sysTimeZoneId.substring(index);

	    index = startTime.indexOf('/');
	    int startMillis;
	    int endMillis;
	    String startDate;
	    String endDate;
	    if (index != -1)
	      {
		startDate = startTime.substring(0, index);
		startMillis = parseTime(startTime.substring(index + 1));
	      }
	    else
	      {
		startDate = startTime;
		// if time isn't given, default to 2:00:00 AM.
		startMillis = 2 * 60 * 60 * 1000;
	      }
	    index = endTime.indexOf('/');
	    if (index != -1)
	      {
		endDate = endTime.substring(0, index);
		endMillis = parseTime(endTime.substring(index + 1));
	      }
	    else
	      {
		endDate = endTime;
		// if time isn't given, default to 2:00:00 AM.
		endMillis = 2 * 60 * 60 * 1000;
	      }

	    int[] start = getDateParams(startDate);
	    int[] end = getDateParams(endDate);
	    return new SimpleTimeZone(stdOffs, stdName, start[0], start[1],
	                              start[2], startMillis, end[0], end[1],
	                              end[2], endMillis, (dstOffs - stdOffs));
	  }
      }

    // FIXME: Produce a warning here?
    catch (IndexOutOfBoundsException _)
      {
      }
    catch (NumberFormatException _)
      {
      }

    return null;
  }

  /**
   * Parses and returns the params for a POSIX TZ date field,
   * in the format int[]{ month, day, dayOfWeek }, following the
   * SimpleTimeZone constructor rules.
   */
  private static int[] getDateParams(String date)
  {
    int[] dayCount = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
    int month;

    if (date.charAt(0) == 'M' || date.charAt(0) == 'm')
      {
	int day;

	// Month, week of month, day of week
	month = Integer.parseInt(date.substring(1, date.indexOf('.')));
	int week = Integer.parseInt(date.substring(date.indexOf('.') + 1,
	                                           date.lastIndexOf('.')));
	int dayOfWeek = Integer.parseInt(date.substring(date.lastIndexOf('.')
	                                                + 1));
	if (week == 5)
	  day = -1; // last day of month is -1 in java, 5 in TZ
	else
	  // first day of week starting on or after.
	  day = (week - 1) * 7 + 1;

	dayOfWeek++; // Java day of week is one-based, Sunday is first day.
	month--; // Java month is zero-based.
	return new int[] { month, day, dayOfWeek };
      }

    // julian day, either zero-based 0<=n<=365 (incl feb 29)
    // or one-based 1<=n<=365 (no feb 29)
    int julianDay; // Julian day, 

    if (date.charAt(0) != 'J' || date.charAt(0) != 'j')
      {
	julianDay = Integer.parseInt(date.substring(1));
	julianDay++; // make 1-based
	// Adjust day count to include feb 29.
	dayCount = new int[]
	           {
	             0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335
	           };
      }
    else
      // 1-based julian day
      julianDay = Integer.parseInt(date);

    int i = 11;
    while (i > 0)
      if (dayCount[i] < julianDay)
	break;
      else
	i--;
    julianDay -= dayCount[i];
    month = i;
    return new int[] { month, julianDay, 0 };
  }

  /**
   * Parses a time field hh[:mm[:ss]], returning the result
   * in milliseconds. No leading sign.
   */
  private static int parseTime(String time)
  {
    int millis = 0;
    int i = 0;

    while (i < time.length())
      if (time.charAt(i) == ':')
	break;
      else
	i++;
    millis = 60 * 60 * 1000 * Integer.parseInt(time.substring(0, i));
    if (i >= time.length())
      return millis;

    int iprev = ++i;
    while (i < time.length())
      if (time.charAt(i) == ':')
	break;
      else
	i++;
    millis += 60 * 1000 * Integer.parseInt(time.substring(iprev, i));
    if (i >= time.length())
      return millis;

    millis += 1000 * Integer.parseInt(time.substring(++i));
    return millis;
  }

  /**
   * Gets the time zone offset, for current date, modified in case of 
   * daylight savings.  This is the offset to add to UTC to get the local
   * time.
   * @param era the era of the given date
   * @param year the year of the given date
   * @param month the month of the given date, 0 for January.
   * @param day the day of month
   * @param dayOfWeek the day of week
   * @param milliseconds the millis in the day (in local standard time)
   * @return the time zone offset in milliseconds.
   */
  public abstract int getOffset(int era, int year, int month,
				int day, int dayOfWeek, int milliseconds);

  /**
   * Get the time zone offset for the specified date, modified in case of
   * daylight savings.  This is the offset to add to UTC to get the local
   * time.
   * @param date the date represented in millisecends
   * since January 1, 1970 00:00:00 GMT.
   * @since 1.4
   */
  public int getOffset(long date)
  {
    return (inDaylightTime(new Date(date))
            ? getRawOffset() + getDSTSavings()
            : getRawOffset());
  }
  
  /**
   * Gets the time zone offset, ignoring daylight savings.  This is
   * the offset to add to UTC to get the local time.
   * @return the time zone offset in milliseconds.  
   */
  public abstract int getRawOffset();

  /**
   * Sets the time zone offset, ignoring daylight savings.  This is
   * the offset to add to UTC to get the local time.
   * @param offsetMillis the time zone offset to GMT.
   */
  public abstract void setRawOffset(int offsetMillis);

  /**
   * Gets the identifier of this time zone. For instance, PST for
   * Pacific Standard Time.
   * @returns the ID of this time zone.  
   */
  public String getID()
  {
    return ID;
  }

  /**
   * Sets the identifier of this time zone. For instance, PST for
   * Pacific Standard Time.
   * @param id the new time zone ID.
   * @throws NullPointerException if <code>id</code> is <code>null</code>
   */
  public void setID(String id)
  {
    if (id == null)
      throw new NullPointerException();
    
    this.ID = id;
  }

  /**
   * This method returns a string name of the time zone suitable
   * for displaying to the user.  The string returned will be the long
   * description of the timezone in the current locale.  The name
   * displayed will assume daylight savings time is not in effect.
   *
   * @return The name of the time zone.
   */
  public final String getDisplayName()
  {
    return (getDisplayName(false, LONG));
  }

  /**
   * This method returns a string name of the time zone suitable
   * for displaying to the user.  The string returned will be of the
   * specified type in the current locale. 
   *
   * @param dst Whether or not daylight savings time is in effect.
   * @param style <code>LONG</code> for a long name, <code>SHORT</code> for
   * a short abbreviation.
   *
   * @return The name of the time zone.
   */
  public String getDisplayName(boolean dst, int style)
  {
      if (style != SHORT && style != LONG) {
	  throw new IllegalArgumentException("Illegal style: " + style);
      }
      
      return ZoneInfoFile.toCustomID(getRawOffset());
  }

  /** 
   * Returns true, if this time zone uses Daylight Savings Time.
   */
  public abstract boolean useDaylightTime();

  /**
   * Returns true, if the given date is in Daylight Savings Time in this
   * time zone.
   * @param date the given Date.
   */
  public abstract boolean inDaylightTime(Date date);

  /**
   * Gets the daylight savings offset.  This is a positive offset in
   * milliseconds with respect to standard time.  Typically this
   * is one hour, but for some time zones this may be half an our.
   * <p>The default implementation returns 3600000 milliseconds
   * (one hour) if the time zone uses daylight savings time
   * (as specified by {@link #useDaylightTime()}), otherwise
   * it returns 0.
   * @return the daylight savings offset in milliseconds.
   * @since 1.4
   */
  public int getDSTSavings ()
  {
    return useDaylightTime () ? 3600000 : 0;
  }

  /**
   * Gets the TimeZone for the given ID.
   * @param ID the time zone identifier.
   * @return The time zone for the identifier or GMT, if no such time
   * zone exists.
   */
  private static TimeZone getTimeZoneInternal(String ID)
  {
    // First check timezones hash
    TimeZone tz = null;
    TimeZone tznew = null;
    for (int pass = 0; pass < 2; pass++)
      {
	synchronized (TimeZone.class)
	  {
	    tz = (TimeZone) timezones().get(ID);
	    if (tz != null)
	      {
		if (!tz.getID().equals(ID))
		  {
		    // We always return a timezone with the requested ID.
		    // This is the same behaviour as with JDK1.2.
		    tz = (TimeZone) tz.clone();
		    tz.setID(ID);
		    // We also save the alias, so that we return the same
		    // object again if getTimeZone is called with the same
		    // alias.
		    timezones().put(ID, tz);
		  }
		return tz;
	      }
	    else if (tznew != null)
	      {
		timezones().put(ID, tznew);
		return tznew;
	      }
	  }

	return null;
      }

    return null;
  }

  /**
   * Gets the TimeZone for the given ID.
   * @param ID the time zone identifier.
   * @return The time zone for the identifier or GMT, if no such time
   * zone exists.
   */
  public static TimeZone getTimeZone(String ID)
  {
    // Check for custom IDs first
    if (ID.startsWith("GMT") && ID.length() > 3)
      {
	int pos = 3;
	int offset_direction = 1;

	if (ID.charAt(pos) == '-')
	  {
	    offset_direction = -1;
	    pos++;
	  }
	else if (ID.charAt(pos) == '+')
	  {
	    pos++;
	  }

	try
	  {
	    int hour, minute;

	    String offset_str = ID.substring(pos);
	    int idx = offset_str.indexOf(":");
	    if (idx != -1)
	      {
		hour = Integer.parseInt(offset_str.substring(0, idx));
		minute = Integer.parseInt(offset_str.substring(idx + 1));
	      }
	    else
	      {
		int offset_length = offset_str.length();
		if (offset_length <= 2)
		  {
		    // Only hour
		    hour = Integer.parseInt(offset_str);
		    minute = 0;
		  }
		else
		  {
		    // hour and minute, not separated by colon
		    hour = Integer.parseInt
		      (offset_str.substring(0, offset_length - 2));
		    minute = Integer.parseInt
		      (offset_str.substring(offset_length - 2));
		  }
	      }

	    // Custom IDs have to be normalized
	    StringBuffer sb = new StringBuffer(9);
	    sb.append("GMT");

	    sb.append(offset_direction >= 0 ? '+' : '-');
	    sb.append((char) ('0' + hour / 10));
	    sb.append((char) ('0' + hour % 10));
	    sb.append(':');
	    sb.append((char) ('0' + minute / 10));
	    sb.append((char) ('0' + minute % 10));
	    ID = sb.toString();

	    return new SimpleTimeZone((hour * (60 * 60 * 1000)
				       + minute * (60 * 1000))
				      * offset_direction, ID);
	  }
	catch (NumberFormatException e)
	  {
	  }
      }

    TimeZone tz = getTimeZoneInternal(ID);
    if (tz != null)
      return tz;

    return new SimpleTimeZone(0, "GMT");
  }

  /**
   * Gets the available IDs according to the given time zone
   * offset.  
   * @param rawOffset the given time zone GMT offset.
   * @return An array of IDs, where the time zone has the specified GMT
   * offset. For example <code>{"Phoenix", "Denver"}</code>, since both have
   * GMT-07:00, but differ in daylight savings behaviour.
   */
  public static String[] getAvailableIDs(int rawOffset)
  {
    synchronized (TimeZone.class)
      {
	HashMap h = timezones();
	int count = 0;
	    Iterator iter = h.entrySet().iterator();
	    while (iter.hasNext())
	      {
		// Don't iterate the values, since we want to count
		// doubled values (aliases)
		Map.Entry entry = (Map.Entry) iter.next();
		if (((TimeZone) entry.getValue()).getRawOffset() == rawOffset)
		  count++;
	      }

	    String[] ids = new String[count];
	    count = 0;
	    iter = h.entrySet().iterator();
	    while (iter.hasNext())
	      {
		Map.Entry entry = (Map.Entry) iter.next();
		if (((TimeZone) entry.getValue()).getRawOffset() == rawOffset)
		  ids[count++] = (String) entry.getKey();
	      }
	    return ids;
      }
  }

  private static int getAvailableIDs(File d, String prefix, ArrayList list)
    {
      String[] files = d.list();
      int count = files.length;
      boolean top = prefix.length() == 0;
      list.add (files);
      for (int i = 0; i < files.length; i++)
	{
	  if (top
	      && (files[i].equals("posix")
		  || files[i].equals("right")
		  || files[i].endsWith(".tab")
		  || aliases0.get(files[i]) != null))
	    {
	      files[i] = null;
	      count--;
	      continue;
	    }

	  File f = new File(d, files[i]);
	  if (f.isDirectory())
	    {
	      count += getAvailableIDs(f, prefix + files[i]
				       + File.separatorChar, list) - 1;
	      files[i] = null;
	    }
	  else
	    files[i] = prefix + files[i];
	}
      return count;
    }

  /**
   * Gets all available IDs.
   * @return An array of all supported IDs.
   */
  public static String[] getAvailableIDs()
  {
    synchronized (TimeZone.class)
      {
	HashMap h = timezones();
	return (String[]) h.keySet().toArray(new String[h.size()]);
      }
  }

  /**
   * Returns the time zone under which the host is running.  This
   * can be changed with setDefault.
   *
   * @return A clone of the current default time zone for this host.
   * @see #setDefault
   */
  public static TimeZone getDefault()
  {
    return (TimeZone) defaultZone().clone();
  }

  public static void setDefault(TimeZone zone)
  {
    // Hmmmm. No Security checks?
    defaultZone0 = zone;
  }

  /**
   * Test if the other time zone uses the same rule and only
   * possibly differs in ID.  This implementation for this particular
   * class will return true if the raw offsets are identical.  Subclasses
   * should override this method if they use daylight savings.
   * @return true if this zone has the same raw offset
   */
  public boolean hasSameRules(TimeZone other)
  {
    return other.getRawOffset() == getRawOffset();
  }

  /**
   * Returns a clone of this object.  I can't imagine, why this is
   * useful for a time zone.
   */
  public Object clone()
  {
    try
      {
	return super.clone();
      }
    catch (CloneNotSupportedException ex)
      {
	return null;
      }
  }
}
