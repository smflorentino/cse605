/* SystemProperties.java -- Manage the System properties.
   Copyright (C) 2004, 2005 Free Software Foundation

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


package gnu.classpath;

import java.util.Properties;

/**
 * The class manages the System properties. This class is only available to
 * privileged code (i.e. code loaded by the bootstrap class loader) and
 * therefore doesn't do any security checks.
 * This class is separated out from java.lang.System to simplify bootstrap
 * dependencies and to allow trusted code a simple and efficient mechanism
 * to access the system properties.
 */
public class SystemProperties
{
  /**
   * Stores the current system properties. This can be modified by
   * {@link #setProperties(Properties)}, but will never be null, because
   * setProperties(null) sucks in the default properties.
   */
  private static Properties properties;

  static {
      FCSystemProperties.prePreInit();
  }

  /**
   * The default properties. Once the default is stabilized,
   * it should not be modified;
   * instead it is cloned when calling <code>setProperties(null)</code>.
   */
  private static final Properties defaultProperties = new Properties();

  static
  {
    FCSystemProperties.preInit(defaultProperties);

    defaultProperties.put("fijicore.version",
                          com.fiji.fivm.Config.VERSION);

    // Network properties
    if (defaultProperties.get("http.agent") == null)
      {
	String userAgent = ("fijicore/"+com.fiji.fivm.Config.VERSION);
	defaultProperties.put("http.agent", userAgent);
      }

    // 8859_1 is a safe default encoding to use when not explicitly set
    if (defaultProperties.get("file.encoding") == null)
      defaultProperties.put("file.encoding", "8859_1");

    // XXX FIXME - Temp hack for old systems that set the wrong property
    if (defaultProperties.get("java.io.tmpdir") == null)
      defaultProperties.put("java.io.tmpdir",
                            defaultProperties.get("java.tmpdir"));

    // FIXME: we need a better way to handle this.
    // For instance, having a single VM class for each OS might help.
    if (defaultProperties.get("gnu.classpath.mime.types.file") == null
        && "Linux".equals(defaultProperties.get("os.name")))
      defaultProperties.put("gnu.classpath.mime.types.file",
                            "/etc/mime.types");

    FCSystemProperties.postInit(defaultProperties);

    // Note that we use clone here and not new.  Some programs assume
    // that the system properties do not have a parent.
    properties = (Properties) defaultProperties.clone();
  }

  public static String getProperty(String name)
  {
    return properties.getProperty(name);
  }

  public static String getProperty(String name, String defaultValue)
  {
    return properties.getProperty(name, defaultValue);
  }

  public static String setProperty(String name, String value)
  {
    return (String) properties.setProperty(name, value);
  }

  public static Properties getProperties()
  {
    return properties;
  }

  public static void setProperties(Properties properties)
  {
    if (properties == null)
      {
        // Note that we use clone here and not new.  Some programs
        // assume that the system properties do not have a parent.
        properties = (Properties)defaultProperties.clone();
      }

    SystemProperties.properties = properties;
  }

  /**
   * Removes the supplied system property and its current value.
   * If the specified property does not exist, nothing happens.
   * 
   * @throws NullPointerException if the property name is null.
   * @return the value of the removed property, or null if no
   *         such property exists.
   */
  public static String remove(String name)
  {
    return (String) properties.remove(name);
  }

}
