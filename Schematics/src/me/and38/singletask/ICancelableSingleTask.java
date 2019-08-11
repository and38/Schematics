package me.and38.singletask;

public interface ICancelableSingleTask extends ISingleTask {
	void cancel();
	boolean isCanceled();
}
