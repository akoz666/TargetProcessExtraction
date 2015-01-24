TargetProcessExtraction allows you to extract User Stories informations which are stored in Target Process.

TargetProcessExtraction is coded in Java 8 !

To launch TargetProcessExtraction, you need to  
1. Install Firefox. Indeed TargetProcessExtraction is using Selenium API.   
2. Desactivate PDF reader plugin in Firefox. The how to it explained here [Remove PDF plugin in FireFox)(https://support.mozilla.org/fr/questions/950086)  
3. Adapt the config.properties file with your specifities :  

* tp.username => username to connect to Target Process => johndoe@gmail.com  
* tp.password => password to connect to Target Process => doedoe  
* tp.baseurl => secure url to connect to Target Process => https://easy.tpondemand.com  
* tp.nonsecurebaseurl => nonsecure url to connect to Target Process => http://easy.tpondemand.com  
* tp.userstory.url => secure url to acess user story via REST service => * https://easy.tpondemand.com/api/v1/Userstories  
* tp.userstory.url.attachment => option to download attachments => ?include=[Attachments]  
* tp.attachment.url => url to download attachment => https://easy.tpondemand.com/Attachment.aspx?attachmentID=  
* tp.attachment.timeoutdownload => timeout to download an attachment (in ms)> => 20000  
* tp.attachment.typemime => list of mime type you accept to download 
* tp.connection.url => authentification url to target process => https://easy.tpondemand.com/login.aspx  
* inputuserstorieslistfile => path where is located the csv file which is containing US ID to extract => C:\\targetprocess\\us.csv  
* outputpathuserstoriessaving => path where will be stored all informations extracted from Target Process => c:\\targetprocess  
