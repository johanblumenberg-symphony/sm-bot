import clients.SymBotClient;
import listeners.IMListener;
import model.InboundMessage;
import model.OutboundMessage;
import model.Stream;

public class IMListenerImpl implements IMListener {

    private SymBotClient botClient;

    public IMListenerImpl(SymBotClient botClient) {
        this.botClient = botClient;
    }

    public void onIMMessage(InboundMessage inboundMessage) {
        OutboundMessage messageOut = new OutboundMessage();
        messageOut.setMessage("Hi "+inboundMessage.getUser().getFirstName()+"!");
        try {
            this.botClient.getMessagesClient().sendMessage(inboundMessage.getStream().getStreamId(), messageOut);

            // add attendees..
//            Attendee dev1 = new Attendee(java.net.URI.create("mailto:dev1@mycompany.com"));
//            dev1.getParameters().add(Role.REQ_PARTICIPANT);
//            dev1.getParameters().add(new Cn("Developer 1"));
//            meeting.getProperties().add(dev1);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onIMCreated(Stream stream) {

    }
}
