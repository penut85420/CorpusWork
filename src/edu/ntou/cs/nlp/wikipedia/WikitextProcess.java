package edu.ntou.cs.nlp.wikipedia;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.*;

import org.oppai.io.LibraryIO;
import org.oppai.utils.LibraryUtils;
import org.w3c.dom.*;

import edu.ntou.cs.nlp.extend.*;
import edu.ntou.cs.nlp.object.ChTool;
import edu.ntou.cs.nlp.object.CorpusLabProcess;
import edu.ntou.cs.nlp.wordSegmentation.segmentor.WordSegmentor;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

import static org.oppai.utils.LibraryUtils.log;
import static edu.ntou.cs.nlp.object.WordsList.isCall;
import static edu.ntou.cs.nlp.object.WordsList.isAlso;

public class WikitextProcess {
	final static String[] InputDirPath = {
		"D:\\Documents\\Corpus\\wiki\\Step0_zhwiki_wikitext_tw\\",
		"D:\\Documents\\Corpus\\wiki\\Step1_zhwiki_wikitext_wellformed_xml\\",
		"D:\\Documents\\Corpus\\wiki\\Step2_zhwiki_first\\",
		"D:\\Documents\\Corpus\\wiki\\Step3_zhwiki_plaintext\\",
		"D:\\Documents\\Corpus\\wiki\\Step4_zhwiki_simple\\",
		"D:\\Documents\\Corpus\\wiki\\Step5_zhwiki_seg\\",
		"D:\\Documents\\Corpus\\wiki\\Step6_zhwiki_rewrite\\",
		"D:\\Documents\\Corpus\\wiki\\Step7_zhwiki_merge\\",
		"D:\\Documents\\Corpus\\wiki\\Step8_zhwiki_dep\\",
		"D:\\Documents\\Corpus\\wiki\\Step9_zhwiki_dep2\\",
	};
	
	public static void main(String[] args) throws Exception {
		LibraryUtils.timestamp("WikitextProcess");
		// Regular Step
		// Step1_SimpleReplace();
		// Step2_FirstParagraphRetrive();
		// Step3_WikitextToPlaintext();
		// Step4_MakeSimple();
		// Step5_WordSeg();
		// Step6_Rewrite();
		Step7_Merge();
		// Step8_DependencyAnalysis();
		
		// Process above done in 472s 
		
		// Testing Field
		// Test_ShowDisambiguation();
		// Test_TitleList();
		
		log("WikitextProcess done in " + LibraryUtils.timestamp("WikitextProcess") + "s");
		LibraryUtils.bgm();
	}
	
	/**
	 * Common Begin & End
	 * 
	 * log(processName + " begin");
	 * LibraryUtils.timestamp();
	 * 
	 * System.out.printf("%s done in %.3f seconds\n", processName, LibraryUtils.timestamp());
	 */

	/**
	 * 將 "&#160" 轉換成 "&#160;"，以符合正規 XML Format
	 * @throws Exception
	 */
	
