package nlp.cse.ntou.edu.ptt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oppai.*;

import nlp.cse.ntou.edu.extend.SyncQueue;
import nlp.cse.ntou.edu.object.CorpusLabAnalysis;
import nlp.cse.ntou.edu.object.CorpusLabProcess;
import nlp.cse.ntou.edu.wordSegmentation.segmentor.WordSegmentor;

import static org.oppai.LibraryUtils.log;

@SuppressWarnings("unused")
public class PttStockProcess {
	
	static String alphabetListPath = "D:\\Documents\\Dictionary\\list_alphabet.txt";
	static String segSymbolInfoPath = "D:\\Documents\\Dictionary\\seg_symbol_space_only.txt";
	static String dictPath = "D:\\Documents\\Dictionary\\dictionary_main.txt";
	static String tunedLexion = "D:\\Documents\\FatCatFatMeow\\Data\\lexion.txt";
	
	static Map<String, String> listStockName2ID;
	static Map<String, String> listStockID2Name;
	static Map<String, String> listArticleDate;
	
	public static void main(String[] args) throws Exception {
		// reopen();
		// _00_WordSeg();
		// _01_TunedLexion();
		// _02_ReWordSeg();
		_03_DetermineSubject();
		// _04_CommentWordSeg();
	} 
	
	/**
	 * 將文章或留言使用 Main Lexion 進行斷詞
	 */
	
	public static void _00_WordSeg() throws Exception {
		String dirInnPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Stock\\";
		String dirOutPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Stock_Seg\\";
		
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, dictPath);
		ws.MaximumMatch("", 1);
		
