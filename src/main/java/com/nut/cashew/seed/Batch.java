package com.nut.cashew.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Batch {
	private final int amountOfThread;
	private final List<Runnable> tasks = new ArrayList<>();

	public Batch(int amountOfThread) {
		if (amountOfThread <= 0) throw new IllegalArgumentException("Thread count must be > 0");
		this.amountOfThread = amountOfThread;
	}

	public Batch add(Runnable... runnables) {
		for (Runnable r : runnables) {
			tasks.add(r);
		}
		return this;
	}

	public void execute()  {
		ExecutorService executor = Executors.newFixedThreadPool(amountOfThread);
		List<Future<?>> futures = new ArrayList<>();

		for (Runnable task : tasks) {
			futures.add(executor.submit(task));
		}

		try {
			// Wait for all tasks to complete
			for (Future<?> future : futures) {
				try {
					future.get(); // will block until the task is done
				} catch (ExecutionException e) {
					e.getCause().printStackTrace(); // or handle as needed
				}
			}

			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.HOURS); // or adjust as needed
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
