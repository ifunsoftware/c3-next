package org.aphreet.c3.search.index.server;

import java.util.Map;

/**
 * Удаленный интерфейс для реализации извлечения текста 
 * и метаданных с помощью библиотеки Apache Tika.
 * @author Ildar Ashirbaev
 */
public interface SearchProvider {
	
	/**
	 * Производит извлечение метаданных и текста.
	 * @param fileName имя файла, из которго необходимо извлечь текст и метаданные
	 * @return <code>Map</code> c метаданными, извлеченными из документа.
	 */
	public Map<String, String> extract(String fileName);
}
