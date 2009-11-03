package org.aphreet.c3.web.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class CollectionUtil {
	
	public static <E> ArrayList<E> newArrayList(){ 
		return new ArrayList<E>();
	}
	
	public static <E> LinkedList<E> newLinkedList(){
		return new LinkedList<E>();
	}
	
	public static <E> HashSet<E> newHashSet(){
		return new HashSet<E>();
	}
	
	public static <K, V> HashMap<K, V> newHashMap(){
		return new HashMap<K, V>();
	}
	
	public static <E> E top(Collection<E> a){
		
		if(a.isEmpty()){
			return null;
		}
		
		return a.iterator().next();
	}
}
