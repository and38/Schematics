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
			if (args.length == 4 || args.length == 5) {
				World world = Bukkit.getServer().getWorld(args[0]);
				int x = Integer.parseInt(args[1]);
				int y = Integer.parseInt(args[2]);
				int z = Integer.parseInt(args[3]);
				boolean remove = args.length == 5 ? args[4].equalsIgnoreCase("remove") : false;
				
				loader.loadSchematicAsync(schematicFile).thenAccept(o -> {
					Schematic schematic = null;
					try {
						schematic = o.orElseThrow(FileNotFoundException::new);
					} catch (FileNotFoundException e) {
						sender.sendMessage("Schematic file not found. Please add it to your schematics folder then do \"/sc <name>\".");
						return;
					}
					// Center the schematic creation at the player
					int newX = x - schematic.getWidth() / 2;
					int newZ = z - schematic.getLength() / 2;
					
					// If remove is active change schematic to remover
					schematic = remove ? schematic.createRemoverSchematic() : schematic;
					sender.sendMessage("Placing schematic: " + schematic.getName());
					loader.placeSchematicAsync(schematic, new Location(world, newX, y, newZ), 
							!remove ? 50 : 0, !remove ? 250 : 0, remove ? 500 : 0, remove);
				});
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("cancel")) {
					sender.sendMessage("Canceling...");
					loader.cancelPlacements();
				} else if (args[0].equalsIgnoreCase("list")) {
					sender.sendMessage("Current Placing: " + loader.getPlacements().toString());
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
