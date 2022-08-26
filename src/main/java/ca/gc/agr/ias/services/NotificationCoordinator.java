package ca.gc.agr.ias.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ca.gc.agr.ias.notification.EventHandler;
import ca.gc.agr.ias.notification.EventRunner;
import ca.gc.agr.ias.notification.Message;

@Service
public class NotificationCoordinator {

    private final Logger logger = LoggerFactory.getLogger(NotificationCoordinator.class);
    
    private final ConcurrentHashMap<Long, EventHandler> handlerMap = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor executor;

    private final Queue<Long> unusedStreams = new ArrayBlockingQueue<>(15);

    // private final ConcurrentHashMap<Long, EventRunner> runnerMap = new ConcurrentHashMap<>();

    private final EventRunner runner;

    @Autowired
    public NotificationCoordinator(ThreadPoolTaskExecutor executor)
    {
        this.executor = executor;
        this.runner = new EventRunner();
        this.executor.execute(this.runner);
        
        logger.debug("Active tasks: {}", this.executor.getActiveCount());
        
    }

    public SseEmitter subscribe(Long partyId)
    {
        EventHandler handler;
        logger.debug("subscribe method invoked.");
        logger.debug("A subscriber {} is attempting to add a new EventHandler to the EventRunner.", partyId);
        handler = handlerMap.computeIfAbsent(partyId, (k) -> 
        {
            EventHandler innerHandler = new EventHandler();
            logger.debug("A new EventHandler is being created for this subscriber.");
            return innerHandler;
        });

        runner.addHandler(handler); 
        logger.debug("Handler found; returning.");

        return handler.getSink();
    }

    public BlockingQueue<Message> becomePublisher(Long partyId)
    {
        EventHandler handler;

        logger.debug("A publisher for partyId {} is attempting to add a new EventHandler to the EventRunner.", partyId);
        handler = handlerMap.computeIfAbsent(partyId, (k) -> 
        { 
            logger.debug("A new EventHandler is being created for this publisher.");
            EventHandler innerHandler = new EventHandler();
            return innerHandler;
        });

        return handler.getSource();
    }

    @Scheduled(fixedDelay = 30000)
    public void clearDeadHandlers()
    {
        logger.debug("Scanning runners to find any disconnected ones.");
        // Evaluate the previously noted unused streams first.
        while (!unusedStreams.isEmpty())
        {
            
            Long key = unusedStreams.remove();
            EventHandler temp = handlerMap.get(key);
            logger.debug("Found one to remove: {}", key);
            if(!temp.isInUse())
            {
                runner.retireEventHandler(temp);
                handlerMap.remove(key);
            }
        }

        // Evaluate the running streams.
        handlerMap.forEach((k, v) -> 
        {
            if (!v.isInUse())
            {
                logger.debug("Found a newly unused EventHandler: {}.", k);
                unusedStreams.add(k);
            }
        });
    }
}
