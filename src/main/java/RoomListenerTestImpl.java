import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clients.SymBotClient;
import listeners.RoomListener;
import model.InboundMessage;
import model.OutboundMessage;
import model.RoomMember;
import model.Stream;
import model.User;
import model.events.RoomCreated;
import model.events.RoomDeactivated;
import model.events.RoomMemberDemotedFromOwner;
import model.events.RoomMemberPromotedToOwner;
import model.events.RoomUpdated;
import model.events.UserJoinedRoom;
import model.events.UserLeftRoom;

public class RoomListenerTestImpl implements RoomListener {

    private SymBotClient botClient;
    private final Logger logger = LoggerFactory.getLogger(RoomListenerTestImpl.class);

	Options options = new Options();

    public RoomListenerTestImpl(SymBotClient botClient) {
        this.botClient = botClient;
        
        options
	    	.addOption(Option.builder("d").longOpt("date").hasArg().desc("Date for the meeting in the format YYYY-MM-DD").required().build())
	    	.addOption(Option.builder("t").longOpt("time").hasArg().desc("Time for the meeting in the format HH:MM").required().build())
	    	.addOption(Option.builder("u").longOpt("duration").hasArg().desc("Duration for the meeting in minutes").required().build())
	    	.addOption(Option.builder("s").longOpt("subject").hasArg().desc("Subject of the meeting").required().build())
	    	.addOption(Option.builder("h").longOpt("help").desc("Show this help").build());
    }

    private void replyMessage(String streamId, String message) {
    	logger.info("Sending message to " + streamId);
    	logger.info(message);
    	
        OutboundMessage messageOut = new OutboundMessage();
        messageOut.setMessage(escapeHtml(message).replaceAll("\n", "<br />"));
        botClient.getMessagesClient().sendMessage(streamId, messageOut);
    }

    private void replyHelpMessage(String streamId) {
    	replyHelpMessage(streamId, null);
    }
    
    private void replyHelpMessage(String streamId, String error) {
    	StringWriter stringWriter = new StringWriter();
    	HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(new PrintWriter(stringWriter), 80, "/smbot", null, options, 0, 0, null, true);
        
        if (error != null) {
        	replyMessage(streamId, error + "\n" + stringWriter.toString());
        } else {
        	replyMessage(streamId, stringWriter.toString());
        }
    }
    
    private String[] parseArgs(String cmdline) throws ParseException {
    	try {
    		return CommandLineUtils.translateCommandline(cmdline);
    	} catch(Exception e) {
    		throw new ParseException(e.getMessage());
    	}
    }
    
    private boolean hasHelpOption(String[] args) throws ParseException {
    	Options options = new Options();
    	options.addOption("h", "help", false, "Show this help");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args, true);
    	return cmd.hasOption("help");
    }
    
    private void onBotMessage(String streamId, String message) {
    	logger.info("onBotMessage()");
    	
    	List<RoomMember> members = this.botClient.getStreamsClient().getRoomMembers(streamId);
    	
    	if (members == null) {
    		logger.error("Members is null");
    		replyMessage(streamId, "Failed to schedule meeting");
    		return;
    	}
    	
        try {
        	String[] args = parseArgs(message);
        	
	        if (hasHelpOption(args)) {
	        	replyHelpMessage(streamId);
	        } else {
		        CommandLineParser parser = new DefaultParser();
		        CommandLine cmd = parser.parse( options, args);
		        
		        String date = cmd.getOptionValue("date");
	        	String time = cmd.getOptionValue("time");
	        	String subject = cmd.getOptionValue("subject");
	        	
	        	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	        	Date d = formatter.parse(date + " " + time);

	        	String msg = "Schedule meeting \"" + subject + "\" at " + d.toString();
	        	
	        	for (RoomMember member : members) {
	        		msg = msg + "\n  Inviting " + member.getId();
	        	}
	        	
	        	logger.info(msg);
	        	replyMessage(streamId, msg);
	        }
        } catch (ParseException e) {
        	logger.info("Got cli ParseException: " + e.getMessage());
        	replyHelpMessage(streamId, e.getMessage());
        } catch (java.text.ParseException e) {
        	logger.info("Got date ParseException: " + e.getMessage());
        	replyHelpMessage(streamId, e.getMessage());
		}
    }
    
    public void onRoomMessage(InboundMessage inboundMessage) {
    	String message = inboundMessage.getMessageText();
    	String streamId = inboundMessage.getStream().getStreamId();
    	
    	if (message.startsWith("/smbot")) {
    		onBotMessage(streamId, message);
    	}
    }

    public void onRoomCreated(RoomCreated roomCreated) {
    }

    public void onRoomDeactivated(RoomDeactivated roomDeactivated) {
    }

    public void onRoomMemberDemotedFromOwner(RoomMemberDemotedFromOwner roomMemberDemotedFromOwner) {
    }

    public void onRoomMemberPromotedToOwner(RoomMemberPromotedToOwner roomMemberPromotedToOwner) {
    }

    public void onRoomReactivated(Stream stream) {
    }

    public void onRoomUpdated(RoomUpdated roomUpdated) {
    }

    public void onUserJoinedRoom(UserJoinedRoom userJoinedRoom) {
    }

    public void onUserLeftRoom(UserLeftRoom userLeftRoom) {
    }
}
