import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.ws.rs.core.NoContentException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clients.SymBotClient;
import exceptions.SymClientException;
import listeners.RoomListener;
import model.InboundMessage;
import model.OutboundMessage;
import model.RoomMember;
import model.Stream;
import model.UserInfo;
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
	private final CalendarCreator calendarCreator = new CalendarCreator();

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
    	Options helpOptions = new Options();
    	helpOptions.addOption("h", "help", false, "Show this help");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(helpOptions, Arrays.copyOfRange(args, 1, args.length), true);
    	return cmd.hasOption("help");
    }
    
    private Map<Long, String> getMemberEmails(String streamId) throws SymClientException, NoContentException {
    	Long botUserId = this.botClient.getBotUserInfo().getId();
    	List<RoomMember> members = this.botClient.getStreamsClient().getRoomMembers(streamId);
    	List<Long> memberIds = members.stream().map(member -> member.getId()).filter(id -> !id.equals(botUserId)).collect(Collectors.toList());
    	List<UserInfo> users = this.botClient.getUsersClient().getUsersFromIdList(memberIds, true);
    	return users.stream().collect(Collectors.toMap(UserInfo::getId, UserInfo::getEmailAddress));
    }
    
    private Date getEndTime(Date start, Integer duration) {
    	Calendar end = Calendar.getInstance();
    	end.setTime(start);
    	end.add(Calendar.MINUTE, duration);    
    	return end.getTime();
    }
    
    private void onBotMessage(String streamId, String sender, String message) {
    	logger.info("onBotMessage()");

        try {
        	String[] args = parseArgs(message);
        	
	        if (hasHelpOption(args)) {
	        	replyHelpMessage(streamId);
	        } else {
		        CommandLineParser parser = new DefaultParser();
		        CommandLine cmd = parser.parse(options, args);
		        
		        String date = cmd.getOptionValue("date");
	        	String time = cmd.getOptionValue("time");
	        	String subject = cmd.getOptionValue("subject");
	        	
	        	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	        	Date start = formatter.parse(date + " " + time);
	        	Date end = getEndTime(start, Integer.parseInt(cmd.getOptionValue("duration")));
	        	
	        	String realStreamId = streamId.replaceAll("_", "/");
	        	while (realStreamId.length() % 3 != 0) {
	        		realStreamId += '=';
	        	}
	        		        	
	        	URIBuilder b = new URIBuilder();
	        	b.setScheme("https");
	        	b.setHost(this.botClient.getConfig().getPodHost());
	        	b.setPort(this.botClient.getConfig().getPodPort());
	        	b.setPath("/client/rtc.html");
	        	b.addParameter("v2", "true");
	        	b.addParameter("startAudioMuted", "false");
	        	b.addParameter("startVideoMuted", "true");
	        	b.addParameter("streamId", realStreamId);
	        	URI meetingUrl = b.build();
	        	
	        	String msg = "Schedule meeting \"" + subject + "\" at " + start.toString() + " to " + end.toString() + "\n";
	        	msg += "Meeting URL: " + meetingUrl.toString() + "\n";
	        	
	        	Map<Long, String> memberEmails = getMemberEmails(streamId);
	        	for (Map.Entry<Long, String> memberEmail : memberEmails.entrySet()) {
	        		msg = msg + "\n  Inviting " + memberEmail.getKey() + " " + memberEmail.getValue();
	        	}
				
	        	replyMessage(streamId, msg);

	        	net.fortuna.ical4j.model.Calendar icsCalendar = calendarCreator.createCalendarEvent(subject, start, end, sender, memberEmails, meetingUrl.toString());
				
				List<String> receivers = memberEmails.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList());
	        	new EmailSender().sendEmail(subject, receivers, icsCalendar.toString());				
	        }
        } catch (ParseException e) {
        	logger.info("Got cli ParseException: " + e.getMessage());
        	replyHelpMessage(streamId, e.getMessage());
        } catch (java.text.ParseException e) {
        	logger.info("Got date ParseException: " + e.getMessage());
        	replyHelpMessage(streamId, e.getMessage());
		} catch (SymClientException e) {
        	logger.info("Got SymClientException: " + e.getMessage());
        	replyHelpMessage(streamId, e.getMessage());
		} catch(NoContentException e) {
        	logger.info("Got NoContentException: " + e.getMessage());
        	replyHelpMessage(streamId, e.getMessage());
		} catch (URISyntaxException e) {
			logger.info("Got URISyntaxException: ", e);
			replyHelpMessage(streamId, "Failed to schedule meeting");
		}
    }
    
    public void onRoomMessage(InboundMessage inboundMessage) {
    	String message = inboundMessage.getMessageText();
		String streamId = inboundMessage.getStream().getStreamId();
		String senderEmail = inboundMessage.getUser().getEmail();
    	
    	if (message.startsWith("/smbot")) {
    		onBotMessage(streamId, senderEmail, message);
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
