package org.daypilot.demo.html5schedulerspring.maintenance;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ResourceRepositoryImpl implements ResourceRepository {

	private final Object lock = new Object();
	
	private Collection<Resource> cache;
	private Map<Long,Resource> storeById;
	private volatile boolean storeInitialized;
	
	private Runnable initializer = () -> {
		synchronized(lock) {
			if(!storeInitialized) {
				ResourceRepositoryImpl.this.cache = Collections.unmodifiableCollection(
					new ResourceSupplier().get());
				ResourceRepositoryImpl.this.storeById = cache.stream()
					.collect(Collectors.toMap(Resource::getId, Function.identity()));
				storeInitialized = true;
				ResourceRepositoryImpl.this.initializer = () -> {};
			}
		}
	};

	@Override
	public Optional<Resource> findById(Long id) {
		initializer.run();
		return Optional.ofNullable(storeById.get(id));
	}

	@Override
	public Iterable<Resource> findAll() {
		initializer.run();
		return cache;
	}
}
