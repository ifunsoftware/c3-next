package org.aphreet.c3.web.util.collection;

import static org.aphreet.c3.web.util.collection.CollectionUtil.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CollectionFactory {

	public static <E> E[] arrayOf(E... elements){
		return elements;
	}
	
	public static <E> List<E> listOf(E... elements){
		return Arrays.asList(elements);
	}
	
	public static <E> Set<E> setOf(E... elements){
		Set<E> set = newHashSet();
		for (E e : elements) {
			set.add(e);
		}
		return set;
	}
}
