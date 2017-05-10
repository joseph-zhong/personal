package edu.washington.cs.grail.relative_size.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

public abstract class CollectionParallelComputation<T> {
	private static final Logger LOG = Logger.getLogger(CollectionParallelComputation.class.getName());
	private Collection<T> collection;
	private LinkedList<SingleElementComputation> threads = new LinkedList<CollectionParallelComputation<T>.SingleElementComputation>();

	public CollectionParallelComputation(Collection<T> collection) {
		this.collection = collection;
	}

	public abstract void run(T element);

	public CollectionParallelComputation<T> start() {
		for (T element : collection) {
			SingleElementComputation current = new SingleElementComputation(element);
			threads.add(current);
			current.start();
		}
		return this;
	}

	public CollectionParallelComputation<T> join(){
		while( !threads.isEmpty() ){
			SingleElementComputation current = threads.removeFirst();
			try {
				current.join();
			} catch (InterruptedException e) {
				LOG.warning("One thread joined with exception " + e.getMessage());
			}
		}
		return this;
	}

	private class SingleElementComputation extends Thread {
		private T element;

		public SingleElementComputation(T element) {
			this.element = element;
		}

		@Override
		public void run() {
			CollectionParallelComputation.this.run(element);
		}
	}
}
