````shell
#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -----------------------------------------------------------------------------
# Start Script for the CATALINA Server
# -----------------------------------------------------------------------------

# Better OS/400 detection: see Bugzilla 31132
os400=false
case "`uname`" in
OS400*) os400=true;;
esac
# 这一步是为了获取当前操作系统的信息

# resolve links - $0 may be a softlink
#这里$0表示脚本自身
PRG="$0"

while [ -h "$PRG" ] ; do             #-h 用于判断PRG所代表的是值是否是一个软连接
  ls=`ls -ld "$PRG"`                 #只列出当前目录的信息，而非列出该目录的所有信息
  link=`expr "$ls" : '.*-> \(.*\)$'` #提取出RPG软连接指向的地址
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
#上面这一段是为了找出startup.sh脚本的实际所在地址


````



