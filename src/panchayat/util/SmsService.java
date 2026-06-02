package panchayat.util;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * SmsService - A mock service to simulate sending SMS notifications.
 * In a real production system, this would integrate with an SMS gateway like Twilio.
 */
public class SmsService {
    
    /**
     * Simulates sending an SMS to all employees.
     * @param meetingType The type of meeting.
     * @param date The date of the meeting.
     * @param parent The parent component to show the simulation popup over.
     */
    public static void sendMeetingNotification(String meetingType, String date, Component parent) {
        // In a real application, you would make an HTTP request to Twilio API here.
        // Example Twilio integration:
        // Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        // Message message = Message.creator(new PhoneNumber(to), new PhoneNumber(from), msg).create();
        
        String msg = String.format("📱 SMS ALERT SENT TO EMPLOYEES:\n\n\"A new %s has been scheduled for %s. All concerned government employees are requested to attend. - Panchayat Office\"", meetingType, date);
        
        // Show a popup to simulate the SMS being sent out
        JOptionPane.showMessageDialog(parent, msg, "SMS Simulation Gateway", JOptionPane.INFORMATION_MESSAGE);
    }
}
