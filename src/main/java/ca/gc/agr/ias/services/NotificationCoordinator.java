package ca.gc.agr.ias.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    throws InterruptedException
    {
        EventHandler handler;
        logger.debug("subscribe method invoked.");
        synchronized(this) 
        {
            logger.debug("Acquiring the handler.");
            handler = handlerMap.computeIfAbsent(partyId, (k) -> 
            {
                EventHandler innerHandler = new EventHandler();
                try 
                {
                    logger.debug("Attempting to add a new EventHandler to the EventRunner.");
                    runner.addHandler(innerHandler); 
                }
                catch (InterruptedException e)
                {
                    logger.debug("InterruptionException thrown, I guess this is it.");
                    return null;
                }
                return innerHandler;
            });

            if (handler.isSinkClosed())
            {
                handler.setSink(new SseEmitter());
            }


        }
        logger.debug("Handler found; returning.");
        // if (!handler.isAlive())
        // {

        // }

        return handler.getSink();
    }

    public BlockingQueue<Message> becomePublisher(Long partyId)
    // throws InterruptedException
    {
        EventHandler handler;

        synchronized(this) 
        {
            handler = handlerMap.computeIfAbsent(partyId, (k) -> 
            { 
                EventHandler innerHandler = new EventHandler();
                try 
                {
                    logger.debug("Attempting to add a new EventHandler to the EventRunner.");
                    runner.addHandler(innerHandler); 
                }
                catch (InterruptedException e)
                {
                    logger.debug("InterruptionException thrown, I guess this is it.");
                    return null;
                }
                return innerHandler;
            });
        }
        return handler.getSource();
    }

    // @Scheduled(fixedDelay = 300000)
    // public void clearDeadHandlers()
    // {
    //     synchronized(this)
    //     {
    //         handlerMap.forEach((k,v)->
    //         {
                
    //         });
    //     }
    // }
}
