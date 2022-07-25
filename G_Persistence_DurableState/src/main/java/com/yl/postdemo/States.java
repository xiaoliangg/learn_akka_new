package com.yl.postdemo;

import com.yl.postdemo.Commands.PostContent;
/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/7/25 11:57
 **/
public class States {
    interface State {}

    enum BlankState implements State {
        INSTANCE
    }

    static class DraftState implements State {
        final PostContent content;

        DraftState(PostContent content) {
            this.content = content;
        }

        DraftState withContent(PostContent newContent) {
            return new DraftState(newContent);
        }

        DraftState withBody(String newBody) {
            return withContent(new PostContent(postId(), content.title, newBody));
        }

        String postId() {
            return content.postId;
        }
    }

    static class PublishedState implements State {
        final PostContent content;

        PublishedState(PostContent content) {
            this.content = content;
        }

        PublishedState withContent(PostContent newContent) {
            return new PublishedState(newContent);
        }

        PublishedState withBody(String newBody) {
            return withContent(new PostContent(postId(), content.title, newBody));
        }

        String postId() {
            return content.postId;
        }
    }
}
