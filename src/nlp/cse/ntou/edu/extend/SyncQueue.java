package nlp.cse.ntou.edu.extend;

import java.util.LinkedList;
import java.util.List;

public class SyncQueue<T extends Object> extends LinkedList<T> {

	private static final long serialVersionUID = 1L;

	public SyncQueue(List<T> asList) {
		super(asList);
	}

	@Override
	public synchronized T poll() {
		return super.poll();
	}
	
	@Override
	public synchronized boolean isEmpty() {
		return super.isEmpty();
	}
}
