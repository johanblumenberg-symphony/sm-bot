import java.net.SocketException;
import java.util.Map;

import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.parameter.*;
import net.fortuna.ical4j.util.UidGenerator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CalendarCreator {

    private final Logger logger = LoggerFactory.getLogger(CalendarCreator.class);

    Calendar createCalendarEvent(String meetingName, java.util.Date startDate,
            java.util.Date endDate, String organizerEmail, Map<Long, String> memberEmails) {
        try {

            // Create a TimeZone
            //TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
            //TimeZone timezone = registry.getTimeZone("America/Mexico_City");
            //VTimeZone tz = timezone.getVTimeZone();

            
            // Create the event
            String eventName = meetingName;
            DateTime start = new DateTime(startDate);
            DateTime end = new DateTime(endDate);
            VEvent meeting = new VEvent(start, end, eventName);

            // add timezone info..
            //meeting.getProperties().add(tz.getTimeZoneId());

            // generate unique identifier..
            UidGenerator ug = new UidGenerator("uidGen");
            Uid uid = ug.generateUid();
            meeting.getProperties().add(uid);
            // Version 2.0 
            meeting.getProperties().add(Version.VERSION_2_0);

            for (Map.Entry<Long, String> memberEmail : memberEmails.entrySet()) {
                if (!memberEmail.getValue().equals(organizerEmail)) {
                    Attendee attendee = new Attendee(java.net.URI.create("mailto:" + memberEmail.getValue()));
                    attendee.getParameters().add(Role.REQ_PARTICIPANT);
                    //dev1.getParameters().add(new Cn("Developer 1")); // Add if we want to add friendly name
                    meeting.getProperties().add(attendee);
                }
            }

            Organizer organizer = new Organizer(java.net.URI.create("mailto:" + organizerEmail));
            meeting.getProperties().add(organizer);

            
            Calendar icsCalendar = new Calendar();
            icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
            icsCalendar.getProperties().add(CalScale.GREGORIAN);

            // Add the event and print
            icsCalendar.getComponents().add(meeting);
            return icsCalendar;

        } catch (SocketException e) {
            logger.info("Got SocketException: " + e.getMessage());
            return null;
        }
        
    }
}