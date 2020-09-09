package here.lenrik1589.rsmm.time;

import net.minecraft.network.PacketByteBuf;

public class TickTime {
	public long tick;
	public Phase phase;
	public short index;

	public enum Phase {
		start,
		tickPending,
		blockEvents,
		entities,
		blockEntities
	}

	public TickTime (long tick, Phase phase) {
		this.tick = tick;
		this.phase = phase;
	}

	public TickTime setIndex (short ind) {
		index = ind;
		return this;
	}

	public void writeTime (PacketByteBuf buffer) {
		buffer.writeLong(tick);
		buffer.writeEnumConstant(phase);
		buffer.writeShort(index);
	}

	public static TickTime readTime (PacketByteBuf buffer) {
		return new TickTime(buffer.readLong(), buffer.readEnumConstant(Phase.class)).setIndex(buffer.readShort());
	}

	@Override
	public String toString () {
		return "TickTime{" +
						"tick=" + tick +
						", phase=" + phase +
						", index=" + index +
						'}';
	}

}
