package org.aphreet.c3.web.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

	private static String hash (String input, String algorithmName){

		if (input == null || input.length() == 0) { 
			return ""; 
		} 

		StringBuffer hexString = new StringBuffer(); 

		try { 
			MessageDigest md = MessageDigest.getInstance(algorithmName); 
			md.update(input.getBytes()); 
			byte[] hash = md.digest(); 

			for (int i = 0; i < hash.length; i++) { 
				if ((0xff & hash[i]) < 0x10) { 
					hexString.append("0" + Integer.toHexString((0xFF & hash[i]))); 
				} 
				else { 
					hexString.append(Integer.toHexString(0xFF & hash[i])); 
				} 
			} 
		} 
		catch (NoSuchAlgorithmException e) {
			System.out.println("alhorithm does not exists");
		} 

		return hexString.toString(); 
	}
	
	public static String getMD5Hash(String message){
		return hash(message, "MD5");
	}
	
	public static String getSHAHash(String message){
		return hash(message, "SHA");
	}
}
