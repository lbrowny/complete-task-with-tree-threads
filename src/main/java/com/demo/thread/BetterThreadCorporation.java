package com.demo.thread;

import com.demo.assist.Listenable;
import com.demo.assist.Listener;
import com.demo.assist.PrintTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 过程：
 *  线程间通过订阅/通知的方式相互串联
 *  上游任务线程完成任务后通知下游任务线程
 * 问题：
 *  轮次控制: 任务通知-任务执行使用异步的方式，无法保证顺序，额外引入了barrier作为轮次控制器
 */
public class BetterThreadCorporation {
	
	public static void main(String[] args) throws BrokenBarrierException, InterruptedException {
		
		CyclicBarrier barrier = new CyclicBarrier(4);
		TakeFromQueueRunnable aRunnable = new TakeFromQueueRunnable(1, barrier);
		TakeFromQueueRunnable bRunnable = new TakeFromQueueRunnable(2, barrier);
		TakeFromQueueRunnable cRunnable = new TakeFromQueueRunnable(3, barrier);
		
		bRunnable.subscribe(aRunnable);
		cRunnable.subscribe(bRunnable);
		aRunnable.subscribe(cRunnable);
		
		Thread a = new Thread(aRunnable);
		Thread b = new Thread(bRunnable);
		Thread c = new Thread(cRunnable);
		a.start();
		b.start();
		c.start();
		
		aRunnable.event();
		for (int i = 0; i < 10; i++) {
			barrier.await();
			barrier.reset();
		}
		a.interrupt();
	}
	
	static class TakeFromQueueRunnable implements Runnable, Listenable, Listener {
		
		private final BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(1);
		
		private final List<Listener> subscriber = new ArrayList<>();
		
		private final int printTarget;
		
		private final CyclicBarrier barrier;
		
		public TakeFromQueueRunnable(int printTarget, CyclicBarrier barrier) {
			this.printTarget = printTarget;
			this.barrier = barrier;
		}
		
		@Override
		public void run() {
			while (true){
				try {
					Runnable task = taskQueue.take();
					task.run();
					subscriber.forEach(Listener::event);
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					break;
				}
			}
		}
		
		public void subscribe(Listenable listenable) {
			listenable.register(this);
		}
		
		@Override
		public void register(Listener listener) {
			subscriber.add(listener);
		}
		
		@Override
		public void event() {
			taskQueue.add(new PrintTask(printTarget));
		}
	}
}
