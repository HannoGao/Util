/*---------------------------------------------------------------
*  Copyright 2015 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense.pdf)
*----------------------------------------------------------------*/

package org.rsna.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedList;

/**
 * A ClassLoader that finds classes in an array of JARs,
 * giving precedence in delegation to the JARs.
 */
public class JarClassLoader extends URLClassLoader {
	
	/**
	 * Get a JarClassLoader initialized to a set of files and directories.
	 * This method places all individual files in the array of JARs.
	 * It searches any directories to find all the files that end in ".jar"
	 * (not case sensitive) and places them in the array as well.
	 * @param files the array of File objects to include in the array of JARs.
	 * The items in the array can be individual JAR files or directories.
	 * @return a JarClassLoader initialized to the set of JARs found in the
	 * files array.
	 */
	public static JarClassLoader getInstance(File[] files) {
		URL[] urls = getJars(files);
		return new JarClassLoader(urls);
	}

	public JarClassLoader(URL[] urls) {
		super(urls);
	}

	public JarClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	protected synchronized Class loadClass(String classname, boolean resolve)
			throws ClassNotFoundException {

		//If the class is already loaded, return it.
		Class theClass = findLoadedClass(classname);
		if (theClass != null) return theClass;

		//Delegate the loading of non-RSNA classes
		if (!classname.startsWith("org.rsna")) {
			try { theClass = findBaseClass(classname); }
			catch (ClassNotFoundException cnfe) {
				theClass = findClass(classname);
			}
		}

		//If it didn't look like a system class, then try the jars first.
		//This violates the normal delegation mechanism, but it is done
		//to ensure that this classloader becomes the classloader of all
		//classes that it can load, even those that could have been loaded
		//by the application classloader from the classpath.
		else {
			try { theClass = findClass(classname); }
			catch (ClassNotFoundException cnfe) {
				theClass = findBaseClass(classname);
			}
		}
		if (resolve) { resolveClass(theClass); }
		return theClass;
	}

	private Class findBaseClass(String name) throws ClassNotFoundException {
		return (getParent() == null) ? findSystemClass(name) : getParent().loadClass(name);
	}

	private static URL[] getJars(File[] files) {
		LinkedList<URL> urlList = new LinkedList<URL>();
		for (File file : files) addJars(urlList, file);
		URL[] urls = new URL[urlList.size()];
		urls = urlList.toArray(urls);
		return urls;
	}
	
	private static void addJars(LinkedList<URL> urlList, File file) {
		if (file.exists()) {
			if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
				try { urlList.add( file.toURI().toURL() ); }
				catch (Exception skip) {
					System.out.println("Unable to add file to classpath: "+file);
				}
			}
			else if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (File f : files) addJars(urlList, f);
			}
		}
	}
}