		new CorpusLabProcess("Word segmention", dirInnPath, dirOutPath, 4) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {		
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String content = LibraryIO.loadFile(fin);
					String seged = ws.MaximumMatch(content.replace("\r", ""), 1).replaceAll("\n", "\r\n");
					content = new String(seged.getBytes("UTF-8"), "UTF-8");
					System.out.println(fin.getName() + " done segmention");
					LibraryIO.writeFile(new File(dirOutPath + fin.getName()), content);
				}
			}
		}.start();
	}
	
	/**
	 * Tuning Lexion 並增加上市上櫃公司名稱進去
	 */
	
	public static void _01_TunedLexion() throws Exception {
		String dirInnPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Stock_Seg\\";
		String fileOutPath = "D:\\Documents\\FatCatFatMeow\\Data\\Lexion_Tuning.txt";
		Map<String, Integer> lexion = new HashMap<>();
		// \xA0 is a fucking non-breaking space.
		Pattern p = Pattern.compile("^[\\p{Alnum}\\p{Punct}\\s§-ㄨ︰-／０-９：-？Ａ-Ｚａ-ｚ［-￣\\xA0].*$");
		
		new CorpusLabAnalysis("Tuned lexion", dirInnPath, 1) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String[] segs = LibraryIO.loadFile(fin).split("[\\s\n\r]");
					for (String seg : segs) {
						lexion.put(seg, 1);
					}
				}
			}
		}.start();
		
		String[] stock_name_list = LibraryIO.loadFileAsLines("D:\\Documents\\FatCatFatMeow\\Data\\StockName.txt");
		for (String ss : stock_name_list)
			lexion.put(ss.trim(), 1);
		
		List<String> keylist = new ArrayList<>(lexion.keySet());
		Collections.sort(keylist);
		StringBuilder sb = new StringBuilder();
		for (String s : keylist) {
			Matcher m = p.matcher(s);
			if (m.find()) continue;
			if (s.trim().isEmpty()) continue;
			sb.append(s + "\r\n");
		}
		LibraryIO.writeFile(fileOutPath, sb.toString());
	}
	
	/**
	 * 使用 Tuned 過的 Lexion 再斷詞一次文章的部分
	 */
	
	public static void _02_ReWordSeg() throws Exception {
		String dirInnPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Stock\\";
		String dirOutPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Stock_Seg2\\";
		
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, tunedLexion);
		ws.MaximumMatch("", 1);
		
		new CorpusLabProcess("Word segmention", dirInnPath, dirOutPath, 4) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {		
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String content = LibraryIO.loadFile(fin).replaceAll("\r", "");
					int i = content.indexOf("※ 發信站:");
					if (i < 0) {
						System.err.println(fin.getName());
						i = content.indexOf("※ 文章網址");
						if (i < 0) i = content.length();
					}
					String seged = ws.MaximumMatch(content.substring(0, i), 1).replaceAll("\n", "\r\n");;
					content = new String(seged.getBytes("UTF-8"), "UTF-8");
					System.out.println(fin.getName() + " done segmention");
					LibraryIO.writeFile(dirOutPath + fin.getName(), content);
				}
			}
		}.start();
	}
	
	/**
	 * 決定文章的主體是誰
	 */
	
	public static void _03_DetermineSubject() throws Exception {
		makeStockList();
		makeArticleDateList();
		String dirInnPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Stock_Seg2\\";
		String fileOutPath = "D:\\Documents\\FatCatFatMeow\\PTT\\ArticleSubjectDetermine.txt";
		FileWriter fw = new FileWriter(new File(fileOutPath));
		
		new CorpusLabAnalysis("Determine article subject", dirInnPath, 1) {
			
			@Override
			public void run(SyncQueue<File> sq) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String title = fin.getName();
					String[] segs = LibraryIO.loadFile(fin).split("[\\s\r\n]");
					// Start finding subject
					boolean isMainSubjectFind = false;
					String subject = "";
					Map<String, Integer> isRe = new HashMap<>();
					for (int i = 0; i < segs.length - 3; i++) {
						if (segs[i+1].equals("(") && segs[i+3].equals(")")) {
							if (listStockID2Name.get(segs[i+2]) != null) {
								// System.out.println(fin.getName() + "\n" + segs[i] + "\t" + segs[i+2] + "\t" + listStockID2Name.get(segs[i+2]));
								subject = segs[i] + "\t" + segs[i+2] + "\t" + listStockID2Name.get(segs[i+2]);
							}
						} else if (listStockName2ID.get(segs[i]) != null) {
							// System.out.println(fin.getName() + "\n" + segs[i] + "\t" + listStockName2ID.get(segs[i]));
							subject = segs[i] + "\t" + listStockName2ID.get(segs[i]);
						} else if (segs[i].equals("-")) {
							String name = segs[i - 1] + segs[i] + segs[i + 1];
							if (listStockName2ID.get(name) != null)
								subject = name + '\t' + listStockName2ID.get(name);
						}
						if (!subject.isEmpty() && !isMainSubjectFind) {
							// System.out.printf("%s\t%s\t%s\t%s\t", args)
							// System.out.println(fin.getName());
							// System.out.println(articleTitleToURL(fin.getName()));
							// System.out.println("Main: " + subject);
							fw.write(String.format("%s\t%s\t%s\t%s\t%s\r\n", "Main", subject.split("\t")[1], title, articleTitleToURL(title), listArticleDate.get(title)));
							// System.out.printf("%s\t%s\t%s\t%s\t%s\n", "Main", subject.split("\t")[1], title, articleTitleToURL(title), listArticleDate.get(title));
							isMainSubjectFind = true;
						} else if (!subject.isEmpty() && isRe.get(subject) == null) { 
							// System.out.println("Sub:  " + subject);
							fw.write(String.format("%s\t%s\t%s\t%s\t%s\r\n", "Sub", subject.split("\t")[1], title, articleTitleToURL(title), listArticleDate.get(title)));
							// System.out.printf("%s\t%s\t%s\t%s\t%s\n", "Sub", subject.split("\t")[1], title, articleTitleToURL(title), listArticleDate.get(title));
						} isRe.put(subject, 1);
					}
				}
			}
		}.start();
		fw.close();
	}
	
	/**
	 * 使用 Tuned Lexion 斷詞留言的部分
	 */
	
	public static void _04_CommentWordSeg() throws Exception {
		String dirInnPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Comment\\";
		String dirOutPath = "D:\\Documents\\FatCatFatMeow\\PTT\\Comment_Seg\\";
		
		WordSegmentor ws = new WordSegmentor(alphabetListPath, segSymbolInfoPath, tunedLexion);
		ws.MaximumMatch("", 1);
		
		new CorpusLabProcess("Word segmention", dirInnPath, dirOutPath, 4) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {		
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String content = LibraryIO.loadFile(fin);
					String seged = ws.MaximumMatch(content.replace("\r", ""), 1).replaceAll("\n ", "\r\n");
					content = new String(seged.getBytes("UTF-8"), "UTF-8");
					System.out.println(fin.getName() + " done segmention");
					LibraryIO.writeFile(new File(dirOutPath + fin.getName()), content);
				}
			}
		}.start();
	}
	
	/**
	 * 將檔案名稱轉為文章網址
	 * @param title
	 * @return
	 */
	
	public static String articleTitleToURL(String title) {
		return "https://www.ptt.cc/" + title.substring(title.indexOf("bbs_"), title.length() - 4).replaceAll("_", "/") + ".html";
	}
	
	public static void makeStockList() throws Exception {
		listStockID2Name = new HashMap<>();
		listStockName2ID = new HashMap<>();
		String fileInnPath = "D:\\Documents\\FatCatFatMeow\\Data\\StockList.txt";
		String[] content = LibraryIO.loadFileAsLines(fileInnPath);
		
		for (String line : content) {
			String[] info = line.split("\t");
			listStockID2Name.put(info[1], info[0]);
			listStockName2ID.put(info[0], info[1]);
		}
	}
	
	public static void makeArticleDateList() throws Exception {
		listArticleDate = new HashMap<>();
		String fileInn = "D:\\Documents\\FatCatFatMeow\\PTT\\ArticleDate.txt";
		String[] lines = LibraryIO.loadFileAsLines(fileInn);
		for (String s : lines) {
			String[] ss = s.split("\t");
			listArticleDate.put(ss[0], ss[1]);
		}
	}
	
	public static void reopen() throws Exception {
		PrintStream out = new PrintStream(new FileOutputStream("D:\\Documents\\FatCatFatMeow\\ArticleSubjectDetermine.log"));
		System.setOut(out);
	}
}
