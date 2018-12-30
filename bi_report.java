package samplesoap;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import javax.xml.soap.*;
import org.w3c.dom.NodeList;


class biJobNotCompletedException extends RuntimeException {
	
	
	private static final long serialVersionUID = 6512805206350139979L;
		
	String s;
	
	public biJobNotCompletedException(String s) {
		super(s);		
	}
}

public class bi_report {
	
	public static void main(String args[]) throws Exception {
	  try {
		if(args.length != 8)
	    {
	        System.out.println("Invalid arguments!! - Proper Usage is:");
	        usage();
	        System.exit(0);
	    }
		else {
		//Parameters
		int wait_time =0;
		int poll_count =0;
		String jobName = args[0];
		if (isInteger(args[1]))
		{
		 poll_count = Integer.parseInt(args[1]);
		} else
		{
			
			System.out.println("Invalid format for poll count please provide Integer!!");	       
	        System.exit(0);	
		}
		String password = args[2];
		String url = args[3];
		String username = args[4];
		if (isInteger(args[5]))
		{
		  wait_time = Integer.parseInt(args[5]);
		} else
		{
			
			System.out.println("Invalid format for wait_time please provide Integer!!");	       
	        System.exit(0);	
		}
		String reportpath = args[6];
		String template = args[7];
		String Remotepath = args[8];
		String url_path ="/xmlpserver/services/v2/ScheduleService?wsdl";
		String user_auth =username+":"+password;		
		String dateStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
		String timeStamp = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
        
		//Check Parameters
		System.out.println(jobName);
		System.out.println(poll_count);
		System.out.println(password);
		System.out.println(url);
		System.out.println(username);
		System.out.println(wait_time);
		System.out.println(reportpath);
		System.out.println(template);
		System.out.println(dateStamp+"_"+timeStamp);
        
        //Prepare Final SOAP
        String bi_submit_message = prepareSOAPmessage("Submit"); 
        bi_submit_message = bi_submit_message.replaceAll("REPORTPATH", reportpath);
        bi_submit_message = bi_submit_message.replaceAll("TEMPATE", template);
        bi_submit_message = bi_submit_message.replaceAll("JOBNAME", jobName);
        bi_submit_message = bi_submit_message.replaceAll("DATESTAMP", dateStamp);
        bi_submit_message = bi_submit_message.replaceAll("TIMESTAMP", timeStamp);
        bi_submit_message = bi_submit_message.replaceAll("USERNAME", username);
        bi_submit_message = bi_submit_message.replaceAll("PASSWORD", password);
        System.out.println(bi_submit_message);
        SOAPMessage submitResponse = callSoapWebService(url,url_path, bi_submit_message, user_auth);
        String jobId = printSOAPResponse(submitResponse,"scheduleReportReturn");        
        String bi_status_message = prepareSOAPmessage("Status");
        bi_status_message = bi_status_message.replaceAll("JOBID", jobId);
        bi_status_message = bi_status_message.replaceAll("USERNAME", username);
        bi_status_message = bi_status_message.replaceAll("PASSWORD", password);
        System.out.println(bi_status_message);        
        
        //int poll_count = 30;  
        String InstanceID = null;
        String status = null;
        String outputId = null;
        String documentdata = null;
        for(int i=1;i<=poll_count;i++)
        {
        	Thread.sleep(wait_time); //30 Seconds 30000
        	System.out.println("Sleep for 30 Seconds");
        	System.out.println( new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
        	System.out.println("Check Status: "+ i +" of "+poll_count);
        	SOAPMessage statusResponse = callSoapWebService(url,url_path, bi_status_message, user_auth);        	
        	status = printSOAPResponse(statusResponse,"status");
        	if (status.equals("S"))
        	{
        	System.out.println(jobName+" Job Completed Successfully");
        	InstanceID = printSOAPResponse(statusResponse,"jobId");
        	System.out.println("InstanceID"+ InstanceID);
        	break;        	
        	}
        }        
        
        isJobComplete(status);
                
        String bi_output_info = prepareSOAPmessage("GetOutputID");
        bi_output_info = bi_output_info.replaceAll("INSTANCEID", InstanceID);
        bi_output_info = bi_output_info.replaceAll("USERNAME", username);
        bi_output_info = bi_output_info.replaceAll("PASSWORD", password);
        System.out.println(bi_output_info); 
        SOAPMessage outinfoResponse = callSoapWebService(url,url_path, bi_output_info, user_auth);
        outputId = printSOAPResponse(outinfoResponse,"outputId");
                
        if (outputId != null )
        {
        String bi_doc_data = prepareSOAPmessage("GetDocument");
        bi_doc_data = bi_doc_data.replaceAll("OUTPUTID", outputId);
        bi_doc_data = bi_doc_data.replaceAll("USERNAME", username);
        bi_doc_data = bi_doc_data.replaceAll("PASSWORD", password);
        System.out.println(bi_doc_data); 
        SOAPMessage docdataResponse = callSoapWebService(url,url_path, bi_doc_data, user_auth);
        documentdata = printSOAPResponse(docdataResponse,"getDocumentDataReturn");
        byte[] documentdata_decoded = Base64.getDecoder().decode(documentdata.getBytes("utf-8"));
        String documentdata_decode =new String(documentdata_decoded, "UTF-8");
        FileWriter fileWriter = new FileWriter(Remotepath+jobName+"_"+dateStamp+"_"+timeStamp+".csv");
        fileWriter.write(documentdata_decode);
        fileWriter.close();      
        }
		}	
	  }
	  catch(hcmJobNotCompletedException e)
		{
		    	System.out.println(e) ;
		}
	}
		        
      
	public static void usage()
	{
		System.out.println(
				 "-- Oracle HCM BI report script \n"
				 +"- One soap call is made to trigger a BI Report \n"
				 + "- A second soap call is repeatedly made to monitor the status of the report \n"
				 + "- Report output csv file will be downloaded to specfied directory \n"
				 +"The success of the script can be used as a trigger of a subsequent file transfer. \n"
				 +"params are \n"				    
				 +   "1  HCM job name \n"
				 +   "2  number of times to poll when checking status \n"
				 +   "3  password \n"
				 +   "4  host server endpoint url (inc https) \n"
				 +   "5  username \n"
				 +   "6  wait time (in seconds) between status checks \n"
				 +   "7  report absolute path \n"
				 +   "8  template name \n"
				);
	}
	
	public static boolean isInteger(String s) {
	    try {
	    	Integer.parseInt(s);
	    } catch (NumberFormatException e) {
	        return false;
	    }
	    return true;
	}
    
	public static void isJobComplete(String s) throws biJobNotCompletedException 
	{ 
		if (!s.equals("S")) 
	   { 
		System.out.println(s);
		throw new biJobNotCompletedException( " Job did not complete before timeout"); 
	   }
	}

	private static String prepareSOAPmessage(String Action)
	{
		String bi_document ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\r\n" + 
				"   <soapenv:Header/>\r\n" + 
				"   <soapenv:Body>\r\n" + 
				"      <v2:getDocumentData>\r\n" + 
				"         <v2:jobOutputID>OUTPUTID</v2:jobOutputID>\r\n" + 
				"         <v2:userID>USERNAME</v2:userID>\r\n" + 
				"         <v2:password>PASSWORD</v2:password>\r\n" + 
				"      </v2:getDocumentData>\r\n" + 
				"   </soapenv:Body>\r\n" + 
				"</soapenv:Envelope>\r\n" + 
				"";
		String bi_output_id ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\r\n" + 
				"   <soapenv:Header/>\r\n" + 
				"   <soapenv:Body>\r\n" + 
				"      <v2:getScheduledReportOutputInfo>\r\n" + 
				"         <v2:jobInstanceID>INSTANCEID</v2:jobInstanceID>\r\n" + 
				"         <v2:userID>USERNAME</v2:userID>\r\n" + 
				"         <v2:password>PASSWORD</v2:password>\r\n" + 
				"      </v2:getScheduledReportOutputInfo>\r\n" + 
				"   </soapenv:Body>\r\n" + 
				"</soapenv:Envelope>";
		
		String bi_report_history ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\r\n" + 
				"   <soapenv:Header/>\r\n" + 
				"   <soapenv:Body>\r\n" + 
				"      <v2:getAllScheduledReportHistory>\r\n" + 
				"         <v2:filter>            \r\n" + 
				"            <v2:jobId>JOBID</v2:jobId>            \r\n" + 
				"         </v2:filter>\r\n" + 
				"         <v2:beginIdx>1</v2:beginIdx>\r\n" + 
				"         <v2:userID>USERNAME</v2:userID>\r\n" + 
				"         <v2:password>PASSWORD</v2:password>\r\n" + 
				"      </v2:getAllScheduledReportHistory>\r\n" + 
				"   </soapenv:Body>\r\n" + 
				"</soapenv:Envelope>";
		
		String bi_submit ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v2=\"http://xmlns.oracle.com/oxp/service/v2\">\r\n" + 
				"   <soapenv:Header/>\r\n" + 
				"   <soapenv:Body>\r\n" + 
				"      <v2:scheduleReport>\r\n" + 
				"         <v2:scheduleRequest>            \r\n" + 
				"            <v2:reportRequest>               \r\n" + 
				"               <v2:attributeFormat>CSV</v2:attributeFormat>\r\n" + 
				"               <v2:attributeLocale>en-US</v2:attributeLocale>\r\n" + 
				"               <v2:attributeTemplate>TEMPLATE</v2:attributeTemplate>\r\n" + 
				"               <v2:attributeTimezone>Europe/London</v2:attributeTimezone>\r\n" + 
				"               <v2:flattenXML>true</v2:flattenXML>\r\n" + 
				"               <v2:reportAbsolutePath>REPORTPATH</v2:reportAbsolutePath>\r\n" + 
				"               <v2:sizeOfDataChunkDownload>-1</v2:sizeOfDataChunkDownload>\r\n" + 
				"            </v2:reportRequest>\r\n" + 
				"            <v2:saveDataOption>false</v2:saveDataOption>\r\n" + 
				"            <v2:saveOutputOption>true</v2:saveOutputOption>            \r\n" + 
				"            <v2:userJobDesc>JOBNAME</v2:userJobDesc>\r\n" + 
				"            <v2:userJobName>JOBNAME_DATESTAMP_TIMESTAMP</v2:userJobName>\r\n" + 
				"         </v2:scheduleRequest>\r\n" + 
				"         <v2:userID>USERNAME</v2:userID>\r\n" + 
				"         <v2:password>PASSWORD</v2:password>\r\n" + 
				"      </v2:scheduleReport>\r\n" + 
				"   </soapenv:Body>\r\n" + 
				"</soapenv:Envelope>\r\n" + 
				"";
		if (Action == "Submit")
		return bi_submit;
		else if(Action == "Status")
		return bi_report_history;
		else if(Action == "GetOutputID")
		return bi_output_id;
		else
		return bi_document;
	}
	private static SOAPMessage createSOAPRequest(String message, String user_auth) throws Exception {
        InputStream is = new ByteArrayInputStream(message.getBytes());
        SOAPMessage request = MessageFactory.newInstance().createMessage(null, is);
        
        //start: setting HTTP headers - optional, comment out if not needed
        String authorization = Base64.getEncoder().encodeToString(user_auth.getBytes("utf-8"));
        MimeHeaders hd = request.getMimeHeaders();
        hd.addHeader("Authorization", "Basic " + authorization);
        //end: setting HTTP headers

        request.saveChanges();

        /* Print the request message, just for debugging purposes */
        System.out.println("Request SOAP Message:");
        request.writeTo(System.out);
        System.out.println("\n");

        return request;
    }	
	
	/**
	 * Method used to print the SOAP Response
	 */
	private static String printSOAPResponse(SOAPMessage soapResponse, String element) throws Exception
	{
	        
	 // get the body
        SOAPBody soapBody = soapResponse.getSOAPBody();
        // find your node based on tag name
        NodeList nodes = soapBody.getElementsByTagName(element);       
        // check if the node exists and get the value
        String status = null;
        Node node = (Node) nodes.item(0);
        status = node != null ? node.getTextContent() : "";
        //System.out.println(status);
        //System.out.println("\n");
        return status;
	}
	
		private static SOAPMessage callSoapWebService(String url, String url_path, String message ,String user_auth) {
            try {
            	                
                  // Create SOAP Connection
                SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
                SOAPConnection soapConnection = soapConnectionFactory.createConnection();
                //Set Timeout
                URL endpoint =
                		  new URL(new URL(url), url_path,
                		          new URLStreamHandler() {
                		            @Override
                		            protected URLConnection openConnection(URL url) throws IOException {
                		              URL target = new URL(url.toString());
                		              URLConnection soapConnection = target.openConnection();
                		              // Connection settings
                		              soapConnection.setConnectTimeout(10000); // 10 sec
                		              soapConnection.setReadTimeout(60000); // 1 min
                		              return(soapConnection);
                		            }
                		          });
                
                // Send SOAP Message to SOAP Server
                SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(message,user_auth), endpoint);

                // Print the SOAP Response
                //System.out.println("Response SOAP Message:");
                //soapResponse.writeTo(System.out);
                System.out.println("\n");
                if (!soapResponse.getSOAPBody().hasFault()) {
                	System.out.println("SOAP Call Success");
                } else {
                    SOAPFault fault = soapResponse.getSOAPBody().getFault();
                    System.out.println("Received SOAP Fault");
                    System.out.println("SOAP Fault Code :" + fault.getFaultCode());
                    System.out.println("SOAP Fault String :" + fault.getFaultString());                   
                }                
                soapConnection.close();
                return soapResponse;
            } catch (Exception e) {
                System.err.println("\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
                e.printStackTrace();
                System.exit(0);
            }
            return null;

	}

}
