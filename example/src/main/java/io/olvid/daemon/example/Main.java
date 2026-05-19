package io.olvid.daemon.example;

import io.grpc.StatusException;
import io.grpc.stub.BlockingClientCall;
import io.olvid.daemon.client.OlvidClient;
import io.olvid.daemon.command.v1.AuthenticationTestRequest;
import io.olvid.daemon.command.v1.DiscussionListRequest;
import io.olvid.daemon.command.v1.MessageSendRequest;
import io.olvid.daemon.datatypes.v1.Discussion;
import io.olvid.daemon.datatypes.v1.Message;
import io.olvid.daemon.notification.v1.MessageSentNotification;
import io.olvid.daemon.notification.v1.SubscribeToMessageSentNotification;
import io.olvid.daemon.services.v1.MessageNotificationServiceGrpc;

public class Main {
    public static void main(String[] args) throws StatusException, InterruptedException {
        OlvidClient client = OlvidClient.newBuilder().build();
        client.stubs.toolCommand.authenticationTest(AuthenticationTestRequest.newBuilder().build());
        System.out.println("Connected to daemon !");

        messageSentNotificationTest();
    }

    private static void messageSentNotificationTest() throws StatusException, InterruptedException {
        OlvidClient client = OlvidClient.newBuilder().build();
        var iterator = client.stubs.discussionCommand.discussionList(DiscussionListRequest.newBuilder().build());
        Discussion discussion = null;
        while (iterator.hasNext()) {
            discussion = iterator.next().getDiscussions(0);
        }
        assert discussion != null;

        BlockingClientCall<?, MessageSentNotification> messageSentBlockingClientCall = MessageNotificationServiceGrpc.newBlockingV2Stub(client.channel).messageSent(SubscribeToMessageSentNotification.newBuilder().setCount(1).build());
        Message sentMessage = client.stubs.messageCommand.messageSend(MessageSendRequest.newBuilder().setDiscussionId(discussion.getId()).setBody("Automatic message from java client tests").build()).getMessage();
        MessageSentNotification messageSentNotification = messageSentBlockingClientCall.read();
        assert messageSentNotification != null;
        assert messageSentNotification.getMessage().getId().equals(sentMessage.getId());
        assert messageSentNotification.getMessage().getBody().equals(sentMessage.getBody());
        System.out.println("Notification received: MESSAGE_SENT !");
    }
}