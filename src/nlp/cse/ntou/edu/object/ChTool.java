package nlp.cse.ntou.edu.object;

import java.util.HashMap;
import java.util.Map;

import org.oppai.LibraryIO;

public class ChTool {
	static Map<String, String> ctcs;
	static boolean init = init();
	
	public static boolean init() {
		try {
			String filePath = "D:\\Documents\\Dictionary\\mapping_ctcs.txt";
			ctcs = new HashMap<>();
			
			String[] cs_lines = LibraryIO.loadFileAsLines(filePath);
			
			for (String lines : cs_lines) {
				String[] seg = lines.split("\t");
				ctcs.put(seg[0], seg[1]);
			}
		} catch (Exception e) { e.printStackTrace(); }
		
		return true;
	}
	
	public static String toSimplified(String s) {
		String[] ss = s.split("");
		String rvalue = "";
		for (String sss : ss) {
			String c = ctcs.get(sss);
			if (c == null) {
				c = sss;
				ctcs.put(c, c);
			}
				
			rvalue += c;
		}
		return rvalue;
	}
	
	public static void main(String[] args) {
		System.out.println(toSimplified("王者天下三山島 (煙台)"));
	}
}
