package nlp.cse.ntou.edu.object;

import static org.oppai.LibraryUtils.log;

import java.io.File;
import java.util.*;

import org.oppai.LibraryUtils;

import nlp.cse.ntou.edu.extend.SyncQueue;

public abstract class CorpusLabAnalysis {
	String processName;
	String inputDirPath;
	int numThread;
	int returnVal = 0;
	Map<String, Integer> map = new HashMap<>();
	
	public CorpusLabAnalysis(String _processName, String _inputDirPath, int _numThread) {
		init(_processName, _inputDirPath, _numThread);
	}
	
	public CorpusLabAnalysis(String _processName, String _inputDirPath) {
		init(_processName, _inputDirPath, 8);
	}
	
	public void init(String _processName, String _inputDirPath, int _numThread) {
		processName = _processName;
		inputDirPath = _inputDirPath;
		numThread = _numThread;
		returnVal = 0;
	}
	
	public synchronized void setValue(String key, Integer n) {
		if (map.get(key) == null) map.put(key, 0);
		map.put(key, map.get(key) + n);
	}
	
	public synchronized int getValue(String key) {
		return map.get(key);
	}
	
	public synchronized void setReturnVal(int n) {
		returnVal += n;
	}
	
	public int getReturnVal() {
		return returnVal;
	}
	
	public void start() throws Exception {
		File inputDir = new File(inputDirPath);
		
		log(processName + " begin");
		LibraryUtils.timestamp();
		
		SyncQueue<File> sq = new SyncQueue<File>(Arrays.asList(inputDir.listFiles()));

		Runnable r = ()->{ 
			try { run(sq); } 
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
	
	public abstract void run(SyncQueue<File> sq) throws Exception; 
}
