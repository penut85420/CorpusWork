package nlp.cse.ntou.edu.wikipedia;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oppai.LibraryIO;
import org.oppai.LibraryUtils;
import org.w3c.dom.*;

import nlp.cse.ntou.edu.extend.SyncQueue;
import nlp.cse.ntou.edu.object.CorpusLabAnalysis;
import nlp.cse.ntou.edu.object.CorpusLabProcess;

import static org.oppai.LibraryUtils.log;

public class WikiAnalysis {
	/**
	 * To-Do Work: 
	 + 中文所有頁面數量	1747961
	 + 中文重定向頁面數量	771390
	 + 中文消岐義頁面數量	28579
	 + 中文列表、年表頁面數量	20960
	 + 中文剩餘所有頁面為 927031
	 + 計算平均 Category 數量	2526411/932867
	 + 計算多少頁面可以/不可以抽出第一行 5058
	 + 英文所有頁面數量 18,803,177
	 + 英文純頁面數量  5,510,893
	 + 英文與中文可以配對的數量 459559 not mapping
	 	+ 可能是音譯不同
	 	+ 可能不是page to page而是page to section的關係
	 + Final En & Zh 可 Map 頁面數量 444098
	 */
	
	static Map<String, String> EnZhMap;
	static Map<String, String> ZhEnMap;
	static Map<String, String> ZhTitleNodeMap;
	static Map<String, String> EnTitleGroupMap;
	static Map<String, String> GroupTagMap;
	static Map<String, String> InfoboxMap;
	static Map<String, String> ZhTitleInfoboxMap;
	static List<String> ZhTitleInfoboxList;
	static Document FinalOut;
	static Node FinalPageset;
	
	public static void main(String[] args) throws Exception {
		// countZhwikiPagesAll();
		// countAverageCategory();
		// countNoFirst();
		// countEnPages();
		// countEnZhLink();
		// linkZhEn();
		// getEnGroupMap();
		// tagGroup();
		// getInfoboxMap();
		// findInfoboxInWiki();
		// categoryNeedInfobox();
		/* zhtitleAndInfobox(); */
		// findInfoboxInWiki();
		// mergeOrSplit();
		countNoHead();
		log("done");
		LibraryUtils.bgm();
	}
	
	// Process
	
	public static void countZhwikiPagesAll() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\Step1_zhwiki_wikitext_wellformed_xml\\";
		
		final String KEY_TOTAL_PAGES = "total_page",
				KEY_REDIRECT_PAGES = "redirect",
				KEY_DISAMBIG_PAGES = "disambig",
				KEY_LISTOF_PAGES = "list_of";
		
