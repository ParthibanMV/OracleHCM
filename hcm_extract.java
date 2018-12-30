package samplesoap;
import java.io.ByteArrayInputStream;
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


class hcmJobNotCompletedException extends Exception{
	
	
	private static final long serialVersionUID = 6512805206350139979L;
		
	String s;
	
	public hcmJobNotCompletedException(String s) {
		super(s);		
	}
}

public class hcm_extract {
	
	public static void main(String args[]) throws Exception {
	try	{
		if(args.length != 6)
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
			
		String url_path ="/hcmService/FlowActionsService?wsdl";
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
		System.out.println(dateStamp+"_"+timeStamp);
        
        //Prepare Final SOAP
        String hcm_submit_message = prepareSOAPmessage("Submit");
        String hcm_status_message = prepareSOAPmessage("Status");
        hcm_submit_message = hcm_submit_message.replaceAll("JOBNAME", jobName);
        hcm_submit_message = hcm_submit_message.replaceAll("DATESTAMP", dateStamp);
        hcm_submit_message = hcm_submit_message.replaceAll("TIMESTAMP", timeStamp);
        hcm_status_message = hcm_status_message.replaceAll("JOBNAME", jobName);
        hcm_status_message = hcm_status_message.replaceAll("DATESTAMP", dateStamp);
        hcm_status_message = hcm_status_message.replaceAll("TIMESTAMP", timeStamp);
        System.out.println(hcm_submit_message);
        System.out.println(hcm_status_message);
        
        //SOAPMessage submitResponse = callSoapWebService(url,url_path, hcm_submit_message, user_auth);
        //int poll_count = 30; 
        String status = null;
        for(int i=1;i<=poll_count;i++)
        {
        	Thread.sleep(wait_time); //30 Seconds 30000
        	System.out.println("Sleep for 30 Seconds");
        	System.out.println( new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
        	System.out.println("Check Status: "+ i +" of "+poll_count);
        	SOAPMessage statusResponse = callSoapWebService(url,url_path, hcm_status_message, user_auth);
        	
        	status = printSOAPResponse(statusResponse);
        	if (status.equals("COMPLETED"))
        	{
        	System.out.println(jobName+" Job Completed Successfully");
        	break;        	
        	}
        }
        isJobComplete(status);
		}	
	    }
	catch(hcmJobNotCompletedException e)
	{
	    	System.out.println(e) ;
	}
}
    
	public static void isJobComplete(String s) throws biJobNotCompletedException 
	{ 
		if (!s.equals("COMPLETE")) 
	   { 
		System.out.println(s);
		throw new biJobNotCompletedException( " Job did not complete before timeout"); 
	   }
	}
	public static void usage()
	{
		System.out.println(
				 "-- Oracle HCM data extract script \n"
				 +"- One soap call is made to trigger a data extraction \n"
				 + "- A second soap call is repeatedly made to monitor the status of the extraction \n"
				 +"The success of the script can be used as a trigger of a subsequent file transfer. \n"
				 +"params are \n"				    
				 +   "1  HCM job name \n"
				 +   "2  number of times to poll when checking status \n"
				 +   "3  password \n"
				 +   "4  host server endpoint url (inc https) \n"
				 +   "5  username \n"
				 +   "6  wait time (in seconds) between status checks \n"
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
    
	private static String prepareSOAPmessage(String Action)
	{
		String hcm_status ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:typ=\"http://xmlns.oracle.com/apps/hcm/processFlows/core/flowActionsService/types/\">\r\n" + 
				"   <soapenv:Header/>\r\n" + 
				"   <soapenv:Body>\r\n" + 
				"      <typ:getFlowTaskInstanceStatus>\r\n" + 
				"         <typ:flowInstanceName>JOBNAME_DATESTAMP_TIMESTAMP</typ:flowInstanceName>\r\n" + 
				"         <typ:flowTaskInstanceName>JOBNAME</typ:flowTaskInstanceName>\r\n" + 
				"      </typ:getFlowTaskInstanceStatus>\r\n" + 
				"   </soapenv:Body>\r\n" + 
				"</soapenv:Envelope>";
		
		String hcm_submit ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:typ=\"http://xmlns.oracle.com/apps/hcm/processFlows/core/flowActionsService/types/\" xmlns:flow=\"http://xmlns.oracle.com/apps/hcm/processFlows/core/flowControllerService/\">\r\n" + 
				"   <soapenv:Header/>\r\n" + 
				"   <soapenv:Body>\r\n" + 
				"      <typ:submitFlow>\r\n" + 
				"         <typ:flowName>JOBNAME</typ:flowName>\r\n" + 
				"         <typ:parameterValues>\r\n" + 
				"            <flow:ParameterName>Effective Date</flow:ParameterName>\r\n" + 
				"            <flow:ParameterValue>DATESTAMP</flow:ParameterValue>\r\n" + 
				"         </typ:parameterValues>\r\n" + 
				"         <typ:parameterValues>\r\n" + 
				"            <flow:ParameterName>Changes Only</flow:ParameterName>\r\n" + 
				"            <flow:ParameterValue>ATTRIB_OLD</flow:ParameterValue>\r\n" + 
				"         </typ:parameterValues>\r\n" + 
				"         <typ:flowInstanceName>JOBNAME_DATESTAMP_TIMESTAMP</typ:flowInstanceName>\r\n" + 
				"         <typ:recurringFlag>false</typ:recurringFlag>\r\n" + 
				"      </typ:submitFlow>\r\n" + 
				"   </soapenv:Body>\r\n" + 
				"</soapenv:Envelope>";
		if (Action == "Submit")
		return hcm_submit;
		else
		return hcm_status;
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
	private static String printSOAPResponse(SOAPMessage soapResponse) throws Exception
	{
	        
	 // get the body
        SOAPBody soapBody = soapResponse.getSOAPBody();
        // find your node based on tag name
        NodeList nodes = soapBody.getElementsByTagName("result");       
        // check if the node exists and get the value
        String status = null;
        Node node = (Node) nodes.item(0);
        status = node != null ? node.getTextContent() : "";
        System.out.println(status);
        System.out.println("\n");
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
                System.out.println("Response SOAP Message:");
                soapResponse.writeTo(System.out);
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
