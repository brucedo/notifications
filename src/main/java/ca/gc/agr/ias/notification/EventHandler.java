package ca.gc.agr.ias.notification;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

public class EventHandler {

    private final Logger logger = LoggerFactory.getLogger(EventHandler.class);

    private volatile SseEmitter sink;
    private Semaphore sinkLock = new Semaphore(1);

    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final AtomicLong msgId = new AtomicLong(0);

    private BlockingQueue<Message> source = new ArrayBlockingQueue<>(50);

    // ***
    private Semaphore stateLock = new Semaphore(1);
    private final AtomicInteger publisherCount;
    // It's uncertain that emitter lives is necessary...
    private final AtomicBoolean emitterLives;
    private final AtomicBoolean emitterUsed;

    public EventHandler()
    {
        sink = new SseEmitter(-1L);
        logger.debug("Timeout of the emitter: {}", sink.getTimeout());
        source = new LinkedBlockingQueue<>();
        sink.onCompletion(this::setComplete);

        this.publisherCount = new AtomicInteger(0);
        this.emitterLives = new AtomicBoolean();
        this.emitterLives.set(true);
        this.emitterUsed = new AtomicBoolean();
        this.emitterUsed.set(false);
    }

    public void setComplete()
    {
        this.complete.set(true);
        this.emitterLives.set(false);
        resetSseEmitter();
        this.emitterLives.set(true);
        
        
    }

    private void resetSseEmitter()
    {
        try
        {
            this.sinkLock.acquire();
            this.sink = new SseEmitter(-1L);
            this.sinkLock.release();
            this.emitterUsed.set(false);
        }
        catch (InterruptedException e)
        {
            logger.debug("Interrupted exception caught - thread shutting down?");
        }
    }

    public void poll() throws InterruptedException {
        
        Message msg;

        logger.debug("EventHandler#poll() invoked.  Checking for messages...");

        msg = source.poll();

        if (msg == null)
        {
            return;
        }
        logger.debug("Processing some message for output...");

        transmitMessage(msg);            
        
    }

    public boolean isAlive()
    {
        return emitterLives.get();
    }

    public boolean isInUse()
    {
        return emitterUsed.get();
    }


    private void transmitMessage(Message msg) throws InterruptedException
    {
        SseEventBuilder builder = SseEmitter.event();

        builder.name(msg.messageName);
        builder.data(msg.msg, MediaType.APPLICATION_JSON_UTF8);
        msg.seq = msgId.getAndAdd(1);
        builder.id("" + msg.seq);

        try 
        {
            sinkLock.acquire();
            this.sink.send(builder);
            sinkLock.release();
        }
        catch (IOException e)
        {
            // if the send excepts, the release won't have happened.  Release the lock here.
            sinkLock.release();
            // There may be some cause to check and evaluate here whether the SSE is dead or alive...
            logger.error("Unable to send message - IOExeception {}", e.getMessage());
            resetSseEmitter();
            // Try to resend after reset
            try 
            {
                sinkLock.acquire();
                this.sink.send(builder);
                sinkLock.release();
            }
            catch (IOException f)
            {
                // If the second send attempt excepts, the release won't have happened.  Release here.
                sinkLock.release();
                logger.debug("Second try failed too - aborting.");
            }
        }
    }

    public SseEmitter getSink()
    {
        return this.sink;

    }

    public BlockingQueue<Message> getSource() {
        return source;
    }

    public int addPublisher()
    {
        return this.publisherCount.incrementAndGet();
    }

    public int retirePublisher()
    {
        return this.publisherCount.decrementAndGet();
    }

    public void acquireStateLock()
    throws InterruptedException
    {
        this.stateLock.acquire();
    }

    public void releaseStateLock()
    {
        this.stateLock.release();
    }
    
    
}
