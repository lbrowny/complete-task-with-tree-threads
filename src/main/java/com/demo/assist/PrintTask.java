package com.demo.assist;


public class PrintTask implements Runnable {
	
	private final int printTarget;
	
	public PrintTask(int printTarget) {
		this.printTarget = printTarget;
	}
	
	public int getPrintTarget() {
		return printTarget;
	}
	
	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName() + " " + printTarget);
	}
}
