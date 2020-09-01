package here.lenrik1589.rsmm.meter;

import here.lenrik1589.rsmm.time.TickTime;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class MeterEvent {
	public TickTime time;
	public Identifier meterId;
	public Event event;

	public MeterEvent (TickTime time, Identifier id, Event event) {
		meterId    = id;
		this.event = event;
		this.time  = time;
	}

	public enum Event {
		powered,
		unpowered,
		moved
	}

	public void writeEvent (PacketByteBuf buffer) {
		time.writeTime(buffer);
		buffer.writeIdentifier(meterId);
		buffer.writeEnumConstant(event);
	}

	public static MeterEvent readEvent (PacketByteBuf buffer) {
		return new MeterEvent(TickTime.readTime(buffer), buffer.readIdentifier(), buffer.readEnumConstant(Event.class));
	}

}
