
## Welcome to dpdirect ## 

#### The complete DataPower SOMA and AMP configuration utility ####


The dpdirect utility is a compact, maintainable java based utility that gives the DP admin and developer full access to each and every AMP and SOMA command, acccessible as a console, as an ant task, from command line, and via a myriad of scripting languages. 

dpdirect also functions as a SOMA/AMP reference, enabling the user to search for and pretty-print XML samples of SOMA and AMP operations.

If you are looking for the DPDirect Web Services Framework - an extensible, rapid-development DataPower SOA Services framework - you can find it here: https://github.com/SysIntAu/DPDirectServicesFramework

### GET THE DISTRIBUTION: ###

Download the distribution from dist/dpdirect-{version}-deploy.zip or use the following URL:
https://github.com/mqsysadmin/dpdirect/raw/master/dist/dpdirect-1.0.5-deploy.zip

Unzip into a local directory - the package will unzipinto its own 'dpdirect' dir.


### SETTING UP: ###


#### Credentials ('_netrc' file) ####

Credentials should be provided via an optional NETRC file. Windows uses "_netrc" whereas unix uses ".netrc".

A '_netrc' (or '.netrc') file in your home path will pass your machine credentials to DPDirect, so that cleartext credentials need not be exposed in properties files passed in clear-text or via ant scripts.

On windows the "_netrc" file should be created in the users home drive (E.g. "H:/_netrc"). Machine credentials in the _netrc file should be in the format:
```
machine <NameOrIP> login <loginName> password <pw>
```
The file will contain a line for each machine and will look something like thefollowing example:
```
machine DPDevice01 login MYLOGIN password MyPass1
machine DPDevice02 login MYLOGIN password MyPass2
```
Alternatively, but not recommended beyond a sandpit environment, credentials may be provided via the environment properties file in the format:
```
userName=devUser
userPassword=devPassWord
``` 
or in the absence of an environment properties file, provided via command line arguments, eg:
```
> dpdirect hostname=soaserv01 userName=devUser userPassword=devPassWord
```


#### The Properties File ####

Typing 'dpdirect DEV' will start the utility with predefined environment properties - in this example the param 'DEV' coresponds to the name a particular properties file - eg. 'DEV.properties' - in the dpdirect dir. 'dpdirect ENV1' would refer to a properties file named 'ENV1.properties'.

Your properties file might look like this:
```
  domain=DPESB
  hostname=dpappliance01
```
Any properties set here can be changed from the dpdirect console - eg  
```
dpdirect> domain=NEWDOMAIN
```

#### Download the IBM SOMA and AMP Schemas from the DP Appliance ####

The user will have immediate access to dpdirect AMP operations, without access to the vast majority SOMA operations. The available operations will support simple configuration and deployment functions.

To access the full suite of both AMP and SOMA operations, Download AMP SOMA schema files (*.xsd) from the store:// directory on the DP Appliance or Virtual machine, and place in the 'dpdirect/schemas/default' directory.
Use the Web GUI, or perform the following commands from the dpdirect directory:
```
dpdirect DEV operation=get-file name=store://app-mgmt-protocol-v3.xsd > ./schemas/download/app-mgmt-protocol-v3.xsd
dpdirect DEV operation=get-file name=store://xml-mgmt-base.xsd > ./schemas/download/xml-mgmt-base.xsd
dpdirect DEV operation=get-file name=store://xml-mgmt.xsd > ./schemas/download/xml-mgmt.xsd
dpdirect DEV operation=get-file name=store://xml-mgmt-b2b.xsd > ./schemas/download/xml-mgmt-b2b.xsd
dpdirect DEV operation=get-file name=store://xml-mgmt-ops.xsd > ./schemas/download/xml-mgmt-ops.xsd
```
Copy over the downloaded files from the schema/download directory to the schema/default directory, over-writing the current schemas. 


#### Enable the XML Management Interface ####

Via the Web-GUI, type 'XML Management' into the search bar, and select 'XML Management Interface'. Select the 'enable' button, and save config. 
See the IBM notes at http://www.ibm.com/support/knowledgecenter/SS9H2Y_7.5.0/com.ibm.dp.doc/xmi_interfaceservices_enabling.html for more detail on XML Management Interface security.


### CONSOLE QUICK START: ###

To bring up the help page, type one of:
'dpdirect help' or 'dpdirect help console' or 'dpdirect help ant'.

