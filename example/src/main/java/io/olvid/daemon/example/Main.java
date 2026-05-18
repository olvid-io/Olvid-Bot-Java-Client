package io.olvid.daemon.example;

import io.olvid.daemon.client.NotificationObserver;
import io.olvid.daemon.client.OlvidClient;
import io.olvid.daemon.command.v1.AuthenticationTestRequest;
import io.olvid.daemon.command.v1.DiscussionListRequest;
import io.olvid.daemon.command.v1.MessageSendRequest;
import io.olvid.daemon.datatypes.v1.Discussion;
import io.olvid.daemon.datatypes.v1.Message;
import io.olvid.daemon.notification.v1.MessageSentNotification;
import io.olvid.daemon.notification.v1.SubscribeToMessageSentNotification;

public class Main {
    public static void main(String[] args) {
        // Print a hello message to the console
        System.out.println("Hello, World!");

        OlvidClient client = OlvidClient.newBuilder().build();
        client.stubs.toolCommand.authenticationTest(AuthenticationTestRequest.newBuilder().build());

        messageSentNotificationTest();

    }

    private static void messageSentNotificationTest() {
        OlvidClient client = OlvidClient.newBuilder().build();
        var iterator = client.stubs.discussionCommand.discussionList(DiscussionListRequest.newBuilder().build());
        Discussion discussion = null;
        while (iterator.hasNext()) {
            discussion = iterator.next().getDiscussions(0);
        }
        assert discussion != null;

        Message[] sentMessage = new Message[1];

        client.stubs.messageNotification.messageSent(SubscribeToMessageSentNotification.newBuilder().setCount(1).build(), new NotificationObserver<>() {
            @Override
            public void onNext(MessageSentNotification notification) {
                System.out.println("message sent: " + notification.getMessage());
                assert sentMessage[0] != null;
                assert notification.getMessage().getId().equals(sentMessage[0].getId());
                assert notification.getMessage().getBody().equals(sentMessage[0].getBody());
            }
        });

        sentMessage[0] = client.stubs.messageCommand.messageSend(MessageSendRequest.newBuilder().setDiscussionId(discussion.getId()).setBody("Automatic message from java client tests").build()).getMessage();
    }
}