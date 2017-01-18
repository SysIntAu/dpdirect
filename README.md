
##Welcome to dpdirect##

####The complete DataPower SOMA and AMP configuration utility



###GET THE DISTRIBUTION:

Download the distribution from dist/dpdirect-{version}-deploy.zip , and unzip into a local directory.

The user will have immediate access to dpdirect AMP operations, without access to the vast majority SOMA operations. The available operations will support simple configuration and deployment functions.

To access the full suite of both AMP and SOMA operations, Download AMP SOMA schema files (*.xsd) from the store:// directory on the DP Appliance or Virtual machine, and place in the 'dpdirect/schemas/default' directory.


###SETTING UP:

Unzip to program files or wherever you like - unzips into its own dir.
You can either put your credentials into a _netrc file in your H:/ dir (recommended), or provide username and userPassword creds in your properties files.
If you add the 'dpdirect' dir to your path, you can run anywhere by typing 'dpdirect DEV' - in this case the param DEV refering to a particular properties file eg. 'DEV.properties' in the dpdirect dir. ’dpdirect ENV1’ would refer to a properties file named ‘ENV1.properties’.

Your dev properties file might look like this:
...
  domain=DPESB
  hostname=dpappliance01
...
Any properties set here can be changed from the console - eg  > domain=NEWDOMAIN


###STUFF TO TRY:

The 'find' function will help you construct a command.
'find filestore' will give you a look at the SOMA structure
...
  DPDirect> find filestore
  # Sample XML:
...
...XML
  <man:request domain="?" xmlns:man="http://www.datapower.com/schemas/management">
      <man:get-filestore annotated="?" layout-only="?" location="?" no-subdirectories="?"/>
  </man:request>
...
...
  # Valid 'location' attribute values:
  local:, store:, export:, cert:, sharedcert:, pubcert:, image:, config:, chkpoints:, logtemp:,
  logstore:, temporary:, tasktemplates:
...
So the command is get-filestore, and should include mandatory children and attributes. Not all attributes are mandatory.
In this case, you will need as a minimum: > get-filestore location=pubcert:

'set-file' and 'get-file' will take a srcFile={path} and destFile={path} param respectively... this will encode and decode the base64 payload and save to the file system.

'set-dir' will copy a directoy to the device. Custom attributes srcDir (local dir) and destDir (in the format 'local:///path')

'get-dir' will copy a directoy from the device to the local File system. Custom attributes destDir (local dir) and srcDir (in the format 'local:///path')

'tail-log' operation will tail the default log. To exit, hit enter.

'get-status' without arguments will display all objects whos status is not currently 0x00000000.

'tail-count' is experimental. 'tail-count name={mpgname} class=MultiProtocolGateway' will monitor the traffic count through the named mpg. It will clean up the temproary monitor when you exit (hit enter).



---
title: dpdirect

description: The complete DataPower SOMA and AMP configuration utility

platform: Java

author: Tim Goodwill, mqsysadmin@gmail.com

tags: DatsPower, dpdirect, AMP, SOMA

created:  2011

uploaded: Dec 2016

---