package org.daypilot.demo.html5schedulerspring.maintenance;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class EventRepositoryImpl implements EventRepository {

	private final Object lock = new Object();

	private final Map<Long,Event> storeById = new LinkedHashMap<>();
	private final NavigableMap<LocalDateTime,Set<Event>> indexByStartTime = new TreeMap<>();
	private final NavigableMap<LocalDateTime,Set<Event>> indexByEndTime = new TreeMap<>();
	private final AtomicLong id = new AtomicLong();

	@Override
	public <S extends Event> S save(final S entity) {
		if(Event.isValid(entity)) {
			final Event newVersion = new Event(entity);
			if(entity.getId() == null) {
			  newVersion.setId(id.getAndIncrement());
			  entity.setId(newVersion.getId());
			}
			synchronized(lock) {
				removeFromAllIndexes( // removing previous version from indexes
					storeById.put(newVersion.getId(), newVersion));
				indexByStartTime.computeIfAbsent(newVersion.getStart(), key -> new LinkedHashSet<>()).add(newVersion);
				indexByEndTime.computeIfAbsent(  newVersion.getEnd(),   key -> new LinkedHashSet<>()).add(newVersion);
			}
		}
		return entity;
	}

	@Override
	public Optional<Event> findById(Long id) {
		synchronized(lock) {
			final Event event = storeById.get(id);
			return event != null
				? Optional.of(new Event(event))
				: Optional.empty();
		}
	}

	@Override
	public void deleteById(final Long id) {
		if(id != null) synchronized(lock) {
			removeFromAllIndexes(
				storeById.remove(id));
		}
	}

	@Override
	public void delete(final Event entity) {
		if(entity != null && entity.getId() != null) synchronized(lock) {
			removeFromAllIndexes(
				storeById.remove(entity.getId()));
		}
	}

	@Override
	public List<Event> findBetween(final LocalDateTime start, final LocalDateTime end) {
		synchronized(lock) {
			return Stream.of(
				indexByStartTime.headMap(end, true).tailMap(start, true), // start >= event.startTime <= end
				indexByEndTime.tailMap(start, true).headMap(end, true))   // start >= event.endTime   <= end 
			  .map(Map::values)
			  .flatMap(Collection::stream)
			  .flatMap(Collection::stream)
			  .distinct()
			  .map(Event::new)
			  .toList();
		}
	}

	private void removeFromAllIndexes(final Event entity) {
		if(entity != null) {
			removeFromIndex(indexByStartTime, entity.getStart(), entity);
			removeFromIndex(indexByEndTime,   entity.getEnd(),   entity);
		}
	}

	private void removeFromIndex(final NavigableMap<LocalDateTime,Set<Event>> index, final LocalDateTime key, final Event event) {
		final Set<Event> eventSet =
			index.getOrDefault(key, Collections.emptySet());
		if(eventSet.remove(event) && eventSet.isEmpty()) {
			index.remove(key);
		}
	}
}
