package org.daypilot.demo.html5schedulerspring.maintenance;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Event {
	
	public Event(final Event event) {
		if(event != null) {
			id = event.id;
			text = event.text;
			start = event.start;
			end = event.end;
			resource = event.resource;
			color = event.color;
		}
	}
	
	@JsonIgnore
	public static boolean isValid(final Event event) {
		return event != null && event.start != null && event.end != null && event.end.compareTo(event.start) >= 0;
	}
	
	@EqualsAndHashCode.Include
	Long id;
	
	String text;

	LocalDateTime start;

	LocalDateTime end;
	
	@JsonIgnore
	Resource resource;

	String color;
	
	@JsonProperty("resource")
	public Long getResourceId() {
		return resource.getId();
	}
}