To open the DPDirect console, cd to the dpdirectdir type 'dpdirect [envName]' – where  ‘envName’ refers to a  properties file eg:
```
> dpdirect DEV
```
where the file ‘DEV.properties’ exists in the dpdirectdir.
Alternatively provide 'hostname=somename' param at a minimum. 
```
> dpdirect hostname=somename
```
Adding the ‘dpdirect’ dir to your PATH variable will enable you to run dpdirectfrom any location.


### STUFF TO TRY: ###

The 'find' function will help you construct a command, and demonstrate the target AMP and SOMA XML structure constructed and posted by the utility.

'find get-status' will give you a look at the SOMA structure
```
  DPDirect> find get-status
  # Sample XML:
```
```XML
<man:request domain="?" xmlns:man="http://www.datapower.com/schemas/management">
    <man:get-status class="?"/>
</man:request>
```
Together with a vast array of enumerated objects:
```
# Valid 'class' attribute values:
ActiveUsers, ARPStatus, Battery, ConnectionsAccepted, CPUUsage, CryptoEngineStatus, CurrentSensors, DateTimeStatus, DNSCacheHostStatus, DNSCacheHostStatus2, DNSNameServerStatus, DNSNameServerStatus2, DNSSearchDomainStatus, DNSStaticHostStatus, DocumentCachingSummary, DocumentStatus, DocumentStatusSimpleIndex, DomainCheckpointStatus, DomainsMemoryStatus, DomainStatus, DomainSummary...
```
So the command is 'get-status', and should include mandatory children and attributes. Not all attributes are mandatory.
In this case, it will suffice to enter at the cmd-line: 
```
DPDirect> get-status
```
'get-status' without arguments will display all objects who's status is not currently 0x00000000.
```
DPDirect> get-status
Class: Statistics, OpState: down, AdminState: disabled
 Name: statistics, EventCode: 0x0034000d, ErrorCode: Object is disabled, ConfigState: saved

Class: NFSDynamicMounts, OpState: down, AdminState: disabled
 Name: nfs-dynamic-mounts, EventCode: 0x0034000d, ErrorCode: Object is disabled, ConfigState: saved
```
The 'find' function will return operations on partial entries, for example the cmd:
```
  DPDirect> find flush
```
will return various flush cache operations, while
```
  DPDirect> find quiesce
```
will return details of several quiesce related operations.

#### Custom Operations ####

'set-file' and 'get-file' will take a srcFile={path} and destFile={path} param respectively... this will encode and decode the base64 payload and save to the file system.

'set-dir' will copy a directoy to the device. Custom attributes srcDir (local dir) and destDir (in the format 'local:///path')

'get-dir' will copy a directoy from the device to the local File system. Custom attributes destDir (local dir) and srcDir (in the format 'local:///path')

'tail-log' operation will tail the default log. To exit, hit enter.

'tail-count' is experimental. 'tail-count name={mpgname} class=MultiProtocolGateway' will monitor the traffic count through the named mpg. It will clean up the temproary monitor when you exit (hit enter).


### dpdirect Command Line Usage ###
      
#### Help: ####
'dpdirect help' returns this page.
'dpdirect help console' returns console specific help.
'dpdirect help ant' returns help on using dpdirect as an ant task.
'dpdirect find <regex>' returns sample XML for any operation matching the given regex,     Eg. 'dpdirect find .*[Cc]hange.*', or containing the given word, Eg. 'dpdirect find change' . The sample XML will indicate the attributes and values that may be set for an operation. Be aware that most attributes and values will be optional.

#### Properties file (optional) ####
The FIRST parameter may name a properties file containing global options
The properties must reside next to the dpdirect jar file and take the form <name>.properties 
It is not necessary to include the .properties extension at the cmd-line
Eg. dpdirect DEV ...
      
