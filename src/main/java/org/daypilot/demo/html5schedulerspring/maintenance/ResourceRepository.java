package org.daypilot.demo.html5schedulerspring.maintenance;

import java.util.Optional;

public interface ResourceRepository {
	public Optional<Resource> findById(Long id);
	public Iterable<Resource> findAll();
}