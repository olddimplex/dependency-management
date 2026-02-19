package org.daypilot.demo.html5schedulerspring.maintenance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository {
	public <S extends Event> S save(final S entity);
	public Optional<Event> findById(Long id);
	public void deleteById(final Long id);
	public void delete(final Event entity);
	public List<Event> findBetween(LocalDateTime start, LocalDateTime end);
}