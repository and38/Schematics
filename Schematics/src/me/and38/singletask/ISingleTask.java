package me.and38.singletask;

public interface ISingleTask {
	void runSingleAsync();
	void runSingle();
	ISingleTask runAndRetrieveAsync();
	ISingleTask runAndRetrieve();
	void runTask() throws InterruptedException;
}
