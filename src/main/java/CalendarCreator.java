
//import net.fortuna.ical4j.*;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.*;
import net.fortuna.ical4j.util.*;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.parameter.*;

import java.net.SocketException;
import java.net.URI;
import java.util.Date;
import java.util.ArrayList;

public class CalendarCreator {

    net.fortuna.ical4j.model.Calendar createCalendarEvent(String meetingName, java.util.Date startDate,
            java.util.Date endDate, Attendee organizer, ArrayList<Attendee> attendeeList) {
        try {

            // Create a TimeZone
            TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
            TimeZone timezone = registry.getTimeZone("America/Mexico_City");
            VTimeZone tz = timezone.getVTimeZone();

            
            // Create the event
            String eventName = meetingName;
            DateTime start = new DateTime(startDate);
            DateTime end = new DateTime(endDate);
            VEvent meeting = new VEvent(start, end, eventName);

            // add timezone info..
            meeting.getProperties().add(tz.getTimeZoneId());

            // generate unique identifier..
            UidGenerator ug = new UidGenerator("uidGen");
            Uid uid = ug.generateUid();
            meeting.getProperties().add(uid);

            for (Attendee attendee : attendeeList)
            {
                if (attendee != organizer)
                {
                    attendee.getParameters().add(Role.REQ_PARTICIPANT);
                    meeting.getProperties().add(attendee);    
                }
            }
            
            organizer.getParameters().add(Role.CHAIR);
            meeting.getProperties().add(organizer);

            // Create a calendar
            net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
            icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
            icsCalendar.getProperties().add(CalScale.GREGORIAN);

            // Add the event and print
            icsCalendar.getComponents().add(meeting);
            return icsCalendar;

        } catch (SocketException e) {
            return null;
        }
    }
}