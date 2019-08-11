package me.and38;

import net.minecraft.server.v1_8_R3.NBTTagList;

public class Schematic {

	private String name;
	private short height;
	private short width;
	private short length;
	private byte[] blockData;
	private byte[] blocks;
	private NBTTagList tileEntities;

	public Schematic(String name, short height, short width, short length, byte[] blocks, byte[] blockData, NBTTagList tileEntities) {
		this.name = name;
		this.height = height;
		this.width = width;
		this.length = length;
		this.blockData = blockData;
		this.blocks = blocks;
		this.tileEntities = tileEntities;
	}

	public Schematic createRemoverSchematic() {
		return new Schematic(name + "|Remover", height, width, length, new byte[blocks.length], new byte[blockData.length], new NBTTagList());
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public short getHeight() {
		return height;
	}

	public short getWidth() {
		return width;
	}

	public short getLength() {
		return length;
	}

	public void setBlockData(byte[] blockData) {
		this.blockData = blockData;
	}
	
	public byte[] getBlockData() {
		return blockData;
	}

	public void setBlocks(byte[] blocks) {
		this.blocks = blocks;
	}
	
	public byte[] getBlocks() {
		return blocks;
	}

	public NBTTagList getTileEntities() {
		return tileEntities;
	}

	public String toString() {
		return String
				.format("Schematic[height=%d,width=%d,length=%d,blocksLength=%d,dataLength=%d,tileEntitites=%s",
						height, width, length,
						blockData.length, blocks.length,
						tileEntities.toString());
	}
}
