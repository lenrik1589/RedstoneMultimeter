package narcolepticfrog.rsmm.clock;

import net.minecraft.network.PacketBuffer;

public class SubtickTime implements Comparable<SubtickTime> {

    private int tick;
    private int subtickIndex;

    public SubtickTime(int tick, int subtickIndex) {
        this.tick = tick;
        this.subtickIndex = subtickIndex;
    }

    public int getTick() {
        return tick;
    }

    public int getSubtickIndex() {
        return subtickIndex;
    }

    @Override
    public int compareTo(SubtickTime o) {
        if (this.tick != o.tick) {
            return this.tick - o.tick;
        } else {
            return this.subtickIndex - o.subtickIndex;
        }
    }

    public boolean equals(Object o) {
        if (o instanceof SubtickTime) {
            SubtickTime ot = (SubtickTime) o;
            return compareTo(ot) == 0;
        }
        return false;
    }

    public String toString() {
        return "SubtickTime[tick = " + tick + ", subtickIndex = " + subtickIndex + "]";
    }

    public void writeToBuffer(PacketBuffer buffer) {
        buffer.writeInt(tick);
        buffer.writeInt(subtickIndex);
    }

    public static SubtickTime readFromBuffer(PacketBuffer buffer) {
        int tick = buffer.readInt();
        int subtickIndex = buffer.readInt();
        return new SubtickTime(tick, subtickIndex);
    }

}
