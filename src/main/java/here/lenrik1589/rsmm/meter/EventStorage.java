package here.lenrik1589.rsmm.meter;

import here.lenrik1589.rsmm.Names;

import java.util.*;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class EventStorage {

	private final Meter meter;
	public Long firstTick;
	public final Map<Long, Long>             tick2lastEventTickAfter  = new LinkedHashMap<>();
	public final Map<Long, Long>             tick2lastEventTickBefore = new LinkedHashMap<>();
	public final Map<Long, Boolean>          tick2Power               = new LinkedHashMap<>();
	public final Map<Long, List<MeterEvent>> meterEvents              = new LinkedHashMap<>();

	public EventStorage(Meter meter){
		this.meter = meter;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj instanceof EventStorage) {
			return meterEvents.equals(((EventStorage) obj).meterEvents) && meter.equals(((EventStorage) obj).meter);
		} else {
			return false;
		}
	}

	public void addEvent (MeterEvent event) {
//		Names.LOGGER.info(event);
		if(event.event == MeterEvent.Event.tickStart) {
			if(!meterEvents.isEmpty()) {
				tick2lastEventTickBefore.put(event.time.tick, Iterators.getLast(meterEvents.keySet().iterator()));
			}
		}else{
			tick2lastEventTickBefore.put(event.time.tick, event.time.tick);
			if (!meterEvents.containsKey(event.time.tick)) {
				meterEvents.put(event.time.tick, new ArrayList<>());
			}
			meterEvents.get(event.time.tick).add(event);
//			if (firstTick == null) {
//				firstTick = event.time.tick;
//				tick2lastEventTickAfter.put(event.time.tick, event.time.tick);
//			} else {
//				long tick = Iterators.getLast(tick2lastEventTickAfter.keySet().iterator());
//				if (tick != event.time.tick) {
//					for (long l = tick; l <= event.time.tick; l++) {
//						tick2lastEventTickAfter.put(l, event.time.tick);
//						tick2lastEventTickBefore.put(l, tick);
//					}
//				}
//			}
		}
	}

	public boolean isEmpty () {
		return meterEvents.isEmpty();
	}

	public MeterEvent getLastBefore (long tick) {
		if(isEmpty()){
			return null;
		}else{
			Long lastEventInd = tick2lastEventTickBefore.get(tick);
			if (lastEventInd == null){
				lastEventInd = Iterators.getLast(meterEvents.keySet().iterator());
			}
			ArrayList<MeterEvent> events = (ArrayList<MeterEvent>) meterEvents.get(lastEventInd);
			return Iterators.getLast(events.iterator());
		}
	}

	public MeterEvent getLastPowerBefore (long tick) {
		if(isEmpty()){
			return null;
		}else{
			Long lastEventInd = tick2lastEventTickBefore.get(tick);
			MeterEvent last;
			if(lastEventInd == null) {
				last = Iterators.getLast(meterEvents.get(Iterators.getLast(meterEvents.keySet().iterator())).iterator());
			}else{
				last = Iterators.getLast(meterEvents.get(lastEventInd).iterator());
			}
			if (lastEventInd == null || !last.event.isPower()){
				lastEventInd = Iterators.getLast(meterEvents.keySet().iterator());
				while (lastEventInd >= (Long)meterEvents.keySet().toArray()[0] && !last.event.isPower()) {

				}
			}
			return last;
		}
	}

	public MeterEvent getLast () {
		if (isEmpty()) {
			return null;
		} else {
			ArrayList<MeterEvent> lastEvents = (ArrayList<MeterEvent>) meterEvents.values().toArray()[meterEvents.size() - 1];
			return (lastEvents).get(lastEvents.size() - 1);
		}

	}

	public MeterEvent getLastPower () {
		if (isEmpty()) {
			return null;
		} else {
			MeterEvent last = getLast();
			int index = meterEvents.size() - 1;
			while (index > 0 && !last.event.isPower()) {
				index--;
				last = (MeterEvent) meterEvents.values().toArray()[index];
			}
			return last.event.isPower() ? last : null;
		}
	}

	public boolean containsTick (long tick) {
		return meterEvents.containsKey(tick);
	}

}
