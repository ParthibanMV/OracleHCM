# OracleHCM

Java Program to Invoke Oracle HCM Extract and BI Report using SOAP Service

bi_report.java
-- Oracle HCM BI report script 
- One soap call is made to trigger a BI Report 
- A second soap call is repeatedly made to monitor the status of the report 
- Report output csv file will be downloaded to specfied directory 
The success of the script can be used as a trigger of a subsequent file transfer. 
params are 
-1  HCM job name 
-2  number of times to poll when checking status 
-3  password 
-4  host server endpoint url (inc https) 
-5  username 
-6  wait time (in seconds) between status checks 
-7  report absolute path 
-8  template name 
-9 remotepath

hcm_extract.java
-- Oracle HCM data extract script 
- One soap call is made to trigger a data extraction 
- A second soap call is repeatedly made to monitor the status of the extraction 
The success of the script can be used as a trigger of a subsequent file transfer. 
params are 
1  HCM job name 
2  number of times to poll when checking status 
3  password 
4  host server endpoint url (inc https) 
5  username 
6  wait time (in seconds) between status checks


Sample Call
java bi_report JOBNAME POLL_COUNT USERNAME URL PASSWORD WAIT_TIME REPORTPATH TEMPLATE REMOTEPATH
