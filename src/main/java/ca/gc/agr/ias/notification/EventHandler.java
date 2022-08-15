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

    private final AtomicInteger publisherCount = new AtomicInteger(0);

    private BlockingQueue<Message> source = new ArrayBlockingQueue<>(50);

    public EventHandler()
    {
        sink = new SseEmitter(-1L);
        logger.debug("Timeout of the emitter: {}", sink.getTimeout());
        source = new LinkedBlockingQueue<>();
        sink.onCompletion(this::setComplete);
    }

    public void setComplete()
    {
        this.complete.set(true);
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

        preProcessMessage(msg);            
        
    }

    public boolean isAlive()
    {
        return (!this.isSinkClosed() || this.publisherCount.get() > 0 || this.source.size() > 0);
    }

    // Handle the special message cases.
    private void preProcessMessage(Message msg) throws InterruptedException
    {
        if (msg.messageName == null)
        {
            if (msg.msg instanceof String)
            {
                String special = (String)msg.msg;
                if (special.startsWith("HELO"))
                {
                    this.publisherCount.getAndIncrement();
                }
                else if (special.startsWith("HUP"))
                {
                    this.publisherCount.decrementAndGet();
                }
            }
        }
        else
        {
            transmitMessage(msg);
        }
    }

    private void transmitMessage(Message msg) throws InterruptedException
    {
        SseEventBuilder builder = SseEmitter.event();

        Long seq = msgId.getAndAdd(1);

        builder.name(msg.messageName);
        builder.data(msg.msg, MediaType.APPLICATION_JSON_UTF8);
        builder.id("" + seq);

        // This message may have had it's sequence set if send was tried before, and failed.
        msg.seq = msg.seq == null ? seq : msg.seq;

        try 
        {
            if (this.complete.get())
                throw new IOException("Cannot send to completed stream.");
            sinkLock.acquire();
            this.sink.send(builder);
            sinkLock.release();
        }
        catch (IOException e)
        {
            // There may be some cause to check and evaluate here whether the SSE is dead or alive...
            logger.error("Unable to send message - IOExeception {}", e.getMessage());
            source.add(msg);
        }
    }

    public void setSink(SseEmitter sink) throws InterruptedException {

        sinkLock.acquire();
        this.sink = sink;
        sinkLock.release();

        // clear any backlog that has built while the sink was inaccessible.
        while (!this.source.isEmpty())
        {
            preProcessMessage(source.poll());
        }

    }

    public boolean isSinkClosed() 
    {
        return complete.get();
    }

    public SseEmitter getSink()
    {
        return this.sink;

    }

    public BlockingQueue<Message> getSource() {
        return source;
    }

    public void setSource(BlockingQueue<Message> source) {
        this.source = source;
    }

    
    
    
}
