package com.demo.thread;

import com.demo.assist.Listenable;
import com.demo.assist.Listener;
import com.demo.assist.PrintTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BetterThreadCorporation的同步版本
 * 通知下游的回调使用同步方式完成
 * 问题：
 *  轮次控制: 订阅关系的循环中引入了FlowController作为流程控制器
 */
public class BetterThreadCorporationSync {
	
	public static void main(String[] args) {
		
		TakeFromQueueRunnable aRunnable = new TakeFromQueueRunnable(1);
		TakeFromQueueRunnable bRunnable = new TakeFromQueueRunnable(2);
		TakeFromQueueRunnable cRunnable = new TakeFromQueueRunnable(3);
		FlowController flowController = new FlowController(10);
		
		aRunnable.subscribe(flowController);
		bRunnable.subscribe(aRunnable);
		cRunnable.subscribe(bRunnable);
		flowController.subscribe(cRunnable);
		
		Thread a = new Thread(aRunnable);
		Thread b = new Thread(bRunnable);
		Thread c = new Thread(cRunnable);
		a.start();
		b.start();
		c.start();
		
		flowController.event();
		
	}
	
	static class FlowController implements Listener,Listenable {
		
		private final AtomicInteger count;
		
		private final int max;
		
		private final List<Listener> subscriber = new ArrayList<>();
		
		FlowController(int max) {
			this.count = new AtomicInteger();
			this.max = max;
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
			if (count.incrementAndGet() < max){
				subscriber.forEach(Listener::event);
			}else {
				System.exit(0);
			}
		}
		
	}
	
	static class TakeFromQueueRunnable implements Runnable, Listenable, Listener {
		
		private final BlockingQueue<FutureTask<Boolean>> taskQueue = new ArrayBlockingQueue<>(1);
		
		private final List<Listener> subscriber = new ArrayList<>();
		
		private final int printTarget;
		
		public TakeFromQueueRunnable(int printTarget) {
			this.printTarget = printTarget;
		}
		
		@Override
		public void run() {
			while (true){
				try {
					Runnable task = taskQueue.take();
					task.run();
					subscriber.forEach(Listener::event);
				} catch (InterruptedException e) {
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
			FutureTask<Boolean> task = new FutureTask<>(new PrintTask(printTarget), true);
			try {
				taskQueue.put(task);
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
