package com.yl;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;

/**
 * @description: https://doc.akka.io/docs/akka/2.6/typed/actor-discovery.html#receptionist
 * @author: xiaoliang
 * @date: 2022/7/27 15:19
 **/
public class PingManager {

    interface Command {}

    enum PingAll implements Command {
        INSTANCE
    }

    private static class ListingResponse implements Command {
        final Receptionist.Listing listing;

        private ListingResponse(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new PingManager(context).behavior());
    }

    private final ActorContext<Command> context;
    private final ActorRef<Receptionist.Listing> listingResponseAdapter;

    private PingManager(ActorContext<Command> context) {
        this.context = context;
        // yltodo 消息适配器??
        this.listingResponseAdapter =
                context.messageAdapter(Receptionist.Listing.class, ListingResponse::new);

        context.spawnAnonymous(PingService.create());
    }

    private Behavior<Command> behavior() {
        return Behaviors.receive(Command.class)
                .onMessage(PingAll.class, notUsed -> onPingAll())
                .onMessage(ListingResponse.class, response -> onListing(response.listing))
                .build();
    }

    private Behavior<Command> onPingAll() {
        context
                .getSystem()
                .receptionist()
                // yltodo
                .tell(Receptionist.find(PingService.pingServiceKey, listingResponseAdapter));
        return Behaviors.same();
    }

    private Behavior<Command> onListing(Receptionist.Listing msg) {
        msg.getServiceInstances(PingService.pingServiceKey)
                .forEach(pingService -> context.spawnAnonymous(Pinger.create(pingService)));
        return Behaviors.same();
    }
}
