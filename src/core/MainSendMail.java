package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;


public class MainSendMail{

	static Logger logger = Logger.getLogger("MainApp");
	static FileHandler fh;
	
    static String username;
    static String password;
    static String selectedMailType = "Gmail account";
    static StringBuilder mContent;
    static String mTitle;
    static int totalPersonSentPmPerDayLimit = 9900;	// so nguoi gui toi da moi lan bat chuong trinh     
    public Timer timer = new Timer();
    int timePeriod = 1000 * 60 * 10 ; // 10p // test default
    JavaEmail javaEmail;
    static String[] recipientBrowseArray;
    static boolean isTextFormat = false;
	

	static{
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
	}
	
	public MainSendMail() {
	}
	
	public static void main(String[] args) throws Exception {
		
		if (!isValidateDate()){
			System.out.println("Lisence is invalid.");
			System.exit(0);
		}
		
		if (args.length == 0){
			System.out.println("Can nhap ds mail");
			System.exit(0);
		}
		
		MainSendMail task = new MainSendMail();
		task.initLogger();
		task.initData(args[0]);
		task.run();
        
	}
	
	
	private void run() {
		// TODO Auto-generated method stub
		javaEmail = new JavaEmail();
		javaEmail.setMailServerProperties();
		timer.schedule(new RemindTask1(), 0, timePeriod);
		
	}
	
	class RemindTask1 extends TimerTask {

        @Override
        public void run() {
            
            if (JavaEmail.totalMemSent >= JavaEmail.recipientArray.length) {	// da gui het danh sach
                
                timer.cancel();
                return;
            }

            // setup and send mail
            try {
                javaEmail.sendEmail();
            } catch (AuthenticationFailedException ex){
                // https://www.google.com/settings/security/lesssecureapps
            	logger.log(Level.INFO, "AuthenticationFailedException error " + ex.getMessage());
                return;
            } catch (MessagingException ex) {
            	logger.log(Level.INFO, "MessagingException error " + ex.getMessage());
            } catch (Exception ex) {
            	logger.log(Level.INFO, "Exception error " + ex.getMessage());
            }
        }
    }

	public void initData(String listMailFile) {
		// load property file
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("sysconfig/config.txt"));
			
            username = prop.getProperty("gmailuser").trim();
            password = prop.getProperty("gmailpass").trim();
            String tmp = prop.getProperty("totalPersonPerDay").trim();
            String tmp2 = prop.getProperty("timePerSendMailMin").trim();
            timePeriod = Integer.parseInt(tmp2) * 1000 * 60; // => second => milisecond
            mTitle = prop.getProperty("mailTitle").trim();
            mTitle = new String(mTitle.getBytes("ISO-8859-1"), "UTF-8");
            
            if (tmp != null) {
                int num = Integer.parseInt(tmp);
                if (num < totalPersonSentPmPerDayLimit) {
                    totalPersonSentPmPerDayLimit = num;
                }
            }
            
            StringBuilder text = new StringBuilder();
            Scanner reader = new Scanner(new File(listMailFile));
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                text.append(line);
            }
            
            if ( text.toString().contains(",") ){
                recipientBrowseArray = text.toString().split(",");
            } else {
                recipientBrowseArray = text.toString().split(";");
            }
            
            StringBuilder tmp3 = new StringBuilder();
            Scanner reader1 = new Scanner(new File("sysconfig/mailContentHtml.txt"));
            while (reader1.hasNextLine()) {
                String line = reader1.nextLine();
                tmp3.append(line);
            }

            mContent = tmp3;
		} catch (FileNotFoundException e) {
			logger.log(Level.INFO, "File ko tim thay AutoUpConfig.txt");
		} catch (IOException e) {
			logger.log(Level.INFO, "Doc file AutoUpConfig.txt loi");
		} catch (NumberFormatException e) {
			logger.log(Level.INFO, "So phut bi sai");
		} catch (Exception e) {
			logger.log(Level.INFO, "Loi khong xac dinh: " + e.getMessage());
		}
	}
	
	
	public void initLogger() {
		try {
			int limit = 10000000; // 10 Mb
			// This block configure the logger with handler and formatter
			fh = new FileHandler("error.log", limit, 1, true);
			fh.setEncoding("UTF-8");
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean isValidateDate() throws ParseException { // =true => het han su dung
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	Date expireDate = sdf.parse("2018-12-20");
    			
    	Date today = new Date();
		
    	if (expireDate.after(today)){
    		return true;
    	} 
    	
    	return false;
	}
}
