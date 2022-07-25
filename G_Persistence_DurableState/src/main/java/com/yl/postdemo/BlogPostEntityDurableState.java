package com.yl.postdemo;

import akka.Done;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.state.javadsl.CommandHandler;
import akka.persistence.typed.state.javadsl.CommandHandlerBuilder;
import akka.persistence.typed.state.javadsl.DurableStateBehavior;
import akka.persistence.typed.state.javadsl.Effect;
import com.yl.postdemo.Commands.*;
import com.yl.postdemo.States.BlankState;
import com.yl.postdemo.States.DraftState;
import com.yl.postdemo.States.PublishedState;
import com.yl.postdemo.States.State;
public class BlogPostEntityDurableState
        extends DurableStateBehavior<Command, State> {


    public static Behavior<Command> create(String entityId, PersistenceId persistenceId) {
        return Behaviors.setup(
                context -> {
                    context.getLog().info("Starting BlogPostEntityDurableState {}", entityId);
                    return new BlogPostEntityDurableState(persistenceId);
                });
    }

    private BlogPostEntityDurableState(PersistenceId persistenceId) {
        super(persistenceId);
    }

    @Override
    public State emptyState() {
        return BlankState.INSTANCE;
    }


    @Override
    public CommandHandler<Command, State> commandHandler() {
        CommandHandlerBuilder<Command, State> builder = newCommandHandlerBuilder();

        builder.forStateType(BlankState.class).onCommand(AddPost.class, this::onAddPost);

        builder
                .forStateType(DraftState.class)
                .onCommand(ChangeBody.class, this::onChangeBody)
                .onCommand(Publish.class, this::onPublish)
                .onCommand(GetPost.class, this::onGetPost);

        builder
                .forStateType(PublishedState.class)
                .onCommand(ChangeBody.class, this::onChangeBody)
                .onCommand(GetPost.class, this::onGetPost);

        builder.forAnyState().onCommand(AddPost.class, (state, cmd) -> Effect().unhandled());

        return builder.build();
    }

    private Effect<State> onAddPost(AddPost cmd) {
        return Effect()
                .persist(new DraftState(cmd.content))
                .thenRun(() -> cmd.replyTo.tell(new AddPostDone(cmd.content.postId)));
    }

    private Effect<State> onChangeBody(DraftState state, ChangeBody cmd) {
        return Effect()
                .persist(state.withBody(cmd.newBody))
                .thenRun(() -> cmd.replyTo.tell(Done.getInstance()));
    }

    private Effect<State> onChangeBody(PublishedState state, ChangeBody cmd) {
        return Effect()
                .persist(state.withBody(cmd.newBody))
                .thenRun(() -> cmd.replyTo.tell(Done.getInstance()));
    }

    private Effect<State> onPublish(DraftState state, Publish cmd) {
        return Effect()
                .persist(new PublishedState(state.content))
                .thenRun(
                        () -> {
                            System.out.println("Blog post published: " + state.postId());
                            cmd.replyTo.tell(Done.getInstance());
                        });
    }

    private Effect<State> onGetPost(DraftState state, GetPost cmd) {
        cmd.replyTo.tell(state.content);
        return Effect().none();
    }

    private Effect<State> onGetPost(PublishedState state, GetPost cmd) {
        cmd.replyTo.tell(state.content);
        return Effect().none();
    }

}