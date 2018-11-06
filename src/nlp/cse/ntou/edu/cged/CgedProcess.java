package nlp.cse.ntou.edu.cged;

import org.oppai.*;
import org.w3c.dom.*;
import java.io.*;

import nlp.cse.ntou.edu.wordSegmentation.segmentor.WordSegmentor;

public class CgedProcess {
	public static void main(String[] args) throws Exception {
		cgedDetectionLabel();
	}
	
	public static void cgedSingleSegmention() throws Exception {
		String inputFilePath = "D:\\Documents\\Corpus\\cged\\CGED16_TOCFL_Train_All.xml";
		String outputFilePath = "D:\\Documents\\Corpus\\cged\\CGED16_TOCFL_Train_All_Single_Seged.xml";
		Document doc = LibraryIO.loadXML(inputFilePath);
		
		NodeList nl_text = doc.getElementsByTagName("TEXT");
		NodeList nl_corr = doc.getElementsByTagName("CORRECTION");
		int nl_len = nl_text.getLength();
		for (int i = 0; i < nl_len; i++) {
			// Print progress
			if (i % 100 == 0 || i == nl_len - 1)
				System.out.printf("%5d/%5d\n", i, nl_len - 1);
			
			// Segmenting text in <TEXT>
			String content = nl_text.item(i).getTextContent().trim();
			StringBuilder sb = new StringBuilder();
			for (char c : content.toCharArray())
				sb.append(c + " ");
			nl_text.item(i).setTextContent('\n' + new String(sb.toString().getBytes(), "UTF-8").trim() + '\n');
			
			// Segmenting text in <CORRECTION>
			content = nl_corr.item(i).getTextContent().trim();
			sb = new StringBuilder();
			for (char c : content.toCharArray())
				sb.append(c + " ");
			nl_corr.item(i).setTextContent('\n' + new String(sb.toString().getBytes(), "UTF-8").trim() + '\n');
		}
		
		LibraryIO.writeXML(outputFilePath, doc);
		System.out.println("Single segmention done");
	}
	
	public static void cgedMaximumMatchSegmention() throws Exception {
		String sMainDictPath = "D:\\Documents\\Dictionary\\dictionary_main.txt";
		String sAlphabetListPath = "D:\\Documents\\Dictionary\\list_alphabet.txt";
		String sSegSymbolInfoPath = "D:\\Documents\\Dictionary\\seg_symbol_space_only.txt";
		
		WordSegmentor ws = new WordSegmentor(sAlphabetListPath, sSegSymbolInfoPath, sMainDictPath);
		
		String inputFilePath = "D:\\Documents\\Corpus\\cged\\CGED16_TOCFL_Train_All.xml";
		String outputFilePath = "D:\\Documents\\Corpus\\cged\\CGED16_TOCFL_Train_All_Seged.xml";
		Document doc = LibraryIO.loadXML(inputFilePath);
		NodeList nl_text = doc.getElementsByTagName("TEXT");
		NodeList nl_corr = doc.getElementsByTagName("CORRECTION");
		
		int nl_len = nl_text.getLength();
		
		for (int i = 0; i < nl_len; i++) {
			// Print progress
			if (i % 100 == 0 || i == nl_len - 1)
				System.out.printf("%5d/%5d\n", i, nl_len - 1);
			
			// Segmenting text in <TEXT>
			String content = nl_text.item(i).getTextContent().trim();
			nl_text.item(i).setTextContent('\n' + ws.MaximumMatch(content, WordSegmentor.HASH_MODE).trim() + '\n');
			
			// Segmenting text in <CORRECTION>
			content = nl_corr.item(i).getTextContent().trim();
			nl_corr.item(i).setTextContent('\n' + ws.MaximumMatch(content, WordSegmentor.HASH_MODE).trim() + '\n');
		}
		
		LibraryIO.writeXML(outputFilePath, doc);
		System.out.println("Maximum match segmention done");
	}
	
	public static void cgedDetectionLabel() throws Exception {
		String inputFilePath = "D:\\Documents\\Corpus\\cged\\CGED16_TOCFL_Train_All_Single_Seged.xml";
		String trainFilePath = "D:\\Documents\\Corpus\\cged\\DetectionLevel\\CGED_Train.txt";
		String lebelFilePath = "D:\\Documents\\Corpus\\cged\\DetectionLevel\\CGED_Label.txt";
		
		new File(trainFilePath).mkdirs();
		
		Document doc = LibraryIO.loadXML(inputFilePath);
		NodeList nl_text = doc.getElementsByTagName("TEXT");
		NodeList nl_corr = doc.getElementsByTagName("CORRECTION");
		int nl_len = nl_text.getLength();
		// nl_len = 10;
		StringBuilder sb_train = new StringBuilder();
		StringBuilder sb_label = new StringBuilder();
		for (int i = 0; i < nl_len; i++) {
			sb_train.append(nl_text.item(i).getTextContent().trim() + "\n");
			sb_label.append("1\n");
		}
		
		for (int i = 0; i < nl_len; i++) {
			sb_train.append(nl_corr.item(i).getTextContent().trim() + "\n");
			sb_label.append("0\n");
		}
		LibraryIO.writeFile(trainFilePath, sb_train.toString());
		LibraryIO.writeFile(lebelFilePath, sb_label.toString());
		System.out.println("Detection level label done");
	}
	
	public static void cgedIdentificationLable() {
		
	}
}
