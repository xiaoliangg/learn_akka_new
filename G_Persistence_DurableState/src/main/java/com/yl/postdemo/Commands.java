package com.yl.postdemo;

import akka.Done;
import akka.actor.typed.ActorRef;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/7/25 11:59
 **/
public class Commands {
    public interface Command {}
    public static class AddPost implements Command {
        final PostContent content;
        final ActorRef<AddPostDone> replyTo;

        public AddPost(PostContent content, ActorRef<AddPostDone> replyTo) {
            this.content = content;
            this.replyTo = replyTo;
        }
    }

    public static class AddPostDone implements Command {
        final String postId;

        public AddPostDone(String postId) {
            this.postId = postId;
        }
    }
    public static class GetPost implements Command {
        final ActorRef<PostContent> replyTo;

        public GetPost(ActorRef<PostContent> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class ChangeBody implements Command {
        final String newBody;
        final ActorRef<Done> replyTo;

        public ChangeBody(String newBody, ActorRef<Done> replyTo) {
            this.newBody = newBody;
            this.replyTo = replyTo;
        }
    }

    public static class Publish implements Command {
        final ActorRef<Done> replyTo;

        public Publish(ActorRef<Done> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class PostContent implements Command {
        final String postId;
        final String title;
        final String body;

        public PostContent(String postId, String title, String body) {
            this.postId = postId;
            this.title = title;
            this.body = body;
        }
    }
}
