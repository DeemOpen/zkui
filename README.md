zkui - Zookeeper UI Dashboard
====================
A UI dashboard that allows CRUD operations on Zookeeper.

Requirements
====================
Requires Java 7 to run.

Setup
====================
1. mvn clean install
2. Copy the config.cfg to the folder with the jar file. Modify it to point to the zookeeper instance. Multiple zk instances are coma separated.  eg: server1:2181,server2:2181. First server should always be the leader.
3. Run the jar. ( nohup java -jar zkui-2.0-SNAPSHOT-jar-with-dependencies.jar & )
4. <a href="http://localhost:9090">http://localhost:9090</a> 

Login Info
====================
username: admin, pwd: manager (Admin privileges, CRUD operations supported)
username: appconfig, pwd: appconfig (Readonly privileges, Read operations supported)

You can change this in the config.cfg

Technology Stack
====================
1. Embedded Jetty Server.
2. Freemarker template.
3. H2 DB.
4. Active JDBC.
5. JSON.
6. SLF4J.
7. Zookeeper.
8. Apache Commons File upload.
9. Bootstrap.
10. Jquery.
11. Flyway DB migration.

Features
====================
1. CRUD operation on zookeeper properties.
2. Export properties.
3. Import properties via call back url.
4. Import properties via file upload.
5. History of changes + Path specific history of changes.
6. Search feature.
7. Rest API for accessing Zookeeper properties.
8. Basic Role based authentication.
9. LDAP authentication supported.
10. Root node /zookeeper hidden for safety.
11. ACL supported global level.

Import File Format
====================
#add property
/appconfig/path=property=value
#remove a property
-/path/property

You can either upload a file or specify a http url of the version control system that way all your zookeeper changes will be in version control. 

Export File Format
====================
/appconfig/path=property=value

You can export a file and then use the same format to import.

SOPA/PIPA BLACKLISTED VALUE
====================
All password will be displayed as SOPA/PIPA BLACKLISTED VALUE for a normal user. Admins will be able to view and edit the actual value upon login.
Password will be not shown on search / export / view for normal user.
For a property to be eligible for black listing it should have (PWD / pwd / PASSWORD / password) in the property name.

LDAP
====================
If you want to use LDAP authentication provide the ldap url. This will take precedence over roleSet property file authentication.
ldapUrl=ldap://<ldap_host>:<ldap_port>/dc=mycom,dc=com
If you dont provide this then default roleSet file authentication will be used.

REST call
====================
A lot of times you require your shell scripts to be able to read properties from zookeeper. This can now be achieved with a http call. Password are not exposed via rest api for security reasons. The rest call is a read only operation requiring no authentication.

Eg:
http://localhost:9090/acd/appconfig?propNames=foo&host=myhost.com
This will first lookup the host name under /appconfig/hosts and then find out which path the host point to. Then it will look for the property under that path.

There are 2 additional properties that can be added to give better control.
cluster=cluster1
http://localhost:9090/acd/appconfig?propNames=foo&cluster=cluster1&host=myhost.com
In this case the lookup will happen on lookup path + cluster1.

app=myapp
http://localhost:9090/acd/appconfig?propNames=foo&app=myapp&host=myhost.com
In this case the lookup will happen on lookup path + myapp.

A shell script will call this via
MY_PROPERTY="$(curl -f -s -S -k "http://localhost:9090/acd/appconfig?propNames=foo&host=`hostname -f`" | cut -d '=' -f 2)"
echo $MY_PROPERTY

Standardization
====================
Zookeeper doesnt enforce any order in which properties are stored and retrieved. ZKUI however organizes properties in the following manner for easy lookup.
Each server/box has its hostname listed under /appconfig/hosts and that points to the path where properties reside for that path. So when the lookup for a property occurs over a rest call it first finds the hostname entry under /appconfig/hosts and then looks for that property in the location mentioned.
eg: /appconfig/hosts/myserver.com=/appconfig/dev/app1 
This means that when myserver.com tries to lookup the propery it looks under /appconfig/dev/app1

You can also append app name to make lookup easy.
eg: /appconfig/hosts/myserver.com:testapp=/appconfig/dev/test/app1 
eg: /appconfig/hosts/myserver.com:prodapp=/appconfig/dev/prod/app1

Lookup can be done by grouping of app and cluster. A cluster can have many apps under it. When the bootloader entry looks like this /appconfig/hosts/myserver.com=/appconfig/dev the rest lookup happens on the following paths.
/appconfig/dev/..
/appconfig/dev/hostname..
/appconfig/dev/app..
/appconfig/dev/cluster..
/appconfig/dev/cluster/app..

This standardization is only needed if you choose to use the rest lookup. You can use zkui to update properties in general without worry about this organizing structure.

HTTPS
====================
You can enable https if needed. 
keytool -keystore keystore -alias jetty -genkey -keyalg RSA


Limitations
====================
1. ACLs are fully supported but at a global level.

Screenshots
====================
Basic Role Based Authentication
<br/>
<img src="https://raw.github.com/DeemOpen/zkui/master/images/zkui-0.png"/>
<br/>

Dashboard Console
<br/>
<img src="https://raw.github.com/DeemOpen/zkui/master/images/zkui-1.png"/>
<br/>

CRUD Operations
<br/>
<img src="https://raw.github.com/DeemOpen/zkui/master/images/zkui-2.png"/>
<br/>

Import Feature
<br/>
<img src="https://raw.github.com/DeemOpen/zkui/master/images/zkui-3.png"/>
<br/>

Track History of changes
<br/>
<img src="https://raw.github.com/DeemOpen/zkui/master/images/zkui-4.png"/>
<br/>

Status of Zookeeper Servers
<br/>
<img src="https://raw.github.com/DeemOpen/zkui/master/images/zkui-5.png"/>
<br/>

License & Contribution
====================

ZKUI is released under the Apache 2.0 license. Comments, bugs, pull requests, and other contributions are all welcomed!

Thanks to Jozef Krajčovič for creating the logo which has been used in the project.
https://www.iconfinder.com/iconsets/origami-birds
