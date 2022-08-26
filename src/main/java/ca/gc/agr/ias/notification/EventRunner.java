package ca.gc.agr.ias.notification;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventRunner implements Runnable
{

    private final Logger logger = LoggerFactory.getLogger(EventRunner.class);

    private ConcurrentLinkedQueue<EventBundle> handlers = new ConcurrentLinkedQueue<>();

    // Statistics for this runner
    private volatile double secondsPerPass;
    private volatile double secondsBetweenHandlers;

    private volatile boolean quit = false;

    @Override
    public void run() 
    {
    
        double[] window = new double[15];
        int windex = 0; // get it?  window index? ha. ha. ha.

        // infinite loop.
        while (true)
        {
            try 
            {
                LocalDateTime start = LocalDateTime.now();
                logger.debug("There are {} handlers waiting for processing.", handlers.size());
                for (EventBundle handler : handlers)
                {
                    handler.poll();
                    logger.debug("Last datetime a message was sent: {}", handler.getLastTimeSent());
                    if (handler.getLastTimeSent().isBefore(LocalDateTime.now().minusSeconds(30)))
                    {
                        handler.issueHeartbeat();
                    }
                }

                
                LocalDateTime end = LocalDateTime.now();

                // Update statistics.

                if (windex % window.length == 0)
                {
                    double avg = Arrays.stream(window).sum() / window.length;
                    this.secondsPerPass = avg;
                    this.secondsBetweenHandlers = avg / (double)(handlers.size());
                }

                // Ask the thread to block for a bit if the turnaround time was very quick so it's not just spinning
                // doing nothing.
                if (LocalDateTime.from(start).until(end, ChronoUnit.MILLIS) < 100)
                {
                    Thread.sleep(500);
                }
            }
            catch (InterruptedException e)
            {
                logger.debug("Thread being asked to terminate...");
                break;
            }

            windex = (windex + 1) % window.length ;
        }
    }

    public double getSecondsPerPass()
    {
        return this.secondsPerPass;
    }

    public double getSecondsBetweenHandlers()
    {
        return this.getSecondsBetweenHandlers();
    }

    public void addHandler(EventHandler handler)
    {
        handlers.add(new EventBundle(handler));
    }

    public void retireEventHandler(EventHandler toRemove)
    {
        handlers.removeIf((t)-> t.getHandler() == toRemove);
    }
    
}
