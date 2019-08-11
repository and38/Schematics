package me.and38.singletask;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class BukkitCompletableFuture<T> extends CompletableFuture<T> {
	
	private Plugin plugin;
	private BukkitScheduler scheduler;
	
	public BukkitCompletableFuture(Plugin plugin, BukkitScheduler scheduler) {
		this.plugin = plugin;
		this.scheduler = scheduler;
	}
	
	@Override
	public CompletableFuture<Void> thenAccept(Consumer<? super T> consumer) {
		return super.thenAccept(t -> {
			new BukkitSingleTask<Void>(plugin, scheduler) {
				public void runTask() {
					consumer.accept(t);
				}
			}.callSingle();
		});
	}
	
	@Override
	public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> consumer) {
		return super.thenAccept(t -> {
			new BukkitSingleTask<Void>(plugin, scheduler) {
				public void runTask() {
					consumer.accept(t);
				}
			}.callSingleAsync();
		});
	}
	
	public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> function) {
		return super.<U>thenApply(t -> {
			try {
				return new BukkitSingleTask<U>(plugin, scheduler) {
					public U callTask() {
						return function.apply(t);
					}
				}.callSingle().get().get();
			} catch (Exception e) {
				return null;
			}
		});
	}
	
	public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> function) {
		return super.<U>thenApply(t -> {
			try {
				return new BukkitSingleTask<U>(plugin, scheduler) {
					public U callTask() {
						return function.apply(t);
					}
				}.callSingleAsync().get().get();
			} catch (Exception e) {
				return null;
			}
		});
	}
	
	public <U> CompletableFuture<Optional<U>> thenApplySafe(Function<? super T, ? extends U> function) {
		return super.<Optional<U>>thenApply(t -> {
			try {
				return new BukkitSingleTask<U>(plugin, scheduler) {
					public U callTask() {
						return function.apply(t);
					}
				}.callSingle().get();
			} catch (Exception e) {
				return Optional.empty();
			}
		});
	}
	
	public <U> CompletableFuture<Optional<U>> thenApplyAsyncSafe(Function<? super T, ? extends U> function) {
		return super.<Optional<U>>thenApply(t -> {
			try {
				return new BukkitSingleTask<U>(plugin, scheduler) {
					public U callTask() {
						return function.apply(t);
					}
				}.callSingleAsync().get();
			} catch (Exception e) {
				return Optional.empty();
			}
		});
	}
}
