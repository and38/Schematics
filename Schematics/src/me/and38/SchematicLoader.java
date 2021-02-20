package me.and38;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import me.and38.singletask.BukkitSingleTask;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;

import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.material.RedstoneTorch;
import org.bukkit.material.Torch;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.base.Predicate;


public class SchematicLoader {
	
	//TODO: Add more unsafe blocks...
	private static final Class<?>[] UNSAFE = {Torch.class, RedstoneTorch.class};
	private Set<SchematicPlaceTask> placements = new HashSet<>();
	
	private Plugin plugin;
	private BukkitScheduler scheduler;

	public SchematicLoader(Plugin plugin, BukkitScheduler scheduler) {
		this.plugin = plugin;
		this.scheduler = scheduler;
	}

	public CompletableFuture<Optional<Schematic>> loadSchematicAsync(File file) {
		return BukkitSingleTask.ofCallable(plugin, scheduler, () -> {
			return SchematicLoader.this.loadSchematic(file);
		}).callSingleAsync();
	}
	
	public Schematic loadSchematic(File file) {
		NBTTagCompound schematic = null;
		try (FileInputStream inputStream = new FileInputStream(file)) {
			schematic = NBTCompressedStreamTools.a(inputStream);
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
		if (schematic == null) {
			return null;
		}
		
		return new Schematic(file.getName().split("\\.")[0], schematic.getShort("Height"),
				schematic.getShort("Width"), schematic.getShort("Length"),
				schematic.getByteArray("Blocks"),
				schematic.getByteArray("Data"), 
				schematic.getList("TileEntities", 10)); // 10 is the value for NBTTagCompound, meaning the list element type
	}

	public void placeSchematicAsync(File file, Location loc, long block, long row, long vertical, boolean topFirst, boolean replaceWithAir) {
		SchematicPlaceTask task = new SchematicPlaceTask(plugin, scheduler, this, null, 
				file, loc, block, row, vertical, topFirst, replaceWithAir);
		placements.add(task);
		task.callSingleAsync().thenAccept(t -> t.ifPresent(placements::remove));
	}
	
	public void placeSchematicAsync(Schematic schematic, Location loc) {
		placeSchematicAsync(schematic, loc, 0, 0, 0, false, true);
	}
	
	public void placeSchematicAsync(Schematic schematic, Location loc, long block, long row, long vertical, boolean topFirst, boolean replaceWithAir) {
		SchematicPlaceTask task = new SchematicPlaceTask(plugin, scheduler, this, schematic, 
				null, loc, block, row, vertical, topFirst, replaceWithAir);
		placements.add(task);
		task.callSingleAsync().thenAccept(t -> t.ifPresent(placements::remove));
	}

	public void placeSchematic(Schematic schematic, Location location, long perBlock, long row, long vertical, boolean topFirst, boolean replaceWithAir) throws InterruptedException {
		if (schematic == null) return;
		//TODO: maybe cache this?
		Map<Location, Runnable> tileEntities = loadTileEntities(schematic, location);
		loadChunks(schematic, location);

		@SuppressWarnings("deprecation")
		ThrowableBiConsumer<byte[], Location, InterruptedException> blockPlacer = (b, loc) -> {
			Material material = getMaterial(b[0]);
			if (material != Material.AIR) {
				Thread.sleep(perBlock);
			}
			
			scheduler.callSyncMethod(plugin, () -> {
				if (!replaceWithAir && material == Material.AIR) return null;
				
				if (material == Material.AIR && loc.getBlock().getType() == Material.CHEST) {
					loc.getBlock().breakNaturally();
				}
				loc.getBlock().setTypeIdAndData(byteToInt(b[0]), b[1], false);
				loc.getWorld().playEffect(loc, Effect.STEP_SOUND, 
						Block.getCombinedId(((CraftWorld) loc.getWorld()).getHandle().getType(
								new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))));
				
				if (tileEntities.containsKey(loc)) {
					tileEntities.get(loc).run();
				}
				return null;
			});
		};
		
		HashMap<byte[], Location> delayed = new HashMap<>();
		// y is height, x is width, z is length.
		Predicate<Integer> yPredicate = (i) -> {
			if (topFirst) {
				return i > -1;
			} else {
				return i < schematic.getHeight();
			}
		};
		for (int y = topFirst ? schematic.getHeight() - 1 : 0; yPredicate.apply(y); y += topFirst ? -1 : 1) {
			boolean placed = false;
			for (int z = 0; z < schematic.getLength(); z++) {
				boolean placedRow = false;
				for (int x = 0; x < schematic.getWidth(); x++) {
					int index = (y * schematic.getLength() + z) * schematic.getWidth() + x;
					byte[] block = new byte[] { schematic.getBlocks()[index], schematic.getBlockData()[index] };
					if (isUnsafe(block[0])) {
						delayed.put(block, new Location(location.getWorld(), x, y, z));
						continue;
					}
					if (getMaterial(block[0]) != Material.AIR || topFirst) placed = placedRow = true;
					
					blockPlacer.accept(block, new Location(location.getWorld(),
							location.getBlockX() + x, location.getBlockY() + y,
							location.getBlockZ() + z));
				}
				if (placedRow)
					Thread.sleep(row);
			}
			if (placed)
				Thread.sleep(vertical);
		}
		for (Entry<byte[], Location> entry : delayed.entrySet()) {
			blockPlacer.accept(entry.getKey(), entry.getValue().add(location));
		}
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) { }
	}
	
	private void loadChunks(Schematic schematic, Location location) {
		World world = location.getWorld();
		Chunk cornerChunk = world.getChunkAt(location);
		
		for (int i = 0; i < schematic.getWidth() / 16; i++) {
			for (int j = 0; j < schematic.getLength() / 16; j++) {
				int chunkLocX = cornerChunk.getX() + i;
				int chunkLocZ = cornerChunk.getZ() + j;

				Chunk chunk = world.getChunkAt(chunkLocX, chunkLocZ);
				if (!chunk.isLoaded()) {
					System.out.println("trying to load chunk");
					chunk.load(true);
					System.out.println("Loaded chunk");
				}
			}
		}
	}
	
	private Map<Location, Runnable> loadTileEntities(Schematic schematic, Location location) {
		Map<Location, Runnable> map = new HashMap<>();
		CraftWorld world = (CraftWorld) location.getWorld();
		for (int i = 0; i < schematic.getTileEntities().size(); i++) {
			final NBTTagCompound compound = schematic.getTileEntities().get(i);
			final int adjustedX = compound.getInt("x") + location.getBlockX();
			final int adjustedY = compound.getInt("y") + location.getBlockY();
			final int adjustedZ = compound.getInt("z") + location.getBlockZ();
			Runnable updateTileEntity = () -> {
				TileEntity tileEntity = world.getTileEntityAt(adjustedX, adjustedY, adjustedZ);
				tileEntity.a(compound);
				tileEntity.a(new BlockPosition(adjustedX, adjustedY, adjustedZ));
				world.getHandle().getChunkAtWorldCoords(tileEntity.getPosition()).a(tileEntity);
			};
			
			map.put(new Location(location.getWorld(), adjustedX, adjustedY, adjustedZ), updateTileEntity);
		}
		return map;
	}
	
	public void cancelPlacements() {
		Iterator<SchematicPlaceTask> it = placements.iterator();
		while (it.hasNext()) {
			it.next().cancel();
			it.remove();
		}
	}
	
	public Set<SchematicPlaceTask> getPlacements() {
		return placements;
	}
	
	private boolean isUnsafe(byte id) {
		return Arrays.stream(UNSAFE).anyMatch(clazz -> clazz.isAssignableFrom(getMaterial(id).getData()));
	}
	
	private static int byteToInt(byte b) {
		return b & 0xFF;
	}
	
	@SuppressWarnings("deprecation")
	private Material getMaterial(byte id) {
		return Material.getMaterial(byteToInt(id));
	}

}
