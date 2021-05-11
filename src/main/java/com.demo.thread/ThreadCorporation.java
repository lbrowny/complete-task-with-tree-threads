package com.demo.thread;

import com.demo.assist.PrintTask;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 过程：
 *  线程通过一个共享的队列获取任务
 *  线程完成任务后发布任务到下一个线程
 * 问题：
 *  依赖关系: 上游的任务线程需要感知下游线程
 *  耦合：暴露了下游任务线程的实现细节给上游线程
 */
public class ThreadCorporation {
	
	public static void main(String[] args) {
		//初始化任务
		BlockingQueue<Runnable> allTaskQueue = new ArrayBlockingQueue<>(30);
		for (int i = 0; i <= 30; i++) {
			allTaskQueue.add(new PrintTask(i % 3 + 1));
		}
		
		//发布第一个任务
		BlockingQueue<Runnable> aTaskQueue = new ArrayBlockingQueue<>(1);
		Runnable poll = allTaskQueue.poll();
		assert poll != null;
		aTaskQueue.add(poll);
		
		BlockingQueue<Runnable> bTaskQueue = new ArrayBlockingQueue<>(1);
		BlockingQueue<Runnable> cTaskQueue = new ArrayBlockingQueue<>(1);
		Thread a = new Thread(new TakeFromQueueRunnable(aTaskQueue, bTaskQueue, allTaskQueue));
		Thread b = new Thread(new TakeFromQueueRunnable(bTaskQueue, cTaskQueue, allTaskQueue));
		Thread c = new Thread(new TakeFromQueueRunnable(cTaskQueue, aTaskQueue, allTaskQueue));
		a.start();
		b.start();
		c.start();
	}
	
	static class TakeFromQueueRunnable implements Runnable{
		
		private final BlockingQueue<Runnable> subscribeQueue;
		
		private final BlockingQueue<Runnable> publishQueue;
		
		private final BlockingQueue<Runnable> taskListQueue;
		
		public TakeFromQueueRunnable(BlockingQueue<Runnable> subscribeQueue, BlockingQueue<Runnable> publishQueue, BlockingQueue<Runnable> taskListQueue) {
			this.subscribeQueue = subscribeQueue;
			this.publishQueue = publishQueue;
			this.taskListQueue = taskListQueue;
		}
		
		@Override
		public void run() {
			try {
				while (true){
					//
					Runnable task = subscribeQueue.take();
					task.run();
					
					Runnable waitPublishTask = taskListQueue.poll(0, TimeUnit.MINUTES);
					if (waitPublishTask != null){
						publishQueue.put(waitPublishTask);
					}else {
						break;
					}
				}
			} catch (InterruptedException e) {
				//
				throw new RuntimeException(e);
			}
		}
	}
}
