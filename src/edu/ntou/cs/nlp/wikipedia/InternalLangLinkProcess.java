package edu.ntou.cs.nlp.wikipedia;

import java.io.*;

import org.oppai.io.LibraryIO;

import edu.ntou.cs.nlp.extend.SyncQueue;
import edu.ntou.cs.nlp.object.CorpusLabProcess;

public class InternalLangLinkProcess {
	public static void main(String[] args) throws Exception {
		String dinput = "D:\\Documents\\Corpus\\wiki\\zh_interlanglink\\split\\";
		String doutput = "D:\\Documents\\Corpus\\wiki\\zh_interlanglink\\split_retrive\\";
		
		new CorpusLabProcess("Internal lang link retrive", dinput, doutput) {
			
			@Override
			public void run(SyncQueue<File> sq, String outputDirPath) throws Exception {
				while (!sq.isEmpty()) {
					File fin = sq.poll();
					String content = "";
					String[] lines = LibraryIO.loadFileAsLines(fin.getPath());
					for (String line : lines) {
						String[] sp = line.split(" ");
						String zh = sp[0].replaceFirst("<http://zh.dbpedia.org/resource/(.*?)>.*", "$1");
						String en = sp[1].replaceFirst(".*<http://dbpedia.org/resource/(.*?)>.*", "$1");
						content += zh + "\t" + en + "\n";
					}
					LibraryIO.writeFile(outputDirPath +  fin.getName(), content);
					System.out.println(fin.getName() + " done");
				}				
			}
		}.start();;
	}
}
