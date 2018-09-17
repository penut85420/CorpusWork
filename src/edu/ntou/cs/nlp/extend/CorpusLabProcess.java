package edu.ntou.cs.nlp.extend;

import static org.oppai.utils.LibraryUtils.log;

import java.io.File;
import java.util.*;
import org.oppai.utils.LibraryUtils;

public abstract class CorpusLabProcess {
	
	String processName;
	String inputDirPath;
	String outputDirPath;
	int numThread;
	
	public CorpusLabProcess(String _processName, String _inputDirPath, String _outputDirPath, int _numThread) {
		init(_processName, _inputDirPath, _outputDirPath, _numThread);
	}
	
	public CorpusLabProcess(String _processName, String _inputDirPath, String _outputDirPath) {
		init(_processName, _inputDirPath, _outputDirPath, 8);
	}
	
	public void init(String _processName, String _inputDirPath, String _outputDirPath, int _numThread) {
		processName = _processName;
		inputDirPath = _inputDirPath;
		outputDirPath = _outputDirPath;
		numThread = _numThread;
	}
	
	public void start() throws Exception {
		File inputDir = new File(inputDirPath);
		File outputDir = new File(outputDirPath);
		
		if (!outputDir.exists())
			outputDir.mkdirs();
		
		log(processName + " begin");
		LibraryUtils.timestamp();
		
		SyncQueue<File> sq = new SyncQueue<File>(Arrays.asList(inputDir.listFiles()));

		Runnable r = ()->{ 
			try { run(sq, outputDirPath); } 
			catch (Exception e) { e.printStackTrace(); } 
		};
		
		List<Thread> tlist = new ArrayList<>();
		
		for (int i = 0; i < numThread; i++) {
			tlist.add(new Thread(r));
			tlist.get(i).start();
		}
		
		for (Thread t : tlist) t.join();

		System.out.printf("%s done in %.3fs\n", processName, LibraryUtils.timestamp());
	}
	
	public abstract void run(SyncQueue<File> sq, String outputDirPath) throws Exception; 
	
}
