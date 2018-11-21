//package nlp.cse.ntou.edu.wikipedia;
//
//import static nlp.cse.ntou.edu.wikipedia.WikitextProcess.createTextTag;
//
//import java.io.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import javax.xml.parsers.DocumentBuilderFactory;
//
//import org.oppai.LibraryIO;
//import org.w3c.dom.*;
//
//import nlp.cse.ntou.edu.extend.SyncQueue;
//import nlp.cse.ntou.edu.object.CorpusLabProcess;
//
//public class EnwikiProcess {
//	final static String[] InputDirPath = {
//		"D:\\Documents\\Corpus\\wiki\\enwiki\\Step0_enwiki_split\\",
//		"D:\\Documents\\Corpus\\wiki\\enwiki\\Step1_enwiki_retrieve\\",
//		"D:\\Documents\\Corpus\\wiki\\enwiki\\Step2_enwiki_filtered\\",
//	};
//	
//	public static void main(String[] args) throws Exception {
//		/**
//		 * 需先 Run 過 s0_split.py
//		 * 若檔名格式是 %.4d 要再跑一次 s1_rename.py
//		 * 抽取 wikidump 中的 pages 並切割成小檔案
//		 */
//		
//		Step0_Retrieve();
//		
//		/**
//		 * Step0_Retrieve 執行完後要執行 s2_merge.py
//		 * 將 pages 合併在一起
//		 */
//	}
//	
//	/**
//	 * 將 Page Category 抽取出來
//	 * @throws Exception
//	 */
//	
//	public static void Step0_Retrieve() throws Exception {
//		new CorpusLabProcess("Retrieve", InputDirPath[0], InputDirPath[1], 8) {
//			
//			@Override
//			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
//				while (!sq.isEmpty()) {
//					File fin = sq.poll();
//					
//					Document doc = LibraryIO.loadXML(fin);
//					Document fout = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
//					
//					// Input node
//					NodeList nd = doc.getElementsByTagName("page");
//					
//					// Output pageset
//					Element pageset = fout.createElement("pageset");
//					fout.appendChild(pageset);
//					
//					for (int i = 0; i < nd.getLength(); i++) {
//						// Output page
//						Element page = fout.createElement("page");
//		
//						// Deal with title
//						Element e = (Element) nd.item(i);
//						String title = e.getElementsByTagName("title").item(0).getTextContent();
//						if (title.toLowerCase().contains("list of")) continue;
//						if (title.toLowerCase().contains(":")) continue;
//						
//						// Deal with text
//						String text = e.getElementsByTagName("text").item(0).getTextContent();
//						
//						// Skip if disambiguation or redirect
//						if (text.toLowerCase().contains("{{disambiguation}}")) continue;
//						if (text.toLowerCase().contains("{{disambiguation|")) continue;
//						if (text.toLowerCase().startsWith("#redirect")) continue;
//						
//						Element category = fout.createElement("category");
//						Matcher m = Pattern.compile("(?i)\\[\\[category:(.*?)\\]\\]").matcher(text);
//						while (m.find()) {
//							String ss = m.group(1);
//							int idx = ss.indexOf("|");
//							if (idx < 0) idx = ss.length();
//							category.appendChild(createTextTag(fout, "cat", new String(ss.substring(0, idx).getBytes(), "UTF-8")));
//						}
//						
//						page.appendChild(createTextTag(fout, "title", title));
//						page.appendChild(category);
//						pageset.appendChild(page);
//					}
//					// XML Output
//					LibraryIO.writeXML(outputDirPath + fin.getName(), fout);
//					System.out.println(fin.getName() + " done first para");
//				}
//			}
//		}.start();
//		
//	}
//
//	/**
//	 * 過濾掉 Redirect, List of, Disambiguation 的頁面
//	 * @throws Exception
//	 */
//	
//	public static void Step1_Filtered() throws Exception {
//		new CorpusLabProcess("Filtered", InputDirPath[1], InputDirPath[2]) {
//			
//			@Override
//			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
//				while (!sq.isEmpty()) {
//					File fin = sq.poll();
//					Document doc = LibraryIO.loadXML(fin);
//					NodeList nd = doc.getElementsByTagName("page");
//					
//					for (int i = 0; i < nd.getLength(); i++) {
//						Node page = nd.item(i);
//						String title = ((Element) page).getElementsByTagName("title").item(0).getTextContent();
//						// String text = ((Element) page).getElementsByTagName("text").item(0).getTextContent();
//						
//						if (title.toLowerCase().contains("list of")) continue;
//						
//					}
//				}
//			}
//			
//		}.start();
//	}
//}