	public static void Step1_SimpleReplace() throws Exception {
		new CorpusLabProcess("Simple replace", InputDirPath[0], InputDirPath[1]) {
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
		
		// 1 thread  done in 340s
		// 8 threads done in  90s
	}
	
	/**
	 * 擷取首段章節的部分
	 * 並且移除年表、消岐義、重定向等頁面
	 */
	
	public static void Step2_FirstParagraphRetrive() throws Exception {
		
		new CorpusLabProcess("First paragraph retrive", InputDirPath[1], InputDirPath[2]) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					// XML Document IO
					Document doc =  LibraryIO.loadXML(fin);
					Document fout = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					
					// Input node
					NodeList nd = doc.getElementsByTagName("page");
					
					// Output pageset
					Element pageset = fout.createElement("pageset");
					fout.appendChild(pageset);
					
					for (int i = 0; i < nd.getLength(); i++) {
						// Output page
						Element page = fout.createElement("page");
						page.setAttribute("id", String.valueOf(i));
		
						// Deal with title
						Element e = (Element) nd.item(i);
						String title = e.getElementsByTagName("title").item(0).getTextContent();
						
						// Skip if list kind
						if (title.contains("年表")) continue;
						if (title.contains("列表")) continue;
						
						// Deal with text
						String text = e.getElementsByTagName("text").item(0).getTextContent();
						
						// Skip if disambiguation or redirect
						if (text.toLowerCase().contains("{{disambiguation}}")) continue;
						if (text.toLowerCase().contains("{{disambiguation|")) continue;
						if (text.toLowerCase().startsWith("#redirect")) continue;
						if (text.startsWith("#重定向")) continue;
						
						// Deal with category
						Element category = fout.createElement("category");
						Matcher m = Pattern.compile("(?i)\\[\\[category:(.*?)\\]\\]").matcher(text);
						while (m.find()) {
							String ss = m.group(1);
							int idx = ss.indexOf("|");
							if (idx < 0) idx = ss.length();
							category.appendChild(createTextTag(fout, "cat", new String(ss.substring(0, idx).getBytes(), "UTF-8")));
						}
						
						m = Pattern.compile("(?i)\\[\\[分類:(.*?)\\]\\]").matcher(text);
						while (m.find()) {
							String ss = m.group(1);
							int idx = ss.indexOf("|");
							if (idx < 0) idx = ss.length();
							category.appendChild(createTextTag(fout, "cat", new String(ss.substring(0, idx).getBytes(), "UTF-8")));
						}
						page.appendChild(category);
						
						// Deal with first paragraph
						text = clearTag(text, "<!--", "<", ">", 1);
						Integer endlen = text.indexOf("==");
						endlen = endlen > 0? endlen: text.length();
						text = text.substring(0, endlen);
						
						page.appendChild(createTextTag(fout, "title", title));
						page.appendChild(createTextTag(fout, "text", text));
						page.appendChild(category);
						pageset.appendChild(page);
					}
					
					// XML Output
					LibraryIO.writeXML(outputDirPath + fin.getName(), fout);
					System.out.println(fin.getName() + " done first para");
				}
			}
		}.start();
		
		// 8 threads done in 88s
	}

	/**
	 * 將 Wikitext 語法移除
	 * @throws Exception
	 */
	
	public static void Step3_WikitextToPlaintext() throws Exception {
		String processName = "Wikitext to plaintext";
		String inputDirPath = "D:\\Documents\\Corpus\\wiki\\Step2_zhwiki_first\\";
		String outputDirPath = "D:\\Documents\\Corpus\\wiki\\Step3_zhwiki_plaintext\\";
		
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		log(processName + " begin");
		LibraryUtils.timestamp();
		
		SyncQueue<File> sq = new SyncQueue<File>(Arrays.asList(inputDir.listFiles()));

		Runnable r = ()->{
			while (!sq.isEmpty()) {
				try {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("page");
					
					for (int i = 0; i < nd.getLength(); i++) {
						Element page = (Element)nd.item(i);
						String title = page.getElementsByTagName("title").item(0).getTextContent();
						Node textNode = page.getElementsByTagName("text").item(0);
						String text = textNode.getTextContent();
						text = text.replaceAll("(?i)\\{\\{" + Pattern.quote(title) + "\\}\\}", title); // 把 Fake Title 語法改成 Plain Text
						text = clearTag(text, "{{", "{", "}", 2); // 移除 {{tag}} 語法
						text = clearTag(text, "[[File", "[", "]", 2); // 移除圖片插入語法
						text = text.replaceAll("<.*?/>", ""); // 移除 Self-Closed Tag
						text = text.replaceAll("(?s)<ref.*?>.*?</ref>", ""); // 移除 <ref..>text</ref> 中間的文字
						text = text.replaceAll("(?s)<div.*?>.*?</div>", ""); // 移除 <div..>text</div> 中間的文字
						text = clearTag(text, "<", ">"); // 移除所有 <tag> 標籤
						text = clearTag(text, "-{T", "{", "}", 1); // 移除語言選擇
						text = text.replaceAll("\\-\\{.*?(zh-hant:)(.*?);(.*?)}-", "$2"); // 提取正體中文的稱呼
						text = text.replaceAll("\\-\\{.*?(zh-tw:)(.*?);(.*?)}-", "$2"); // 提取正體中文的稱呼
						text = text.replaceAll("\\-\\{(.*?)\\}\\-", "$1"); // 提取單詞
						text = clearTag(text, "(", ")"); // 移除所有半形括號與內容
						text = clearTag(text, "（", "）"); // 移除所有全形括號與內容
						text = clearTag(text, "{", "}"); // 移除所有 {tag} 標籤
						text = getLinkText(text); // 提取頁面連結文字
						text = text.replaceAll("'", ""); // 移除粗體標記
						text = text.replaceAll("\\*", ""); // 移除粗體標記
						text = text.replaceAll("[\r\n]", ""); // 移除所有換行
						
						textNode.setTextContent(text.trim());
					}
					
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					System.out.printf("%s done wikitxt to plain\n", fin.getName());
				} catch (Exception e) { e.printStackTrace(); }
			}
		};
		
		List<Thread> tlist = new ArrayList<>();
		
		for (int i = 0; i < 8; i++) {
			tlist.add(new Thread(r));
			tlist.get(i).start();
		}
		
		for (Thread t : tlist) t.join();

		System.out.printf("%s done in %.3f seconds\n", processName, LibraryUtils.timestamp());
		
		//  1 thread  done in 484s
		//  2 threads done in 265s
		//  4 threads done in 163s
		//  6 threads done in 146s
		//  8 threads done in 141s
		// 16 threads done in 144s
		// 32 threads done in 149s
	}
	
	/**
	 * 將首段章節簡化至第一個句點
	 * @throws Exception
	 */
	
	public static void Step4_MakeSimple() throws Exception {
		new CorpusLabProcess("Make simple sentence", InputDirPath[3], InputDirPath[4]) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList text = doc.getElementsByTagName("text");
					
					for (int i = 0; i < text.getLength(); i++) {
						String t = text.item(i).getTextContent();
						int idx = t.indexOf("。");
						t = t.substring(0, idx > 0? idx + 1: t.length());
						
						text.item(i).setTextContent(t);
					}
					
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					log(fin.getName() + " done make simple");
				}
			}
		}.start();
		
		// Done in 70。
	}
	
	/**
	 * 對首段文章進行斷詞
	 * @throws Exception
	 */
	
	public static void Step5_WordSeg() throws Exception {
		String alphabetListPath = "D:\\Documents\\JavaWorkspace\\WordSegmentation\\data\\list_alphabet.txt";
		String segSymbolInfoPath = "D:\\Documents\\Dictionary\\\\seg_symbol_space_only.txt";
		String dictPath = "D:\\Documents\\JavaWorkspace\\WordSegmentation\\data\\dictionary_main.txt";
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, dictPath);
		ws.MaximumMatch("", 1);
		
		new CorpusLabProcess("Word segmention", InputDirPath[4], InputDirPath[5], 4) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {		
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList context = doc.getElementsByTagName("text");
					
					for (int i = 0; i < context.getLength(); i++) {
						// 先將 title 移除
						String seged = ws.MaximumMatch(context.item(i).getTextContent().replaceAll("={2,}+[^=]+?=+", ""), 1).replaceAll("[\r\n]", "");
						context.item(i).setTextContent(new String(seged.getBytes("UTF-8"), "UTF-8"));
					}
					System.out.println(fin.getName() + " done segmention");
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
				}
			}
		}.start();
		
		// 1 thread  done in 254s
		// 4 threads done in 140s
		// 8 threads done in 126s
	}
	
	/**
	 * 將首句改寫成較為簡短、簡單的構句
	 * @throws Exception 
	 */

	public static void Step6_Rewrite() throws Exception {
		new CorpusLabProcess("Rewrite", InputDirPath[5], InputDirPath[6]) {
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("text");
					
					for (int i = 0; i < nd.getLength(); i++) {
						String text = nd.item(i).getTextContent();
						
						text = text.replaceAll("、", "和");
						
						String[] seg = text.split(" ");
						
						// 修掉 Fake Title 造成的前兩個詞重複
						if (seg.length > 1 && seg[0].equals(seg[1])) seg[1] = "";
						
						String newContent = "";
						boolean isIs = false, isOf = false;
						
						for (int j = 0; j < seg.length; j++) {
							// 暴力修掉「別稱類」修飾語
							try {
								if (isCall(seg[j]) || (isAlso(seg[j-1]) && seg[j].equals("稱"))) {
									if (seg[j-1].equals("，") || isAlso(seg[j-1]))
										seg[j-1] = "";
									seg[j] = "";
									seg[j-1] = "";
									if (seg[j+2].equals("和")) {
										seg[j+2] = "";
										seg[j+3] = "";
										if (seg[j+4].equals("，"))
											seg[j+4] = "";
									} else if (seg[j+2].equals("，"))
										seg[j+2] = "";
								}
							} catch (Exception e) { }
							
							try {
								// 把指改成是
								if (seg[j].equals("泛指")) 
									seg[j] = "是";
								else if (seg[j].equals("指")) {
									seg[j] = "是";
									if (seg[j+1].equals("的是"))
										seg[j+1] = "";
								} else if (seg[j].equals("是") && seg[j+1].equals("指"))
									seg[j+1] = "";
								else if (seg[j-1].equals("可以") && seg[j].equals("是")) {
									seg[j-1] = "";
								}
							} catch (Exception e) { }
							
							if (seg[j].equals("是")) isIs = true;
							else if (seg[j].equals("的")) isOf = true;
							else if (seg[j].equals("，")) {
								if ((j+1 < seg.length && seg[j+1].equals("以及")) || !isIs || !isOf || j > 50)
									seg[j] = "";
								else {
									seg[j] = "。";
									newContent += seg[j];
									break;
								}
							}
							newContent += seg[j] + " ";
						}
						
						nd.item(i).setTextContent(newContent.replaceAll("  ", " "));
						// break;
					}
					
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					log(fin.getName() + " done rewrite");
					// break;
				}
			}
		}.start();
		
		// 1t 324s
		// 8t 101s
	}
	
	public static void Step7_Merge() throws Exception {
		int fileBaseSize = 3 * 1024;
		File dirIn = new File(InputDirPath[6]);
		File dirOut = new File(InputDirPath[7]);
		
		if (!dirOut.exists()) dirOut.mkdirs();
		
		int fid = 1;
		String fileNameFormat = InputDirPath[7] + "zhwikiMain_%04d.xml";
		String fileName = String.format(fileNameFormat, fid);
		
		String fhead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<pageset>\n";
		// System.out.printf(fileName, fid);
		
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		File fout = new File(fileName);
		writer.write(fhead);
		
		for (File fin : dirIn.listFiles()) {
			for (String line : LibraryIO.loadFileAsLines(fin.getPath())) {
				if (line.equals("<pageset>") || line.startsWith("<?xml vers") || line.equals("</pageset>")) continue;
				writer.write(line + "\n");
				writer.flush();
				
				if (line.equals("</page>") && fout.length() > fileBaseSize) {
					writer.write("</pageset>");
					writer.close();

					fileName = String.format(fileNameFormat, ++fid);
					writer = new PrintWriter(fileName, "UTF-8");
					fout = new File(fileName);
					writer.write(fhead);
				}
			}
			log(fin.getName() + " done merge");
		}
		
		writer.write("</pageset>");
		writer.close();
	}
	
	public static void Step8_DependencyAnalysis() throws Exception {
		String parserModel = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz";
		
		new CorpusLabProcess("Dependency analysis", InputDirPath[6], InputDirPath[8], 1) {
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
					TreebankLanguagePack tlp = lp.treebankLanguagePack();
					GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
					
					File fin = sq.poll();
					LibraryUtils.timestamp(fin.toString());
					Document doc = LibraryIO.loadXML(fin);
					NodeList pages = doc.getElementsByTagName("page");
					int pageslen = pages.getLength();
					
					for (int i = 0; i < pageslen; i++) {
						Element page = (Element) pages.item(i);
						Element depTree = doc.createElement("deptree");
						Element tdlxml = doc.createElement("tdl");
						
						String text = page.getElementsByTagName("text").item(0).getTextContent();
						String[] splitText = text.split(" ");
						
						System.out.printf("[%d] %s\n", splitText.length, text);
						if (splitText.length > 100) {
							System.out.println("Skip due to too long sentence");
							page.appendChild(depTree);
							continue;
						}
						
						try {
							List<CoreLabel> rawWords = SentenceUtils.toCoreLabelList(text.split(" "));
							
							// Parsing tree and output
							Tree parse = lp.apply(rawWords);
							depTree.setTextContent(parse.toString());
							page.appendChild(depTree);
							
							// Type Dependency
							GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
							List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
							Iterator<TypedDependency> it = tdl.iterator();
							// tdlxml.setTextContent(tdl.toString());
							
							while (it.hasNext()) {
								TypedDependency td = it.next();
								log(String.format("reln: %s, %s %s %d, %s %s %d", td.reln(),
										td.dep().word(), td.dep().tag(), td.dep().index(),
										td.gov().word(), td.gov().tag(), td.gov().index()));
								tdlxml.appendChild(createTextTag(doc, "td", td.toString()));
							}
							page.appendChild(tdlxml);
//							TypedDependency first = it.next();
//							String title = first.dep().word();
//							IndexedWord tobe = first.gov();
							
//							String classifyContent = String.format("%s(%s)\n", title, tobe.word(), first.reln());
//							
//							while (it.hasNext()) {
//								TypedDependency td = it.next();
//								// if (tobe.equals(td.gov())) log(td);
//								if (tobe.equals(td.gov())) {
//									if (td.reln().getShortName().equals("compound:nn"))
//										classifyContent += String.format("%s%s\n", td.dep().word(), tobe.word());
//									else if (td.reln().getShortName().equals("nmod:assmod"))
//										classifyContent += String.format("%s%s\n", td.dep().word(), tobe.word());
//								}
//							}
							
							
							System.out.printf("%s %d/%d done\n", fin.getName(), i + 1, pageslen);
						} catch (Exception e) { e.printStackTrace(); }
						break; // When test on 1 page
					}
					
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					lp = null; tlp = null; gsf = null; System.gc();
					
					log(fin.getName() + " done in " + LibraryUtils.timestamp(fin.toString()));
				}
			}
		}.start();
	}
	
	// ===== Testing =====
	
	/**
	 * 此段程式碼為偵測消岐義的頁面
	 * 透過偵測該頁面內容是否包含 "{{disambiguation}}" 和 "{{disambiguation|" 為基準
	 * 若單純偵測 "disambiguation"
	 * 可能會誤刪內文包含引用消岐義語法的一般頁面
	 * 
	 * 2018三月的版本初估有480個消岐義頁面
	 */
	
	public static void Test_ShowDisambiguation() throws Exception {
		String processName = "Show disambiguation";
		String inputDirPath = "D:\\Documents\\Corpus\\wiki\\Step1_zhwiki_wikitext_wellformed_xml\\";
		inputDirPath = "D:\\Documents\\Corpus\\wiki\\Step1_zhwiki_wikitext_wellformed_xml\\";
		String outputFileName = "D:\\Documents\\Corpus\\wiki\\Test\\disambiguation.xml";
		outputFileName = "D:\\Documents\\Corpus\\wiki\\Test\\disambiguation2.xml";
		
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputFileName);
		
		if (!outputDir.getParentFile().exists())
			outputDir.getParentFile().mkdirs();
		
		Document fout = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element fout_pageset = fout.createElement("pageset"); 
		
		System.out.println(processName + " begin");
		LibraryUtils.timestamp();
		
		int total = 0;
		
		for (File fin : inputDir.listFiles()) {
			Document doc = LibraryIO.loadXML(fin);
			NodeList nd = doc.getElementsByTagName("page");
			
			for (int i = 0; i < nd.getLength(); i++) {
				Element page = (Element)nd.item(i);
				String text = page.getElementsByTagName("text").item(0).getTextContent();
				
				if (text.toLowerCase().contains("{{disambiguation}}") || text.toLowerCase().contains("{{disambiguation|")) {
					total++;
					String title = page.getElementsByTagName("title").item(0).getTextContent();
					System.out.println(title + " is disambiguation");
					Element fout_page = fout.createElement("page");
					Element fout_location = fout.createElement("location");
					Element fout_title = fout.createElement("title");
					Element fout_text = fout.createElement("text");
					
					fout_location.setTextContent(fin.getName());
					fout_title.setTextContent(title);
					fout_text.setTextContent(text);
					
					fout_page.appendChild(fout_location);
					fout_page.appendChild(fout_title);
					fout_page.appendChild(fout_text);
					fout_pageset.appendChild(fout_page);
				}
			}
			System.out.println(fin.getName() + " done");
		}
		
		Element fout_total = fout.createElement("total");
		fout_total.setTextContent(String.valueOf(total));
		fout_pageset.appendChild(fout_total);
		fout.appendChild(fout_pageset);
		LibraryIO.writeXML(outputFileName, fout);
		
		System.out.printf("%s done in %.3f seconds\n", processName, LibraryUtils.timestamp());
		
		// Done in 169s
	}
	
	/**
	 * 把所有標題取出來變成檔案
	 * @throws Exception
	 */
	
	public static void Test_TitleList() throws Exception {
		new CorpusLabProcess("Title list", InputDirPath[4], "D:\\Documents\\Corpus\\Wiki\\TitleList\\") {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("page");
					
					Document dout = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					Element dout_titleset = dout.createElement("titleset");
					
					for (int i = 0; i < nd.getLength(); i++) {
						Element page = (Element) nd.item(i);
						String title = page.getElementsByTagName("title").item(0).getTextContent();
						String text  = page.getElementsByTagName("text") .item(0).getTextContent();
						NodeList catlist = page.getElementsByTagName("cat");
						
						Element dout_title = dout.createElement("title");
						dout_title.appendChild(createTextTag(dout, "titletw", title));
						dout_title.appendChild(createTextTag(dout, "titlecn", ChTool.toSimplified(title)));
						dout_title.appendChild(createTextTag(dout, "first", text));
						Element dout_category = dout.createElement("category");
						for (int j = 0; j < catlist.getLength(); j++)
							dout_category.appendChild(createTextTag(dout, "cat", catlist.item(j).getTextContent()));
						dout_title.appendChild(dout_category);
						dout_titleset.appendChild(dout_title);
					}
					dout.appendChild(dout_titleset);
					log(fin.getName() + " done title list");
					LibraryIO.writeXML(outputDirPath + fin.getName(), dout);
				}
			}
		}.start();;
	}
	
	public static void Test_TitleList2() throws Exception {
		File dirInn = new File("D:\\Documents\\Corpus\\wiki\\title\\");
		Writer w = new PrintWriter("D:\\Documents\\Corpus\\wiki\\title.txt");
		
		for (File fin : dirInn.listFiles()) {
			String[] lines = LibraryIO.loadFileAsLines(fin.getPath());
			for (String line : lines)
				w.write(line + "\t" + ChTool.toSimplified(line) + "\n");
			log(fin.getName() + " done");
		}
		w.close();
	}
	
	public static void Test_CategoryCount() throws Exception {
		
	}
	
	// ===== Tool Functions =====
	
	/**
	 * 消除 Wikitext 中，所有以左右對稱括號之語法。
	 * 例："ab{{123{456}789}}cd" 會被轉換成 "abcd"
	 * @param content 需要處理的字串
	 * @param tagTrg 該語法之開頭，以上例為 "{{"
	 * @param tagLeft 該語法對稱括號之左括號，以上例為 "{"
	 * @param tagRight 該語法對稱括號之又括號，以上例為 "}"
	 * @param initLeft 該語法起始左括號量，以上例為 2
	 * @return 處理完成後的字串
	 */
	
	public static String clearTag(String content, String tagTrg, String tagLeft, String tagRight, int initLeft) {
		while (content.indexOf(tagTrg) != -1) {
			int i, left = initLeft, 
				begin = content.indexOf(tagTrg);
			
			String tBefore = content.substring(0, begin), 
					tLeft = content.substring(begin + tagTrg.length());
			char[] c = tLeft.toCharArray();
			
			for (i = 0; i < c.length; i++) {
				if (tagLeft.equals(String.valueOf(c[i])))
					left++;
				else if (tagRight.equals(String.valueOf(c[i])))
					left--;
				if (left == 0) break;
			}
			if (i == c.length) { 
				// System.err.println("Not matched");
				return content;
			}
			tLeft = tLeft.substring(i + tagRight.length());
			content = tBefore +  tLeft;
		}
		
		return content;
	}
	
	public static String clearTag(String content, String tagLeft, String tagRight) {
		return clearTag(content, tagLeft, tagLeft, tagRight, 1);
	}
	
	/**
	 * 提取 Wiki 內參考連結的字串
	 * @param content that include sth like [[link]]
	 * @return link
	 */
	
	public static String getLinkText(String content) {
		while (content.indexOf("[[") != -1) {
			try {
				int begin = content.indexOf("[[");
				int end = content.indexOf("]]");
				String t = content.substring(begin + 2, end);
				while (t.indexOf("|") != -1)
					t = t.substring(t.indexOf("|") + 1);
				
				content = content.substring(0, begin) + t + content.substring(end + 2);
			} catch (Exception e) { return content; }
		}
		
		return content;
	}
	
	/**
	 * 創建一個包含一段指定的文字的 XML Text Node
	 * @param d
	 * @param tag
	 * @param text
	 * @return
	 */
	
	public static Element createTextTag(Document d, String tag, String text) {
		Element e = d.createElement(tag);
		e.appendChild(d.createTextNode(text));
		return e;
	}

}
