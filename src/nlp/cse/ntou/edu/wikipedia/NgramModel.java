package nlp.cse.ntou.edu.wikipedia;

import java.io.*;
import java.util.*;

import org.oppai.*;
import org.w3c.dom.*;

import static org.oppai.LibraryUtils.log;
import nlp.cse.ntou.edu.wordSegmentation.segmentor.*;
import nlp.cse.ntou.edu.WordSegmentation.BigramSeg;
import nlp.cse.ntou.edu.WordSegmentation.WordSeg;
import nlp.cse.ntou.edu.extend.SyncQueue;
import nlp.cse.ntou.edu.object.CorpusLabAnalysis;
import nlp.cse.ntou.edu.object.CorpusLabProcess;

public class NgramModel {
	static Map<String, Integer> countWF;
	static Map<String, Integer> unigram_id;
	static int countTotal;
	
	public static void main(String[] args) throws Exception {
		// _00_Fix();
		// _01_Seg();
		// _02_CountWordFrequncy();
		// _03_CountBigramFrequency();
		// _04_BigramSeg("");
		_03_CountBigramFrequency();
		// _04_BigramSeg("1");
		// _03_CountBigramFrequency(2);
		// log(isSymbol("。"));
		// makeUnigramList();
		LibraryUtils.bgm();
		String alphabetListPath = "D:\\Documents\\Dictionary\\list_alphabet.txt";
		String segSymbolInfoPath = "D:\\Documents\\Dictionary\\seg_symbol_space_only.txt";
		String dictPath = "D:\\Documents\\Dictionary\\dictionary_main.txt";
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, dictPath, 5);
		log(ws.MaximumMatch("《天涯明月刀》遊戲以古龍同名小說為改編，將小說原著人物還原遊戲中並重現小說中各式經典武俠招式。故事背景是在白玉京失蹤後的武俠生態，為了抗衡被方龍香獨攬大權的青龍會，四大高手各懷抱負先後創立了寒江城、帝王州、水龍吟與萬里殺四大盟會，並廣招太白、神威、唐門、丐幫、天香、五毒、真武、神刀八荒門派好手，一同抗衡青龍會的暴行。", 1));
		_02_CountWordFrequncy();
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
		
		FileWriter fw = new FileWriter(new File(dirOutPath + "count_uni.txt"));
		List<String> keylist = new ArrayList<>(countWF.keySet());
		Collections.sort(keylist);
		fw.write(countTotal + "\tWF\n");
		int id = 1000;
		for (String k : keylist)
			if (!k.isEmpty())
				fw.write(k + "\t" + countWF.get(k) + "\t" + id++ + "\n");
		fw.close();
	}
	
	public static void _03_CountBigramFrequency() throws Exception {
		makeUnigramList();
		countWF = new HashMap<>();
		countTotal = 0;
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage2_zhwiki_seg\\";
//		String dataBiCount = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\bi_count" + v + ".dat";
//		String dataBiIndex = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\bi_index" + v + ".dat";
		String biOutput = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\bi\\";
		new CorpusLabAnalysis("Count word frequency", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList page = doc.getElementsByTagName("text");
					
					for (int i = 0; i < page.getLength(); i++) {
						String[] segs = page.item(i).getTextContent().replaceAll("  ", " ").split(" ");
						addCountTotal(segs.length - 1);
						for (int j = 0; j < segs.length - 1; j++) {
							if (segs[j].isEmpty() || segs[j+1].isEmpty()) {
								addCountTotal(-1);
								continue;
							}
							if (isSymbol(segs[j]) || isSymbol(segs[j+1])) {
								addCountTotal(-1);
								continue;
							}
							putCountWFMap(segs[j] + " " + segs[j+1]);
						}
					}
					log(fin.getName());
				}
			}
		}.start();
		
//		File fbi = new File(dataBiCount);
//		FileWriter fw = new FileWriter(fbi);
//		FileWriter iw = new FileWriter(new File(dataBiIndex));
		List<String> keylist = new ArrayList<>(countWF.keySet());
		log("Sorting");
		Collections.sort(keylist);
		log("Sorted");
		
		for (String k : keylist) {
			
			String[] nk = k.split(" ");
			Integer i = unigram_id.get(nk[0]);
			if (i == null) log(nk[0]);
//			File dd = new File(biOutput + unigram_id.get(nk[0]));
//			dd.mkdirs();
//			dd = new File(biOutput + unigram_id.get(nk[0]) + "\\" + unigram_id.get(nk[1]) + ".txt");
//			FileWriter fw = new FileWriter(dd);
//			fw.write(countWF.get(k));
//			fw.close();
		}
		
//		fw.write(countTotal + "\n");
//		String pre_key = "";
//		log("Writing bi count");
//		for (String k : keylist) {
//			String[] nk = k.split(" ");
//			if (!nk[0].equals(pre_key)) {
//				pre_key = nk[0];
//				fw.flush();
//				iw.write(nk[0] + '\t' + fbi.length() + '\n');
//			}
//			fw.write(nk[1] + "\t" + countWF.get(k) + "\n");
//		}
//		fw.close();
//		iw.close();
//		log("done");
	}
	
	public static void _04_BigramSeg(String id) throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage1_zhwiki_plaintxt_replacefix\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\stage4_zhwiki_bigram_seg" + id + "\\";
		String bigramPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\bi_count" + id + ".txt";
		String bindexPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\bi_index" + id + ".txt";
		String dictpath = "D:\\Documents\\Dictionary\\dictionary_main.txt";
		WordSeg ws = new WordSeg(dictpath);
		BigramSeg b = new BigramSeg(bigramPath, bindexPath, ws);
		b.seg("暖身");
		
		new CorpusLabProcess("Segmentation", dirInnPath, dirOutPath, 1) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("text");
					
					for (int i = 0; i < nd.getLength(); i++) {
						String content = nd.item(i).getTextContent();
						String s = b.seg(new String(content.getBytes(), "UTF-8"));
						nd.item(i).setTextContent(new String(s.replaceAll("[\r\n]", " ").getBytes(), "UTF-8"));
					}
					
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					log(fin.getName());
				}
			}
		}.start();
	}
	
	public static boolean isSymbol(String s) {
		String s1 = "!!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
		String s2 = "！＼、＂＃＄＆（）＊＋，－。／：；＜＝＞？＠「」︿＿｛｝｜～《》";
		if (s1.contains(s)) return true;
		if (s2.contains(s)) return true;
		return false;
	}
	
	public static synchronized void putCountWFMap(String key) {
		Integer i = countWF.get(key);
		if (i != null) countWF.put(key, i + 1);
		else countWF.put(key, 1);
	}
	
	public static synchronized void addCountTotal(int n) {
		countTotal += n;
	}
	
	public static void makeUnigramList() throws Exception {
		unigram_id = new HashMap<>();
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_20150325\\count_uni.txt";
		BufferedReader br = new BufferedReader(new FileReader(new File(dirOutPath)));
		br.readLine();
		while (br.ready()) {
			String[] ss = br.readLine().split("\\s");
			unigram_id.put(ss[0], Integer.parseInt(ss[2]));
		}
		br.close();
	}
}
