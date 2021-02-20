package me.and38;

import java.io.File;
import java.io.FileNotFoundException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Schematics extends JavaPlugin {
	
	private File schematicFile = new File(getDataFolder().getAbsolutePath() + File.separator + "schematics" + File.separator + "default.schematic");
	
	private SchematicLoader loader;
	private static final String COMMAND_PERMISSION_NODE = "schematics.command.sc";
	
	public void onEnable() {
		if (!schematicFile.getParentFile().exists()) {
			schematicFile.getParentFile().mkdirs();
		}
		loader = new SchematicLoader(this, Bukkit.getServer().getScheduler());
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {		
		if (!sender.hasPermission(COMMAND_PERMISSION_NODE) && !sender.isOp()) return false;
		
		if (cmd.getName().equalsIgnoreCase("sc")) {
			if (args.length == 8 || args.length == 9) {
				World world = Bukkit.getServer().getWorld(args[0]);
				int x = Integer.parseInt(args[1]);
				int y = Integer.parseInt(args[2]);
				int z = Integer.parseInt(args[3]);
				int blockDelay = Integer.parseInt(args[4]);
				int rowDelay = Integer.parseInt(args[5]);
				int verticalDelay = Integer.parseInt(args[6]);
				boolean replaceWithAir = Boolean.parseBoolean(args[7]);
				boolean isRemovalSchematic = args.length == 9 ? args[8].equalsIgnoreCase("remove") : false;
				
				sender.sendMessage("Loading schematic: " + schematicFile.getName());
				loader.loadSchematicAsync(schematicFile).thenAccept(o -> {
					Schematic schematic = null;
					try {
						schematic = o.orElseThrow(FileNotFoundException::new);
					} catch (FileNotFoundException e) {
						sender.sendMessage("Schematic file not found. Please add it to your schematics folder then do \"/sc <name>\".");
						return;
					}
					
					// Center the schematic creation at the specified coordinates
					int newX = x - schematic.getWidth() / 2;
					int newZ = z - schematic.getLength() / 2;
					
					schematic = isRemovalSchematic ? schematic.createRemoverSchematic() : schematic;
					sender.sendMessage("Placing schematic: " + schematic.getName());
					loader.placeSchematicAsync(schematic, new Location(world, newX, y, newZ), 
							blockDelay, rowDelay, verticalDelay, isRemovalSchematic, replaceWithAir);
				});
			} else if (args.length <= 2) {
				if (args[0].equalsIgnoreCase("cancel")) {
					sender.sendMessage("Canceling...");
					loader.cancelPlacements();
				} else if (args[0].equalsIgnoreCase("list")) {
					sender.sendMessage("Currently placing: " + loader.getPlacements().toString());
				} else {
					schematicFile = new File(getDataFolder().getAbsolutePath() + File.separator + "schematics" + File.separator + args[0] + ".schematic");
					sender.sendMessage("Current schematic set to " + args[0] + ".schematic");
				}
			} else {
				sender.sendMessage("Bad arguments");
			}
		}
		return true;
	}

}
