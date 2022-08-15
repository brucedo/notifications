package ca.gc.agr.ias.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.gc.agr.ias.notification.Message;
import ca.gc.agr.ias.serde.Count;

@Service
public class Publisher {

    Logger logger = LoggerFactory.getLogger(Publisher.class);
    
    private final NotificationCoordinator coordinator;

    @Autowired
    public Publisher(NotificationCoordinator coord)
    {
        this.coordinator = coord;
    }

    public void beginCount(Long partyId)
    {
        BlockingQueue<Message> pipe = coordinator.becomePublisher(partyId);

        Message greet = new Message();
        greet.msg = "HELO";
        pipe.add(greet);


        CompletableFuture.runAsync(()->{
            for (int i = 0; i < 100; i++)
            {
                logger.debug("Publishing a new event.");
                Message msg = new Message();
                msg.messageName = "counter";
                msg.msg = new Count().setCount(i).setOf(100);
                pipe.add(msg);
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    logger.debug("Message maker interrupted, quitting.");
                    break;
                }
            }
        });

        Message signoff = new Message();
        signoff.msg = "HUP";
        pipe.add(signoff);
    }
}
