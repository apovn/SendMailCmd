package core;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaEmail {

    Session mailSession;
    static String[] recipientArray;
    static int totalMemSent;
    static int countNumberPersonSentToday = 0;	// dem so lan gui moi lan bat chuong trinh
    String fromUser;
    String fromUserEmailPassword;
    String emailHost;
    int numPersonPerMailLimit;
	private Logger logger = MainSendMail.logger;
    static String[] byPassEmail = new String[20];
    
    public JavaEmail(){
        
        fromUser = MainSendMail.username;
        fromUserEmailPassword = MainSendMail.password;
        emailHost = "smtp.gmail.com";
        numPersonPerMailLimit = 95;
        
        initByPassEmail();
        if ( MainSendMail.selectedMailType.equalsIgnoreCase("Yahoo account") ){
            emailHost = "smtp.mail.yahoo.com";
        }
        
		recipientArray = MainSendMail.recipientBrowseArray;
    }
    
    public void setMailServerProperties() {
        Properties emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", "587");
        emailProperties.put("mail.smtp.auth", "true");
        emailProperties.put("mail.smtp.starttls.enable", "true");    
        emailProperties.put("mail.smtp.ssl.trust", "smtp.gmail.com"); // rat qtrong, de xac thuc quyen lquan https://www.google.com/settings/security/lesssecureapps
        mailSession = Session.getDefaultInstance(emailProperties, null);
    }

    public void sendEmail() throws AddressException, MessagingException, AuthenticationFailedException {
  
        // check moi luot gui
        if (countNumberPersonSentToday >= MainSendMail.totalPersonSentPmPerDayLimit) {
            return;
        }
                
        MimeMessage emailMessage = draftEmailMessage();

        Transport transport = mailSession.getTransport("smtp");
        transport.connect(emailHost, fromUser, fromUserEmailPassword);
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());   // test
        transport.close();
        System.out.println("Email sent successfully.");
        
        // write result
//        if (MainSendMail.isBrowseFile == false ){
//            writeLastPersonId2File(totalMemSent);
//        }
        
        logger .log(Level.INFO, "Sent to: {0}", Arrays.toString(emailMessage.getAllRecipients()));
        
        // het so luong gui trong 1 day
        if (countNumberPersonSentToday >= MainSendMail.totalPersonSentPmPerDayLimit) {
            Toolkit.getDefaultToolkit().beep();
        }        
        
    }
    
    public MimeMessage draftEmailMessage() throws AddressException, MessagingException {
        String[] toEmails;
        toEmails = recipientArray;
        
        String emailSubject = MainSendMail.mTitle;
        String emailBody = MainSendMail.mContent.toString();
        MimeMessage emailMessage = new MimeMessage(mailSession);
        
        /**
         * Set the mail recipients
         * Tinh so luong email moi lan gui
         */
        int countMemberPerMail = 0;
        for ( ; totalMemSent < toEmails.length; totalMemSent++) {
            try {
                if ( checkEmailBeforSend(toEmails) ) {
                    continue;
                }
                emailMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(toEmails[totalMemSent]));
            } catch (AddressException e) {
                continue;
            }
            countNumberPersonSentToday++;
            countMemberPerMail++;
            if (countMemberPerMail >= numPersonPerMailLimit || countNumberPersonSentToday >= MainSendMail.totalPersonSentPmPerDayLimit ){
                totalMemSent++; // tang truoc khi break khoi vong lap vi memberPerMail luon lon hon totalMemSent 1dv
                break;
            }
        }
        
        emailMessage.setFrom(new InternetAddress(fromUser));
        emailMessage.setSubject(emailSubject, "UTF-8");
        emailMessage.setHeader("Content-Type", "text/html; charset=UTF-8");
        
        if ( MainSendMail.isTextFormat ){   // text format
            emailMessage.setText(emailBody, "UTF-8");
        } else {
            emailMessage.setText(emailBody, "UTF-8", "html");
        }
        
        
        return emailMessage;
    }    

//    private void readRecipientList() throws FileNotFoundException, UnsupportedEncodingException {
//        
//        // Location of file to read
//        File file = new File("listEmail.txt");
//        StringBuilder text = new StringBuilder();
//
//        try {
//            try (Scanner scanner = new Scanner(file)) {
//                while (scanner.hasNextLine()) {
//                    String line = scanner.nextLine();
//                    text.append(line);
//                }
//            }
//            if ( text.toString().contains(",") ){
//                recipientArray = text.toString().split(",");
//            } else {
//                recipientArray = text.toString().split(";");
//            }
//            logger.log(Level.INFO, "Total member in listEmail.txt file: {0}", recipientArray.length);
//        } catch (IOException e) {
//            logger.log(Level.WARNING, "Can not read 'listEmail.txt' file");
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "Error3: {0}", e);
//        }
//    }
    
    public void writeLastPersonId2File(int lastPerId) {
        try {
            String lastPersonId = "mlastpsid:" + lastPerId + ",";
            File newTextFile = new File("SendMailLog.log");
            BufferedWriter writer; // true= append
            writer = new BufferedWriter(new FileWriter(newTextFile, true));
            writer.write(lastPersonId);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Write lastPerId error");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error4: {0}", e);
        }
    }
    
    public int getLastPersonSentId() {

        String lastPerId = null;
        try {
            InputStream inStream = new FileInputStream("SendMailLog.log");
            BufferedReader buffreader;  // read Unicode String from file
            buffreader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String line;
            StringBuilder text = new StringBuilder();
            while ((line = buffreader.readLine()) != null) {
                // detach msg list
                if (line.contains("mlastpsid")) {
                    text.append(line);
                }
            }
            buffreader.close();
            if (text.toString().isEmpty()) {
                logger.log(Level.WARNING, "Start from 0");
                return 0; // neu ko tim thay, start tu dau
            }
            String[] arrayValue = text.toString().split(",");
            lastPerId = arrayValue[arrayValue.length - 1].split(":")[1];


        } catch (IOException e) {
            logger.log(Level.WARNING, "Start from 0.");
            return 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error4: {0}", e);
            return 0;
        }

        return Integer.parseInt(lastPerId);
    }    

    private void initByPassEmail() {
        byPassEmail[0] = "hotro";
        byPassEmail[1] = "support";
        byPassEmail[2] = "admin";
        byPassEmail[3] = "quantri";
        byPassEmail[4] = "dieuhanh";
        byPassEmail[5] = "lienhe";
        byPassEmail[6] = "contact";
        byPassEmail[7] = "admicro";
        byPassEmail[8] = "webmaster";
    }

    private boolean checkEmailBeforSend(String[] toEmails) {
        boolean byPass = false;
        for (int i = 0; i < byPassEmail.length; i++) {
            
            if (byPassEmail[i] == null){
                break;
            }
            if ( toEmails[totalMemSent].toString().contains(byPassEmail[i]) ){
                byPass = true;
                break;
            }
        }
        
        if ("".equals(toEmails[totalMemSent].toString()) || !toEmails[totalMemSent].toString().contains("@")) {
            byPass = true;
        }
                   
        return byPass;
    }
}