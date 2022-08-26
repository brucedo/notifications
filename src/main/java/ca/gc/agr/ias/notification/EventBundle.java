package ca.gc.agr.ias.notification;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBundle {

    Logger logger = LoggerFactory.getLogger(EventBundle.class);

    private EventHandler handler;
    private LocalDateTime lastTimeSent;
    private double[] sendingTimes = new double[15]; // averaging window
    private int currentWindowIndex = 0;

    public EventBundle()
    {
        lastTimeSent = LocalDateTime.MIN;
    }

    public EventBundle(EventHandler handler)
    {
        lastTimeSent = LocalDateTime.MIN;
        this.handler = handler;
    }

    public void poll() throws InterruptedException
    {
        LocalDateTime start = LocalDateTime.now();
        int sentCount = this.handler.poll();
        LocalDateTime end = LocalDateTime.now();

        // update statistics
        if (sentCount > 0)
        {
            this.lastTimeSent = end;
        }
        sendingTimes[currentWindowIndex] = (double)(LocalDateTime.from(start).until(end, ChronoUnit.MILLIS)) / 1000.00D;
    }

    public void issueHeartbeat() throws InterruptedException
    {
        if (this.handler.isInUse())
        {
            this.handler.issueHeartbeat();
            this.lastTimeSent = LocalDateTime.now();
        }
    }

    public EventHandler getHandler() {
        return handler;
    }

    public void setHandler(EventHandler handler) {
        this.handler = handler;
    }

    public LocalDateTime getLastTimeSent() {
        return lastTimeSent;
    }

    public double getAvgSendTime()
    {
        double avg = 0.0D;

        for (double time : sendingTimes)
        {
            avg += time;
        }

        avg = avg / 15D;

        return avg;
    }
    
}
