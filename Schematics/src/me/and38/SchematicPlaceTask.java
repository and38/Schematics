package me.and38;

import java.io.File;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import me.and38.singletask.BukkitSingleTask;

public class SchematicPlaceTask extends BukkitSingleTask<SchematicPlaceTask> {

	private SchematicLoader loader;
	private Schematic schematic;
	private File schematicFile;
	private Location location;
	private long blockDelay;
	private long rowDelay;
	private long verticalDelay;
	private boolean topFirst;
	private boolean replaceWithAir;
	
	public SchematicPlaceTask(Plugin plugin, BukkitScheduler scheduler, SchematicLoader loader, 
			@Nullable Schematic schematic, @Nullable File schematicFile, Location loc, 
			long blockDelay, long rowDelay, long verticalDelay, boolean topFirst, boolean replaceWithAir) {
		super(plugin, scheduler);
		this.schematic = schematic;
		this.schematicFile = schematicFile;
		this.loader = loader;
		this.location = loc;
		this.blockDelay = blockDelay;
		this.rowDelay = rowDelay;
		this.verticalDelay = verticalDelay;
		this.topFirst = topFirst;
		this.replaceWithAir = replaceWithAir;
	}
	
	public SchematicPlaceTask callTask() {
		if (schematic == null) {
			if (schematicFile == null) {
				throw new RuntimeException("Schematic file was null");
			}
			schematic = loader.loadSchematic(schematicFile);
		}
		
		try {
			loader.placeSchematic(schematic, location, blockDelay, rowDelay, verticalDelay, topFirst, replaceWithAir);
		} catch (InterruptedException e) {
			System.out.println("Schematic with name \"" + schematic.getName() + "\" build canceled");
		}
		return this;
	}
	
	public String toString() {
		return "Task with ID: " + getTaskId() + " is Placing " + schematic.getName() + " at (" + location.getBlockX() + 
				", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
	}

}
