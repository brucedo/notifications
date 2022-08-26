package ca.gc.agr.ias.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ca.gc.agr.ias.services.NotificationCoordinator;
import ca.gc.agr.ias.services.Publisher;

@RestController
@RequestMapping("/notification")
public class Demo {

    private final Logger logger = LoggerFactory.getLogger(Demo.class);

    private Publisher publisher;
    private NotificationCoordinator coordinator;

    @Autowired
    public Demo(Publisher publisher, NotificationCoordinator coordinator)
    {
        this.publisher = publisher;
        this.coordinator = coordinator;
    }
    
    @PostMapping("/start/{partyId}")
    @ResponseStatus(HttpStatus.OK)
    public void startSending(@PathVariable Long partyId)
    {
        publisher.beginCount(partyId);
    }

    @GetMapping("/subscribe/{partyId}")
    public SseEmitter subscribe(@PathVariable Long partyId)
    {
            return coordinator.subscribe(partyId);   
    }
}
