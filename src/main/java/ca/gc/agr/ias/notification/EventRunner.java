package ca.gc.agr.ias.notification;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventRunner implements Runnable
{

    private final Logger logger = LoggerFactory.getLogger(EventRunner.class);

    private ArrayList<EventHandler> handlers = new ArrayList<>();

    // New EventHandlers given to EventRunner are placed here; processing begins after they are merged into the main set.
    private ArrayList<EventHandler> mergeQueue = new ArrayList<>();
    private Semaphore mergeQLock = new Semaphore(1);

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
            // logger.debug("Taking another spin through the message queue.");
            LocalDateTime start = LocalDateTime.now();
            try 
            {
                for (EventHandler handler : handlers)
                {
                    
                    handler.poll();
                }
            
                LocalDateTime end = LocalDateTime.now();

                // Update statistics.
                window[windex] = (double)(LocalDateTime.from(start).until(end, ChronoUnit.MILLIS)) / 1000.00D;

                if (windex % window.length == 0)
                {
                    double avg = Arrays.stream(window).sum() / window.length;
                    this.secondsPerPass = avg;
                    this.secondsBetweenHandlers = avg / (double)(handlers.size());
                }

                // Ask the thread to block for a bit if the turnaround time was very quick so it's not just spinning
                // doing nothing.
                if (window[windex] < 100)
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

            try
            {
                mergeNewHandlers();
            }
            catch (InterruptedException e)
            {
                logger.error("InterruptedException received while holding lock to merge new handlers.");
                quit = true;
                break;
            }
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

    public void addHandler(EventHandler handler) throws InterruptedException
    {
        mergeQLock.acquire();
        logger.debug("Adding new handler for pickup...");
        this.mergeQueue.add(handler);
        mergeQLock.release();
    }

    private void mergeNewHandlers() throws InterruptedException
    {
        mergeQLock.acquire();
        if (mergeQueue.size() > 0)
        {
            logger.debug("Adding new set of EventHandlers for running.");
        }
        handlers.addAll(mergeQueue);
        mergeQueue.clear();
        mergeQLock.release();
    }

    public void retireEventHandler(EventHandler toRemove) throws InterruptedException
    {
        mergeQLock.acquire();
        handlers.remove(toRemove);
        mergeQLock.release();
    }
    
}
