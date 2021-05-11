package com.demo.thread;

import com.demo.assist.PrintTask;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 过程：
 *  线程通过一个共享的任务队列获取任务
 *  任务线程会从任务队列中  尝试获取任务 - 检查该任务是否该由自己执行 - 是则执行，否则将任务放回队列中，过程中使用一个共享锁来获取操作的原子性
 *  该过程会一直循环直到任务队列中没有任务可供执行
 * 问题：
 *  性能: 线程获取锁有不确定性，即使使用公平锁降低不确定性，最差情况下仍需要30(总计任务数)*3(线程数)=90次锁获取，并且该值随任务数及线程数线性增长
 */
public class ThreadCompete {
	
	public static void main(String[] args) {
		//初始化任务
		BlockingDeque<PrintTask> allTaskQueue = new LinkedBlockingDeque<>(30);
		for (int i = 0; i < 30; i++) {
			allTaskQueue.add(new PrintTask(i % 3 + 1));
		}
		
		//发布第一个任务
		Lock lock = new ReentrantLock(true);
		
		Thread a = new Thread(new TakeFromQueueRunnable(allTaskQueue, lock, 1));
		Thread b = new Thread(new TakeFromQueueRunnable(allTaskQueue, lock, 2));
		Thread c = new Thread(new TakeFromQueueRunnable(allTaskQueue, lock, 3));
		a.start();
		b.start();
		c.start();
	}
	
	static class TakeFromQueueRunnable implements Runnable{
		
		private final BlockingDeque<PrintTask> taskListQueue;
		
		private final Lock lock;
		
		private final int printTarget;
		
		public TakeFromQueueRunnable(BlockingDeque<PrintTask> allTaskQueue, Lock lock, int printTarget) {
			this.taskListQueue = allTaskQueue;
			this.lock = lock;
			this.printTarget = printTarget;
		}
		
		@Override
		public void run() {
			while (true){
				try {
					lock.lockInterruptibly();
					PrintTask task = taskListQueue.poll(0, TimeUnit.MINUTES);
					if (task == null){
						break;
					}
					if (task.getPrintTarget() == printTarget){
						task.run();
					}else {
						taskListQueue.putFirst(task);
					}
					
				} catch (InterruptedException e) {
					break;
				}finally {
					lock.unlock();
				}
			}
		}
	}
}