		CorpusLabAnalysis cla = new CorpusLabAnalysis("Count pages", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				int totalPages = 0, redirectPages = 0, disambig = 0, listof = 0;
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					
					NodeList nd = doc.getElementsByTagName("page");
					totalPages += nd.getLength();
					
					for (int i = 0; i < nd.getLength(); i++) {
						Element page = (Element) nd.item(i);
						String text = page.getElementsByTagName("text").item(0).getTextContent().toLowerCase();
						String title = page.getElementsByTagName("title").item(0).getTextContent();
						
						if (text.contains("#redirect") || text.contains("#重定向"))
							redirectPages++;
						
						if (text.contains("{{disambig"))
							disambig++;
						
						if (title.contains("列表") || title.contains("年表"))
							listof++;
						
					}
					System.out.println(fin.getName());
				}
				setValue(KEY_TOTAL_PAGES, totalPages);
				setValue(KEY_REDIRECT_PAGES, redirectPages);
				setValue(KEY_DISAMBIG_PAGES, disambig);
				setValue(KEY_LISTOF_PAGES, listof);
			}
		};
		cla.start();
		System.out.printf("Pages: %d, Redirect: %d, Disambig: %d, List of: %d\n", 
				cla.getValue(KEY_TOTAL_PAGES), 
				cla.getValue(KEY_REDIRECT_PAGES),
				cla.getValue(KEY_DISAMBIG_PAGES),
				cla.getValue(KEY_LISTOF_PAGES));
	}

	public static void countAverageCategory() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\Step2_zhwiki_first\\";
		
		final String KEY_PAGES = "pages",
				KEY_CATS = "cats";
		
		CorpusLabAnalysis cla = new CorpusLabAnalysis("avg category count", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				int totalPages = 0, totalCats = 0;
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList pages = doc.getElementsByTagName("page");
					NodeList cats  = doc.getElementsByTagName("cat"); 
					
					totalPages += pages.getLength();
					totalCats += cats.getLength();
					System.out.println(fin.getName());
				}
				setValue(KEY_PAGES, totalPages);
				setValue(KEY_CATS, totalCats);
			}
		};
		cla.start();
		System.out.printf("Total Pages: %d, Cats: %d\n",
				cla.getValue(KEY_PAGES), cla.getValue(KEY_CATS));
	}

	public static void countNoFirst() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\Step3_zhwiki_plaintext\\";
		final String KEY_EMPTY = "empty";
		
		CorpusLabAnalysis cla = new CorpusLabAnalysis("No first", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				int empty = 0;
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("text");
					for (int i = 0; i < nd.getLength(); i++)
						if (nd.item(i).getTextContent().isEmpty())
							empty++;
					System.out.println(fin.getName());
				}
				setValue(KEY_EMPTY, empty);
			}
		};
		
		cla.start();
		System.out.printf("Empty: %d\n", cla.getValue(KEY_EMPTY));
	}

	public static void countEnPages() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\enwiki\\Step0_enwiki_split\\";
		final String KEY_EN_PAGES = "en_pages";
		
		CorpusLabAnalysis cla = new CorpusLabAnalysis("Count en pages", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("page");
					
					setValue(KEY_EN_PAGES, nd.getLength());
					System.out.println(fin.getName());
				}
			}
		};
		
		cla.start();
		System.out.printf("Pages: %d\n", cla.getValue(KEY_EN_PAGES));
	}

	public static void countEnZhLink() throws Exception {
		getEnZhMap();
		
		final String KEY_NULL = "null";
		Writer w = new FileWriter("D:\\Documents\\Corpus\\wiki\\zh_interlanglink\\title not map.txt");
		
		CorpusLabAnalysis cla = new CorpusLabAnalysis("en zh link count", "D:\\Documents\\Corpus\\wiki\\Step4_zhwiki_simple") {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("title");
					
					for (int i = 0; i < nd.getLength(); i++) {
						String title = nd.item(i).getTextContent();
						if (ZhEnMap.get(title.replaceAll(" ", "_")) == null) {
							w.write(title + "\n");
							setValue(KEY_NULL, 1);
						}
					}
					System.out.println(fin.getName());
				}
			}
		};
		cla.start();
		System.out.printf("Null: %d\n", cla.getValue(KEY_NULL));
		w.close();
	}
	
	public static void linkZhEn() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_title_category\\";
		getEnZhMap();
		
		CorpusLabAnalysis cla = new CorpusLabAnalysis("en zh link", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("page");
					for (int i = 0; i < nd.getLength(); i++) {
						Element page = (Element) nd.item(i);
						String title = page.getElementsByTagName("zhtitle").item(0).getTextContent().replaceAll(" ", "_");
						if (ZhEnMap.get(title) == null) continue;
						putNode(title, fin.getName() + "\t" + i);
					}
				}
			}
		};
		cla.start();
		FinalOut = LibraryIO.getNewXML();
		FinalPageset = FinalOut.createElement("pageset");
		String dirInnPath2 = "D:\\Documents\\Corpus\\wiki\\enwiki\\Step2_enwiki_category_merge\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zh_en_wiki_title_category\\";
		new CorpusLabProcess("find en in zh", dirInnPath2, "D:\\Documents\\Corpus\\wiki\\zh_en_wiki_title_category\\") {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("page");
					
					for (int i = 0; i < nd.getLength(); i++) {
						Element page = (Element) nd.item(i);
						String entitle = page.getElementsByTagName("title").item(0).getTextContent();
						String zhtitle = EnZhMap.get(entitle.replaceAll(" ", "_"));
						if (zhtitle != null && ZhTitleNodeMap.get(zhtitle) != null) {
							// log(zhtitle);
							Node final_page = FinalOut.createElement("page");
							String[] tt = ZhTitleNodeMap.get(zhtitle).split("\t");
							File ff = new File(dirInnPath + tt[0]);
							Document dd = LibraryIO.loadXML(ff);
							NodeList nn = dd.getElementsByTagName("page");
							Element pp = (Element) nn.item(Integer.parseInt(tt[1]));
							
							NodeList zhcategory = pp.getElementsByTagName("zhcat");
							NodeList encategory = page.getElementsByTagName("cat");
							
							Node final_zhtitle = FinalOut.createElement("zhtitle");
							Node final_entitle = FinalOut.createElement("entitle");
							Node final_zhcategory = FinalOut.createElement("zhcategory");
							Node final_encategory = FinalOut.createElement("encategory");
							
							final_zhtitle.setTextContent(zhtitle);
							final_entitle.setTextContent(entitle);
							
							for (int j = 0; j < zhcategory.getLength(); j++) {
								Node zhcat = FinalOut.createElement("zhcat");
								zhcat.setTextContent(zhcategory.item(j).getTextContent());
								final_zhcategory.appendChild(zhcat);
							}
							
							for (int j = 0; j < encategory.getLength(); j++) {
								Node encat = FinalOut.createElement("encat");
								encat.setTextContent(encategory.item(j).getTextContent());
								final_encategory.appendChild(encat);
							}
							
							final_page.appendChild(final_zhtitle);
							final_page.appendChild(final_entitle);
							final_page.appendChild(final_zhcategory);
							final_page.appendChild(final_encategory);
							appendPage(final_page);
						}
					}
				}
			}
		}.start();
		FinalOut.appendChild(FinalPageset);
		LibraryIO.writeXML(dirOutPath + "final.xml", FinalOut);
	}
	
	public static void tagGroup() throws Exception {
		getEnGroupMap();
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\enwiki\\Step3_zhenwiki_title_category\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\enwiki\\Step4_zhenwiki_group\\";
		
		new CorpusLabProcess("Tag group", dirInnPath, dirOutPath) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();

					Document doc = LibraryIO.loadXML(fin);
					NodeList pageset = doc.getElementsByTagName("page");
					
					for (int i = 0; i < pageset.getLength(); i++) {
						Element page = (Element) pageset.item(i);
						String entitle = page.getElementsByTagName("entitle").item(0).getTextContent();
						
						String g = EnTitleGroupMap.get(entitle.toLowerCase());
						// if (i % 100 == 0) log(entitle);
						if (g != null) {
							Node group = doc.createElement("group");
							Node tag = doc.createElement("tag");
							group.setTextContent(g);
							tag.setTextContent(GroupTagMap.get(g));
							page.appendChild(group);
							page.appendChild(tag);
						}
						
					}
					log(fin.getName());
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
				}
			}
		}.start();
	}
	
	public static void categoryNeedInfobox() throws Exception {
		getInfoboxMap();
		findInfoboxInWiki();
		
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\enwiki\\Step4_zhenwiki_group\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\enwiki\\Step5_zhenwiki_infobox\\";
		
		new CorpusLabProcess("Category need infobox", dirInnPath, dirOutPath) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList pagelist = doc.getElementsByTagName("page");
					
					for (int i = 0; i < pagelist.getLength(); i++) {
						Element page = (Element) pagelist.item(i);
						String zhtitle = page.getElementsByTagName("zhtitle").item(0).getTextContent();
						String zhtitle_infobox = ZhTitleInfoboxMap.get(zhtitle);
						if (zhtitle_infobox != null) {
							Node tag_ibx = doc.createElement("infoboxPath");
							Node ibx_item = doc.createElement("infobox");
							ibx_item.setTextContent(zhtitle_infobox);
							tag_ibx.appendChild(ibx_item);
							String ibx_cat = InfoboxMap.get("template:" + zhtitle_infobox.toLowerCase());
							while (ibx_cat != null) {
								// log(ibx_cat);
								ibx_item = doc.createElement("infobox");
								ibx_item.setTextContent(ibx_cat);
								tag_ibx.appendChild(ibx_item);
								ibx_cat = InfoboxMap.get(ibx_cat);
							}
							page.appendChild(tag_ibx);
						}
					}
					LibraryIO.writeXML(outputDirPath + fin.getName(), doc);
					log(fin.getName());
				}
			}
		}.start();
	}
	
	public static void mergeOrSplit() throws Exception {
		int fileBaseSize = 4 * 1024 * 1024;
		
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_infoboxPath\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_infoboxPath_merged\\";

		File dirIn = new File(dirInnPath);
		File dirOut = new File(dirOutPath);
		
		if (!dirOut.exists()) dirOut.mkdirs();
		
		int fid = 1;
		String fileNameFormat = dirOutPath + "zhwikiMain_%04d.xml";
		String fileName = String.format(fileNameFormat, fid);
		
		String fhead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<pageset>\n";
		System.out.printf(fileName, fid);		
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
	
	public static void countNoHead() throws Exception {
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\Step10_zhwiki_typedep_merge\\";
		Writer w = new FileWriter("log.log");
		CorpusLabAnalysis cla = new CorpusLabAnalysis("Count no head", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					NodeList nd = doc.getElementsByTagName("page");
					
					for (int i = 0; i < nd.getLength(); i++) {
						Element page = (Element) nd.item(i);
						if (page.getElementsByTagName("cfy").getLength() <= 0) {
							w.write(fin.getName() + page.getElementsByTagName("title").item(0).getTextContent() + '\n');
							setValue("len", 1);
						}
					}
					log(fin.getName());
				}
			}
		};
		cla.start();
		log("No head: " + cla.getValue("len"));
		w.close();
	}
	
	/** 
	 * Deprecated
//	public static void zhtitleAndInfobox() throws Exception {
//		findInfoboxInWiki();
//		getInfoboxMap();
//		Document dout = LibraryIO.getNewXML();
//		Node pageset = dout.createElement("pageset");
//
//		for (String line : ZhTitleInfoboxList) {
//			
//			Node page = dout.createElement("page");
//			Node title = dout.createElement("title");
//			title.setTextContent(key);
//			Node infoboxPath = dout.createElement("infoboxPath");
//			Node infobox = dout.createElement("infobox");
//			String cat = ZhTitleInfoboxMap.get(key);
//			infobox.setTextContent(cat);
//			infoboxPath.appendChild(infobox);
//			String ibx_cat = InfoboxMap.get("template:" + cat.toLowerCase());
//			while (ibx_cat != null) {
//				// log(ibx_cat);
//				infobox = dout.createElement("infobox");
//				infobox.setTextContent(ibx_cat);
//				infoboxPath.appendChild(infobox);
//				ibx_cat = InfoboxMap.get(ibx_cat);
//			}
//			page.appendChild(title);
//			page.appendChild(infoboxPath);
//			pageset.appendChild(page);
//		}
//		dout.appendChild(pageset);
//		LibraryIO.writeXML("D:\\Documents\\Corpus\\wiki\\zhwiki_infobox.xml", dout);
//	}
	*/
	
	// Date Making
	
	public static void getEnZhMap() throws Exception {
		EnZhMap = new HashMap<>();
		ZhEnMap = new HashMap<>();
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\zh_interlanglink\\split_retrive_CT\\";
		
		new CorpusLabAnalysis("en zh title map", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();

					BufferedReader br = new BufferedReader(new FileReader(fin));
					
					while (br.ready()) {
						String[] line = br.readLine().trim().split("\t");
						putMap(line[0], line[1]);
					}
					
					br.close();
				}
			}
		}.start();
	}
	
	public static void getEnGroupMap() throws Exception {
		GroupTagMap = new HashMap<>();
		EnTitleGroupMap = new HashMap<>();
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\enwiki\\guessNE_g00-12\\";
		
		new CorpusLabAnalysis("Group map", dirInnPath) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					
					BufferedReader br = new BufferedReader(new FileReader(fin));
					String line = br.readLine();
					while (line != null) {
						putGroup(line.trim().toLowerCase(), fin.getName().substring(0, 3));
						line = br.readLine();
					}
					br.close();
				}
			}
		}.start();
		// log(EnTitleGroupMap.get("ASCII"));
		
		String s = "G00	Y\r\n" + 
				"G01	Y\r\n" + 
				"G11	N\r\n" + 
				"G02	N\r\n" + 
				"G03	Y\r\n" + 
				"G04	N\r\n" + 
				"G05	N\r\n" + 
				"G06	Y\r\n" + 
				"G07	N\r\n" + 
				"G08	Y\r\n" + 
				"G12	N";
		for (String ss : s.split("\r\n")) {
			String[] sss = ss.split("\t");
			GroupTagMap.put(sss[0], sss[1]);
		}
		log(GroupTagMap.get("G00"));
	}
	
	public static void getInfoboxMap() throws Exception {
		InfoboxMap = new HashMap<>();
		String fileInnPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_crawler\\template_pair.txt";
		int i = 0;
		String[] contents = LibraryIO.loadFileAsLines(fileInnPath);
		for (String line : contents) {
			String[] seg = line.trim().split("\t");
			if (seg[0].contains("呼叫") && InfoboxMap.get(seg[1]) != null) continue;
			InfoboxMap.put(seg[1].toLowerCase(), seg[0].toLowerCase());
			log(i++);
		}
		String key = "Template:Infobox Bangladesh district".toLowerCase();
		String rtn = InfoboxMap.get(key);
		while (rtn != null) {
			log(rtn);
			key = rtn;
			rtn = InfoboxMap.get(key);
		}
	}
	
	public static void findInfoboxInWiki() throws Exception {
		getInfoboxMap();
		ZhTitleInfoboxList = new ArrayList<>();
		String dirInnPath = "D:\\Documents\\Corpus\\wiki\\Step1_zhwiki_wikitext_wellformed_xml\\";
		String dirOutPath = "D:\\Documents\\Corpus\\wiki\\zhwiki_infoboxPath\\";
		Pattern p = Pattern.compile("(?i)\\{\\{(.*)");
		new CorpusLabProcess("Zh title map to infobox", dirInnPath, dirOutPath) {
			
			@Override
			public void run(SyncQueue<File> sq, String dirOut) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					Document doc = LibraryIO.loadXML(fin);
					Document dout = LibraryIO.getNewXML();
					Node pageset = dout.createElement("pageset");
					dout.appendChild(pageset);
					NodeList pages = doc.getElementsByTagName("page");
					
					for (int i = 0; i < pages.getLength(); i++) {
						Element page = (Element) pages.item(i);
						String title = page.getElementsByTagName("title").item(0).getTextContent();
						String text = page.getElementsByTagName("text").item(0).getTextContent();
						Matcher m = p.matcher(text);
						Node dpage = dout.createElement("page");
						Node dtitle = dout.createElement("title");
						dtitle.setTextContent(title);
						dpage.appendChild(dtitle);
						Node infoboxPath = dout.createElement("infoboxPath");
						dpage.appendChild(infoboxPath);
						
						Boolean f = false;
						Map<String, Boolean> re = new HashMap<>();
						while (m.find()) {
							
							String ibx = m.group(1).split("\\|")[0].trim().toLowerCase().replaceAll("  ", " ").replaceAll("_", " ");
							String ibx_cat = InfoboxMap.get("template:" + ibx.toLowerCase());
							if (ibx_cat == null) continue;
							f = true;
							Node infobox = dout.createElement("infoboxOrg");
							if (re.get(ibx) != null) continue;
							infobox.setTextContent(ibx);
							infoboxPath.appendChild(infobox);
							re.put(ibx, true);
							while (ibx_cat != null) {
								infobox = dout.createElement("infobox");
								infobox.setTextContent(ibx_cat);
								infoboxPath.appendChild(infobox);
								ibx_cat = InfoboxMap.get(ibx_cat);
							}							
						}
						if (f) pageset.appendChild(dpage);
					}
					LibraryIO.writeXML(dirOutPath + fin.getName(), dout);
					log(fin.getName());
				}
			}
		}.start();
		// log(ZhTitleInfoboxMap.get("柏拉圖"));
	}
	
	// Data Operation
	
	public static synchronized void putMap(String zh, String en) {
		EnZhMap.put(en, zh);
		ZhEnMap.put(zh, en);
	}
	
	public static synchronized void putNode(String title, String pos) {
		ZhTitleNodeMap.put(title, pos);
	}
	
	public static synchronized void appendPage( Node p) {
		FinalPageset.appendChild(p);
	}
	
	public static synchronized void putGroup(String title, String group) {
		EnTitleGroupMap.put(title, group);
	}
	
	public static synchronized void putInfobox(String title, String infobox) {
		ZhTitleInfoboxList.add(title + "\t" + infobox);
	}
}
