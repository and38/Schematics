package me.and38.singletask;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICallableSingleTask<T> extends ISingleTask {
	CompletableFuture<Optional<T>> callSingleAsync();
	CompletableFuture<Optional<T>> callSingle();
	default T callTask() throws Exception {
		runTask();
		return null;
	}
}
