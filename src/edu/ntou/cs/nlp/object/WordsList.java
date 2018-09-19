package edu.ntou.cs.nlp.object;

import java.util.*;

public class WordsList {
	static Map<String, Map<String, Boolean>> mWordList;
	static boolean isInit = initWordList();
	static boolean isDebug = false;
	
	public static boolean initWordList() {
		mWordList = new HashMap<>();
		initCallList();
		initAlsoList();
		
		return true;
	}
	
	public static void initCallList() {
		String[] callList = "又稱 舊稱 簡稱 或稱 俗稱 又名 別稱 也稱 亦稱".split(" ");
		
		Map<String, Boolean> call = new HashMap<>();
		
		for (String s : callList)
			call.put(s, true);
		
		mWordList.put("call", call);
	}
	
	public static void initAlsoList() {
		String[] alsoList = "又 或 也 亦".split(" ");
		
		Map<String, Boolean> also = new HashMap<>();
		
		for (String s : alsoList)
			also.put(s, true);
		
		mWordList.put("also", also);
	}
	
	public static boolean is(String key, String s, boolean verbose) {
		Map<String, Boolean> map = mWordList.get(key);
		
		if (map == null) {
			if (verbose) System.out.println(key + " not found in words list");
			return false;
		}
		
		Boolean b = map.get(s);
		
		if (b == null) {
			if (verbose) System.out.println(s + " is not in " + key + " list");
			return false;
		}
		
		return true;
	}
	
	public static boolean isCall(String s) { return is("call", s, isDebug); }
	
	public static boolean isAlso(String s) { return is("also", s, isDebug); }
}
