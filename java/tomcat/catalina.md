```shell
cygwin=false
darwin=false
os400=false
hpux=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
HP-UX*) hpux=true;;
esac
# 上面一段是判断当前脚本的的执行环境
# cygwin--wondows类UNIX环境，Darwin--苹果环境，OS400--IBM的AIX，HP-UX--惠普9000系列服务器的操作系统

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

#上面一段代码是为了获取catalina.sh文件实际地址，如果catalina.sh文件本身是一个软连接，就把软连接的实际地址找出来

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set CATALINA_HOME if not already set
[ -z "$CATALINA_HOME" ] && CATALINA_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

# Copy CATALINA_BASE from CATALINA_HOME if not already set
[ -z "$CATALINA_BASE" ] && CATALINA_BASE="$CATALINA_HOME"

#上面一段代码。 如果$CATALINA_HOME为空，则给CATALINA_HOME赋值。
# cd "$PRGDIR/.." >/dev/null 是将cd命令产生的信息清楚掉
```





````shell
#catalina.sh 脚本最终生成的脚本为
nohup /home/F/java_plug/jdk1.8.0_191/bin/java 	-Djava.util.logging.config.file=/home/test/apache-tomcat-8.5.37/conf/logging.properties       -Djava.util.logging.config.file=/home/test/apache-tomcat-8.5.37/conf/logging.properties  -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Djdk.tls.ephemeralDHKeySize=2048  -Djava.protocol.handler.pkgs=org.apache.catalina.webresources -Dorg.apache.catalina.security.SecurityListener.UMASK=0027 -Dignore.endorsed.dirs="" -classpath "/home/test/apache-tomcat-8.5.37/bin/bootstrap.jar:/home/test/apache-tomcat-8.5.37/bin/tomcat-juli.jar" -Dcatalina.base="/home/test/apache-tomcat-8.5.37" -Dcatalina.home="/home/test/apache-tomcat-8.5.37"  -Djava.io.tmpdir="/home/test/apache-tomcat-8.5.37/temp" org.apache.catalina.startup.Bootstrap start >> /home/test/apache-tomcat-8.5.37/logs/catalina.out 2>&1 &
````

