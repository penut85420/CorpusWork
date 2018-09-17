package edu.ntou.cs.nlp.wikipedia;

import java.io.*;
import java.util.zip.*;

import org.oppai.io.LibraryIO;
import org.w3c.dom.*;
import edu.ntou.cs.nlp.wordSegmentation.segmentor.WordSegmentor;

public class WikiPlaintxtProcess {
	
	/**
	 * Wiki Version
	 * Date: 20150325
	 */
	
	static int step = 1;
	
	public static void main(String[] args) throws Exception {
		switch (step) {
		case 1:
			Step1_SimpleReplace();
		case 2:
			Step2_PlaintextSegmentation();
		case 3:
			Step3_Retrive();
		case 4:
			Step4_PlainTextSigleSeg();
		case 5:
			Step5_RetriveSingleWord();
		}
	}
	
	public static void Step1_SimpleReplace() throws Exception {
		String inputDirPath = "wiki\\stage0_zhwiki_plaintxt_complete\\";
		String outputDirPath = "wiki\\stage1_zhwiki_plaintxt_replacefix\\";
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		for (File fin : inputDir.listFiles()) {
			System.out.println("Processing " + fin.getName());
			String content = LibraryIO.loadFile(fin);
			 
			// 將 "&#160" 替換成 "&#160;" 以符合 XML API 的規格
			content = content.replaceAll("&#160[^;]", "&#160;");
			
			// 移除 "\r" 並處理多餘的 "\n\n"
			content = content.replaceAll("\r", "").replaceAll("\n\n", "\n");
			
			LibraryIO.writeFile(outputDirPath + fin.getName(), content);
		}
		
		System.out.println("Simple replace done");
	}

 	public static void Step2_PlaintextSegmentation() throws Exception {
		String inputDirPath = "wiki\\stage1_zhwiki_plaintxt_replacefix\\";
		String outputDirPath = "wiki\\stage2_zhwiki_seg\\";
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		String alphabetListPath = "D:\\Documents\\JavaWorkspace\\WordSegmentation\\data\\list_alphabet.txt";
		String segSymbolInfoPath = "D:\\Documents\\JavaWorkspace\\WordSegmentation\\data\\seg_symbol.txt";
		String dictPath = "D:\\Documents\\JavaWorkspace\\WordSegmentation\\data\\dictionary_main.txt";
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, dictPath);
		
		for (File fin : inputDir.listFiles()) {
			System.out.println("Segmenting " + fin.getName());
			Document doc = LibraryIO.loadXML(fin);
			NodeList context = doc.getElementsByTagName("text");
			
			for (int i = 0; i < context.getLength(); i++) {
				// 先將 title 移除
				String seged = ws.MaximumMatch(context.item(i).getTextContent().replaceAll("={2,}+[^=]+?=+", ""), 1).replaceAll("[\r\n]", "");
				context.item(i).setTextContent(new String(seged.getBytes("UTF-8"), "UTF-8"));
			}
			
			LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
		}
		
		System.out.println("Plain text segmentation done");
	}

 	public static void Step3_Retrive() throws Exception {
 		String inputDirPath = "wiki\\stage2_zhwiki_seg\\";
		String outputDirPath = "wiki\\zhwiki_zip\\";
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(outputDir, "text_word.zip")));
		zos.putNextEntry(new ZipEntry("text"));
		
		for (File fin : inputDir.listFiles()) {
			System.out.println("Writing " + fin.getName());
			Document doc = LibraryIO.loadXML(fin);
			NodeList nd = doc.getElementsByTagName("text");
			
			for (int i = 0; i < nd.getLength(); i++) {
				zos.write(nd.item(i).getTextContent().getBytes("UTF-8"));
			}
		}
		
		zos.finish();
		zos.close();
		
		System.out.println("Retrive done");
 	}
 	
 	public static void Step4_PlainTextSigleSeg() throws Exception {
		String inputDirPath = "wiki\\stage2_zhwiki_seg\\";
		String outputDirPath = "wiki\\stage3_zhwiki_single_word\\";
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		for (File fin : inputDir.listFiles()) {
			System.out.println("Single segmentin " + fin.getName());
			Document doc = LibraryIO.loadXML(fin);
			NodeList contents = doc.getElementsByTagName("text");
			
			for (int i = 0; i < contents.getLength(); i++) {
				String content = contents.item(i).getTextContent();
				StringBuilder sb = new StringBuilder();
				for (char c : content.toCharArray())
					if (c != ' ') {
						sb.append(c);
						sb.append(' ');
					}
				contents.item(i).setTextContent(new String(sb.toString().getBytes(), "UTF-8"));
			}
			
			LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
		}
	}
 	
 	public static void Step5_RetriveSingleWord() throws Exception {
 		String inputDirPath = "wiki\\stage3_zhwiki_single_word\\";
		String outputDirPath = "wiki\\zhwiki_zip\\";
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(outputDir, "text_single.zip")));
		zos.putNextEntry(new ZipEntry("text"));
		
		for (File fin : inputDir.listFiles()) {
			System.out.println("Writing " + fin.getName());
			Document doc = LibraryIO.loadXML(fin);
			NodeList nd = doc.getElementsByTagName("text");
			
			for (int i = 0; i < nd.getLength(); i++)
				zos.write(nd.item(i).getTextContent().getBytes("UTF-8"));
		}
		
		zos.finish();
		zos.close();
		
		System.out.println("Retrive done");
 	}
}
