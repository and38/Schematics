package me.and38.singletask;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

public abstract class BukkitSingleTask<T> extends BukkitRunnable implements ICallableSingleTask<T>, ICancelableSingleTask {
	private boolean done = false;
	private boolean async = false;
	private boolean canceled = false;
	private boolean cancelProcessed = false;
	private CompletableFuture<Optional<T>> future;
	protected Plugin plugin;
	protected BukkitScheduler scheduler;
	
	public BukkitSingleTask(Plugin plugin, BukkitScheduler scheduler) {
		this.plugin = plugin;
		this.scheduler = scheduler;
		this.future = new BukkitCompletableFuture<>(plugin, scheduler);
	}
	
	public static <T> BukkitSingleTask<T> ofCallable(Plugin plugin, BukkitScheduler scheduler, Callable<T> callable) {
		return new BukkitSingleTask<T>(plugin, scheduler) {
			public T callTask() throws Exception {
				return callable.call();
			}
		};
	}
	
	public static <T> BukkitSingleTask<T> ofRunnable(Plugin plugin, BukkitScheduler scheduler, Runnable runnable) {
		return new BukkitSingleTask<T>(plugin, scheduler) {
			public void runTask() {
				runnable.run();
			}
		};
	}
	
	public void runTask() { }
	
	public final void run() {
		T value = null;
		try {
			value = callTask();
		} catch (InterruptedException e) {
			this.cancelProcessed = true;
		} catch (Exception e) {
			future.completeExceptionally(e);
		} finally {
			this.done = true;
			future.complete(Optional.ofNullable(value));
		}
	}
	
	public boolean isDone() {
		return done;
	}
	
	public boolean isAsync() {
		return async;
	}
	
	public BukkitSingleTask<T> runAndRetrieveAsync() {
		runSingleAsync();
		return this;
	}
	
	public BukkitSingleTask<T> runAndRetrieve() {
		runSingle();
		return this;
	}
	
	public void runSingleAsync() {
		this.async = true;
		this.runTaskAsynchronously(plugin);
	}
	
	public void runSingle() {
		this.runTask(plugin);
	}
	
	public CompletableFuture<Optional<T>> callSingleAsync() {
		runSingleAsync();
		return future;
	}
	
	public CompletableFuture<Optional<T>> callSingle() {
		runSingle();
		return future;
	}
	
	public void cancel() {
		if (done) {
			return;
		}
		
		this.canceled = true;
		scheduler.getActiveWorkers().stream().filter(w -> w.getTaskId() == getTaskId())
			.findFirst().ifPresent(w -> w.getThread().interrupt());
		super.cancel();
	}
	
	public boolean isCanceled() {
		return this.canceled;
	}
	
	public boolean isCancelProcessed() {
		return this.cancelProcessed;
	}
	
}
