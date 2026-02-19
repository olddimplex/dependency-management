package org.daypilot.demo.html5schedulerspring.controller;
import java.time.LocalDateTime;

import org.daypilot.demo.html5schedulerspring.maintenance.Event;
import org.daypilot.demo.html5schedulerspring.maintenance.EventRepository;
import org.daypilot.demo.html5schedulerspring.maintenance.Resource;
import org.daypilot.demo.html5schedulerspring.maintenance.ResourceRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class DependencyManagementController {

    private final EventRepository er;

    private final ResourceRepository rr;

    @GetMapping("/resources")
    public Iterable<Resource> resources() {
        return rr.findAll();
    }

    @GetMapping("/events")
    public Iterable<Event> events(@RequestParam("start") final LocalDateTime start, @RequestParam("end") final LocalDateTime end) {
        return er.findBetween(start, end);
    }

    @PostMapping("/events/create")
    public Event createEvent(@RequestBody final EventCreateParams params) {
        return er.save(
        	params.createEvent(rr.findById(params.resource).orElse(null)));
    }

    @PutMapping("/events/update/{id}")
    public Event updateEvent(@PathVariable("id") final Long id, @RequestBody final EventCreateParams params) {
        return er.save(
        	params.updateEvent(er.findById(id).orElse(null)));
    }

    @DeleteMapping("/events/delete/{id}")
    public Event deleteEvent(@PathVariable("id") final Long id) {
        er.deleteById(id);
        return null;
    }

    @PostMapping("/events/move")
    public Event moveEvent(@RequestBody final EventMoveParams params) {

        return er.save(
        	params.moveEvent(er.findById(params.id).orElse(null), rr.findById(params.resource).orElse(null)));
    }

    @PostMapping("/events/setColor")
    public Event setColor(@RequestBody final SetColorParams params) {
        return er.save(
        	params.setColor(er.findById(params.id).orElse(null)));
    }

    public static class EventCreateParams {
        public LocalDateTime start;
        public LocalDateTime end;
        public String text;
        public Long resource;
        
        public Event createEvent(final Resource r) {
            final Event e = new Event();
            e.setStart(start);
            e.setEnd(end);
            e.setText(text);
            e.setResource(r);
            return e;
        }
        
        public Event updateEvent(final Event e) {
        	if(e != null) {
	            e.setStart(start);
	            e.setEnd(end);
	            e.setText(text);
        	}
        	return e;
        }
    }

    public static class EventMoveParams {
        public Long id;
        public LocalDateTime start;
        public LocalDateTime end;
        public Long resource;
        
        public Event moveEvent(final Event e, final Resource r) {
        	if(e != null) {
	            e.setStart(start);
	            e.setEnd(end);
	            e.setResource(r);
        	}
            return e;
        }
    }

    public static class SetColorParams {
        public Long id;
        public String color;
        
        public Event setColor(final Event e) {
        	if(e != null) {
        		e.setColor(color);
        	}
            return e;
        }
    }

}