#### Global/Deployment options (optional) ####
Global Options (must precede any SOMA or AMP operations).
name=value pairs may include:
```
	hostname=<aHostname>	(reqired, cmdLine or properties file)
	userName=<DPusername>	(optional, .netrc or _netrc file, cmdLine, prop file or prompt)
	userPassword=<DPpassword>	(optional, .netrc or _netrc file, cmdLine, prop file or prompt)
	port=<aPort>			(default is '5550')
	domain=<aDomainName>		(default for following operations)
	failOnError=<trueOrFalse>		(default is 'true')
	rollbackOnError=<trueOrFalse>	(default is 'false')
	outputType=<XML|LINES|PARSED>	(default is 'PARSED' Style of output : for the eyeball(PARSED), string manipulation(LINES) or xml parsing(XML))
	verbose=<trueOrFalse>		(default is 'false')
	firmware=<default | 2004 | 3 | 4 | 5>	(major version number, corresponding to a directory in the 'schemas' dir)
	schema=<alt XMLMgmt schema path>	(add schema, alternative schema location)
``` 
         Eg. dpdirect DEV ...
      
#### Command Line vs Console ####

CHOICE: you may hit enter for console mode. Console mode allows one operation at a time.
 
1. Hitting enter at this point (eg 'dpdirect DEV') will enter the dpdirect console mode.
    Console mode allows one operation at a time and retains global settings such as username/userPassword.
    Enter an operation name, eg. 'get-file', with options, eg. 'name=local:///myfile.xml'.
    Use 'find' to discover valid attribute and element values. eg. 'find get-status'.

 OR 

 2. Follow the global options with one or more operations identifiers - 'operation=<op-name>'.
     Stack operations as per follows (this allows reuse of a single session - a faster scripting option):
1. Operations identifier (at least one) - a valid SOMA or AMP operation name
	Followed by...
2. Operation options (optional) - options pertaining to the immediately preceding operation name

     Operations and options may be stacked. Eg.
```
         operation=set-file
         domain=SCRATCH
         set-file@name=<domainName>
         set-file=c:/temp/myfile
         operation=get-status
         class=ActiveUsers
```
                etc....
                    
CONSOLE example: 
```
dpdirect> hostname=soaserv01 userName=EFGRTT userPassword=droWssaP
```
CMDLINE example: 
```
> dpdirect hostname=dp10101 domain=SYSTEST operation=get-status class=ActiveUsers operation=RestartDomainRequest operation=SaveConfig
```      
      Note: an 'Operation' must correspond to a valid SOMA or AMP request element, 
            OR a custom dpdirect operation as follows.
      
#### Custom Operations: ####

'tail-log', 'set-dir' and 'get-dir' are custom operations not catered for in the base schema.
tail-log takes an optional 'name' parameter (name of the log file - default is 'default-log'). 
optional 'filter' and 'filterOut' parameters to filter lines based on whether the given string is contained, and 
an optional 'lines' parameter (starting lines - default is 12).
	Eg.  
```
	tail-log filter=mq lines=30
```

get-status - when issued WITHOUT a 'class=...' identifier, the get-status command will return all 'ObjectStatus' statii that do NOT return an EventCode of '0x00000000', or optionally 
specify 'filter' and/or 'filterOut' parameters to filter lines based on whether the given string is contained.
	Eg. 
```
	get-status filter=MultiProtocolGateway filterOut=0x00000000|disabled
```

get-dir and set-dir take 'srcDir' and 'destDir' params in their native dest and src dir formats.
	
#### Custom Options: ####

srcFile - The value of an option is set to the base64 encoded content of the named file.  It is the source of any base64 payload uploaded to the device, such as set-file and do-import. 

destFile - The datapower response will be base64 decoded and saved to the named path. It is the destination of any base64 payload downloaded from the device, such as get-file and do-export. 

filter and filterOut - will filter tail-log, get-status and get-log output. 

endPoint - Rarely but occasionally a SOMA operation requires posting to the 2004 endpoint. 'endPoint=2004' will alter the XMLManagement end-point. 

Other options are 'AMP', 'SOMA', or a manually constructed relative path, eg '/service/mgmt/amp/1.0'.


### Ant-Task ###

Please refer to the text file 'ant-usage.txt' and the ant xml file 'dptask_example.xml' for ant task guidance.
#### Please Note: there is currently a known ant-task xerces issue running under any ant version above 1.8.0.
Until the issue is resolved, Please copy dpdirect/lib/xercesImpl.jar into your ant-[version]/lib folder. 
Alternatively, run the ant-task with the ant 1.8.0 distribution.

---
title: dpdirect

description: The complete DataPower SOMA and AMP configuration utility

platform: Java

author: Tim Goodwill, mqsysadmin@gmail.com

tags: DatsPower, dpdirect, AMP, SOMA

created:  2011

uploaded: Dec 2016

---
