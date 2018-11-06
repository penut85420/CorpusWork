package nlp.cse.ntou.edu.wikipedia;

import java.io.*;
import java.util.*;

import org.oppai.*;
import org.w3c.dom.*;
import static org.oppai.LibraryUtils.log;
import nlp.cse.ntou.edu.wordSegmentation.segmentor.*;
import nlp.cse.ntou.edu.extend.SyncQueue;
import nlp.cse.ntou.edu.object.CorpusLabAnalysis;
import nlp.cse.ntou.edu.object.CorpusLabProcess;

public class NgramModel {
	static Map<String, Integer> countWF;
	static int countTotal;
	
	public static void main(String[] args) throws Exception {
		// _00_Fix();
		_01_Seg();
		// _01_CountWordFrequncy();
	}
	
	
	
	public static void _00_Fix() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\zhwiki-20150325_plainTXT\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage1_zhwiki_plaintxt_replacefix\\";
		
		new CorpusLabProcess("Replace", dirInnPath, dirOutPath, 4) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String content = LibraryIO.loadFile(fin);
					 
					// 將 "&#160" 替換成 "&#160;" 以符合 XML API 的規格
					content = content.replaceAll("&#160[^;]", "&#160;");
					
					// 移除 "\r" 並處理多餘的 "\n\n"
					content = content.replaceAll("\r", "").replaceAll("\n\n", "\n");
					
					LibraryIO.writeFile(outputDirPath + fin.getName(), content);
					System.out.println(fin.getName() + " done");
				}
			}
		}.start();
	}
	
	public static void _01_Seg() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage1_zhwiki_plaintxt_replacefix\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage2_zhwiki_seg\\";
		String alphabetListPath = "D:\\Documents\\Dictionary\\list_alphabet.txt";
		String segSymbolInfoPath = "D:\\Documents\\Dictionary\\seg_symbol_space_only.txt";
		String dictPath = "D:\\Documents\\Dictionary\\dictionary_main.txt";
		int  hashGap = 7; 
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, dictPath, hashGap);
		log(ws.MaximumMatch("斷詞測試", 1));
		
		new CorpusLabProcess("Segmentation", dirInnPath, dirOutPath) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("text");
					
					for (int i = 0; i < nd.getLength(); i++) {
						String content = nd.item(i).getTextContent();
						String s = ws.MaximumMatch(new String(content.getBytes(), "UTF-8"), 1);
						nd.item(i).setTextContent(new String(s.replaceAll("[\r\n]", " ").getBytes(), "UTF-8"));
					}
					
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					log(fin.getName());
				}
			}
		}.start();
	}
	
	public static void _02_CountWordFrequncy() throws Exception {
		countWF = new HashMap<>();
		countTotal = 0;
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage2_zhwiki_seg\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\";
		
		new CorpusLabAnalysis("Count word frequency", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList page = doc.getElementsByTagName("text");
					
					for (int i = 0; i < page.getLength(); i++) {
						String[] segs = page.item(i).getTextContent().replaceAll("  ", " ").split(" ");
						addCountTotal(segs.length);
						for (String seg : segs) {
							if (seg.isEmpty()) {
								addCountTotal(-1);
								continue;
							}
							putCountWFMap(seg);
						}
					}
					log(fin.getName());
				}
			}
		}.start();
		FileWriter fw = new FileWriter(new File(dirOutPath + "count.txt"));
		List<String> keylist = new ArrayList<>(countWF.keySet());
		Collections.sort(keylist);
		fw.write(countTotal + "\tWF\n");
		for (String k : keylist)
			fw.write(k + "\t" + countWF.get(k) + "\n");
		fw.close();
	}
	
	public static synchronized void putCountWFMap(String key) {
		Integer i = countWF.get(key);
		if (i != null) countWF.put(key, i + 1);
		else countWF.put(key, 1);
	}
	
	public static synchronized void addCountTotal(int n) {
		countTotal += n;
	}
}
