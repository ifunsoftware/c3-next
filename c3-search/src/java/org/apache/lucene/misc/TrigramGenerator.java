package org.apache.lucene.misc;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2004 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Lucene" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Lucene", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class to help generating trigrams
 * 
 * @author Jean-Fran�ois Halleux
 * @version $version$
 */
class TrigramGenerator {

	//BUFFERSIZE must be at least 2
	private static final int BUFFERSIZE = 1024;
	//A set to hold the trigram listeners
	private Set tlset = new TreeSet();
	private Reader reader;

	public TrigramGenerator() {
		;
	}

	public TrigramGenerator(Reader r) {
		setReader(r);
	}

	public void setReader(Reader r) {
		reader = r;
	}

	/*
	 * TrigramListener are notified whenever a new trigram
	 * is generated from the reader
	 */
	public void addTrigramListener(TrigramListener tl) {
		tlset.add(tl);
	}

	/*
	 * Process the reader and notifies regitered listeners
	 * Reader is closed when processing is finished
	 */
	public void start() throws IOException {
		start(Integer.MAX_VALUE);
	}

	/*
	 * Process the reader and notifies regitered listeners
	 * Reader is closed when processing is finished
	 */
	public void start(int maxGrams) throws IOException {
		char[] buf = new char[BUFFERSIZE];
		char[] trigram = new char[3];
		char charmin2 = 0;
		char charmin1 = 0;

		int nbread = reader.read(buf, 0, BUFFERSIZE);

		boolean firstbuf = true;
		int totalGrams = 0;

		while (nbread != -1) {
			if (!firstbuf) {
				trigram[0] = charmin2;
				trigram[1] = charmin1;
				trigram[2] = buf[0];
				totalGrams =
					addTrigram(new String(trigram), totalGrams, maxGrams);
				if (totalGrams == -1)
					break;
				if (nbread >= 2) {
					trigram[0] = charmin1;
					trigram[1] = buf[0];
					trigram[2] = buf[1];
					totalGrams =
						addTrigram(new String(trigram), totalGrams, maxGrams);
					if (totalGrams == -1)
						break;
				}
			}

			for (int i = 2; i < nbread; i++) {
				String s = new String(buf, i - 2, 3);
				totalGrams = addTrigram(new String(s), totalGrams, maxGrams);
				if (totalGrams == -1)
					break;
			}

			if (totalGrams == -1)
				break;

			if (nbread == BUFFERSIZE) {
				charmin2 = buf[nbread - 2];
				charmin1 = buf[nbread - 1];
			}
			firstbuf = false;
			nbread = reader.read(buf, 0, BUFFERSIZE);
		}

		reader.close();
	}

	/*
	 * A new trigram has been found : notifies every listener
	 */
	private int addTrigram(String trigram, int totalGrams, int maxGrams) {
		if (totalGrams < maxGrams) {
			Iterator it = tlset.iterator();
			Trigram t = new Trigram(trigram);
			while (it.hasNext()) {
				((TrigramListener) it.next()).addTrigram(t);
			}
			return ++totalGrams;
		} else {
			return -1;
		}
	}

	/**
	 * Generates .tri files from a directory location
	 * Each subDirectory of fileLocation should be named xx where
	 * xx is the ISO Language code of the files contained in xx
	 * Language files should be plain text
	 * xx.tri files are generated at the same directory level as
	 * fileLocation
	 */
	public void generateTriFiles(String fileLocation) throws IOException {
		File f = new File(fileLocation);
		if (f.isDirectory()) {
			String[] files = f.list();
			for (int i = 0; i < files.length; i++) {
				File fd = new File(f.getAbsolutePath() + "\\" + files[i]);
				if (fd.isDirectory()) {
					processLanguageDir(fileLocation, fd);
				}
			}
		}
	}

	/**
	 * Process a single language directory by processing all
	 * its language files and storing a xx.tri file at fileLocation
	 */
	private void processLanguageDir(String fileLocation, File f)
		throws IOException {
		Trigrams ts = new Trigrams();
		TrigramGenerator tg = new TrigramGenerator();
		tg.addTrigramListener(ts);

		String[] files = f.list();
		for (int i = 0; i < files.length; i++) {
			Reader r =
				new BufferedReader(
						new InputStreamReader(
								new FileInputStream(f.getAbsolutePath() + "\\" + files[i]), 
								"UTF-16BE"));
			tg.setReader(r);
			tg.start();
		}
		ts.saveToFile(fileLocation + "\\" + f.getName() + ".tri");
	}
	
	public static void main(String[] args) throws IOException {
		
		new TrigramGenerator().generateTriFiles("C:\\java\\c3\\languageGuesser\\languageSamples");
		System.out.println("trigram files generated");
	}

}