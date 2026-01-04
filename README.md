# milter4j

milter4j is a flexible Sendmail mail filter written in Java. It is intended
to run as a service (daemon) and used by milter-enabled Mail Transfer
Agents (MTA) such as Sendmail, Postfix, etc..  It allows inspection of email
messages **on the fly** and enables immediate actions based on their
content (e.g. rejecting spam, adding or modifying headers, ...).

milter4j acts as a milter API–compliant proxy. When an SMTP connection is
established, the associated MTA sends a *connect* request to milter4j.
In response, milter4j assigns a pooled worker thread, which uses a
dedicated instance of each registered
[MailFilter](https://github.com/jelmd/milter4j/blob/main/src/de/ovgu/cs/milter4j/MailFilter.java)
to process all subsequent milter requests for that specific connection.

The worker collects all data sent via the milter interface, makes it
available to the MailFilter instances, forwards consecutive milter commands
to each filter one by one, collects their responses, assembles the final
milter response, and returns it to the MTA.

This approach requires only a single JVM instance. It saves resources,
enables very fast and efficient mail processing, supports handling large
numbers of emails in parallel, and makes writing custom mail filters
damn easy.

milter4j comes with two built-in **MailFilter** implementations:
- **NullFilter**: A filter that performs no actions. It can be used to easily
  collect basic mail statistics or as a starting point for a new MailFilter,
  with all required methods already implemented.
- **RequestDumper**: A simple mail filter that logs all milter API requests
  received from the mail server at INFO level. It is useful for understanding
  how an MTA interacts with the milter API client and what data is provided.
  It can also serve as a starting point for writing a MailFilter, with all
  milter-relevant methods already in place.

However, by default, no MailFilter is configured. In this case, milter4j simply answers all milter requests with a *continue* action.


# Build
```
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
/bin/ant
```

## Build Documentation
```
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
/bin/ant api
```

# Deployment example
```sh
scp -pr dist mailhost:/opt/milter4j
scp -pr etc mailhost:/opt/milter4j/
# on the mailhost adjust runtime parameters
cp /opt/milter4j/etc/milter4j /etc/init.d/
cp /opt/milter4j/etc/milter.conf /etc/
mkdir /var/log/milter4j
chown smmsp:staff /var/log/milter4j
vi /etc/milter.conf /opt/milter4j/bin/milter4j
# start the server
sudo -c smmsp /etc/init.d/milter4j start
# stop the server
sudo -c smmsp /etc/init.d/milter4j stop
```
For more details wrt. milter4j configuration see https://github.com/jelmd/milter4j/blob/main/src/de/ovgu/cs/milter4j/Configuration.java

To configure Sendmail to use milter4j, add a line like the following to
Sendmail's m4 configuration file (usually `/etc/mail/sendmail.mc`) and then
regenerate the Sendmail configuration file (usually `/etc/mail/sendmail.cf`):
```
INPUT_MAIL_FILTER(`milter4j',`S=inet:44444@localhost,F=T,T=C:1m;S:15s;R:8m;E:9m')
```

# How to add another mail filter


To add a new mail filter, create a Java class that extends the
[MailFilter](https://github.com/jelmd/milter4j/blob/main/src/de/ovgu/cs/milter4j/MailFilter.java),
compile it, and package it along with its dependencies into a JAR file. Copy
this JAR file (and any dependent JARs) into milter4j's `lib` directory
(usually `/opt/milter4j/lib/`).

Finally, add the filter to the milter configuration file
(usually `/etc/mail/milter.conf`) using a line like:

```xml
<filter class="my.simple.SpamMailFilter" conf="..."/>
```
and restart milter4j.


# Java Management Extension (JMX) support

If milter4j is started with JMX support enabled (see `bin/milter4j`), tools such
as `jconsole` can be used to connect to milter4j's embedded JMX server, for
example:

`jconsole -pluginpath lib/milter4j-1.1.4.jar mailhost:12345`

Using JMX, it is possible to monitor and control various aspects of the JVM
running milter4j as well as milter4j itself.

With respect to milter4j, the following operations are available:

- adjust logging levels on the fly
- shut down the service
- reload the configuration (re-read the config file)
- display the current configuration
- retrieve per-worker connection statistics (e.g. the number of connections
  handled by each worker so far)
- retrieve the service start time
- retrieve the service implementation version
- retrieve the current number of workers
- retrieve the total number of connections handled by milter4j so far
- retrieve per-MailFilter statistics, including a breakdown of milter commands
  passed to the MailFilter and the corresponding response counts,
  grouped by response type.
