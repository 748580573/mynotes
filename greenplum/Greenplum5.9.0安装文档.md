#  Greenplum5.9.0

### 创建apadmin账户

gp数据库不能以root用户安装，需要另外创建用户

```
# groupadd gpadmin
# useradd gpadmin -g gpadmin
# passwd gpadmin
New password: <changeme>
Retype new password: <changeme>

```

### 查看主机名

```
输入 hostname 查看主机名
我都是bigdata,后面的教程都会以bigdata为主机名
```

### 配置内核参数

```
vim /etc/sysctl.conf
以下面内容覆盖原有内容：
kernel.shmmax = 500000000
kernel.shmmni = 4096
kernel.shmall = 4000000000
kernel.sem = 250 512000 100 2048
kernel.sysrq = 1
kernel.core_uses_pid = 1
kernel.msgmnb = 65536
kernel.msgmax = 65536
kernel.msgmni = 2048
net.ipv4.tcp_syncookies = 1
net.ipv4.conf.default.accept_source_route = 0
net.ipv4.tcp_tw_recycle = 1
net.ipv4.tcp_max_syn_backlog = 4096
net.ipv4.conf.all.arp_filter = 1
net.ipv4.ip_local_port_range = 10000 65535
net.core.netdev_max_backlog = 10000
net.core.rmem_max = 2097152
net.core.wmem_max = 2097152
vm.overcommit_memory = 2
使生效
sysctl -p
```

```
vim /etc/security/limits.conf
以下面的内容进行覆盖
soft nofile 65536
hard nofile 65536
soft nproc 131072
hard nproc 131072
```

###  设置磁盘预读

#### Centos7.2需要加入服务logind

```
echo "RemoveIPC=no" >> /etc/systemd/logind.conf
    service systemd-logind restart
```

####  关闭防火墙 

````
    systemctl stop firewalld.service
    systemctl disable firewalld.service 
    vim /etc/selinux/config 
    改：#SELINUX=enforcing
    SELINUX=disabled
````

#### 重启rebbot



### gp安装

#### 设置host，对bigdata设置ip映射

#### rpm安装包

使用ftp工具将greenplum-db-5.5.0-rhel7-x86_64.rpm上传至服务器任意位置

````
rpm -Uvh greenplum-db-5.5.0-rhel7-x86_64.rpm
#默认安装到/usr/local，授权给gpadmin
chown -R gpadmin /usr/local/greenplum*
chgrp -R gpadmin /usr/local/greenplum*
#设置环境变量
source /usr/local/greenplum-db/greenplum_path.sh
````

#### 创建instance需要的目录

````
mkdir -p /home/gpdata
    mkdir -p /home/gpdata/master
    mkdir -p /home/gpdata/gp1 
    mkdir -p /home/gpdata/gp2 
    mkdir -p /home/gpdata/gp3 
    mkdir -p /home/gpdata/gp4
#修改目录属主
    chown -R gpadmin:gpadmin /home/gpdata
    chown -R gpadmin:gpadmin /home/gpdata/master
    chown -R gpadmin:gpadmin /home/gpdata/gp*
````

#### 切换用户gpadmin

```
su gpadmin
```

进入$开头的bash环境

```
cd
vim .bash_profile
添加以下：
source /usr/local/greenplum-db-5.5.0/greenplum_path.sh
export MASTER_DATA_DIRECTORY=/home/gpdata/master/gpseg-1
export PGPORT=2345
export PGUSER=gpadmin
export PGDATABASE=gpdb
```

vim .bashrc,添加上文同样内容

#### 设置子节点的host

```
vim all_hosts_file
添加一行bigdata（单机版只有一个host）
```

#### 权限互通

```
gpssh-exkeys -f all_hosts_file
```

#### 编辑gp初始化文件

````
vim initgp_config
写入：
SEG_PREFIX=gpseg
PORT_BASE=33000
declare -a DATA_DIRECTORY=(/home/gpdata/gp1 /home/gpdata/gp2 /home/gpdata/gp3 /home/gpdata/gp4) 
MASTER_HOSTNAME=bigdata
MASTER_PORT=2345
MASTER_DIRECTORY=/home/gpdata/master              
DATABASE_NAME=gpdb
````

#### 设置节点服务器

```
vim seg_hosts_file
写入：
bigdata
本例单机，只有bigdata这一台
```

#### 初始化GP

```
gpinitsystem -c initgp_config -h seg_hosts_file
```

### 连接GP

#### psqql登录修改密码

```
    su gpadmin
    psql -p 2345
    修改数据库密码
        alter role gpadmin with password 'bigdata2018';
    \q
```

#### 远程连接配置

```
vim /home/gpdata/master/gpseg-1/postgresql.conf
修改：
#listen_addresses = '*'，去#注释
vim /home/gpdata/master/gpseg-1/pg_hba.conf
添加：
host     all         gpadmin         0.0.0.0/0               md5
```

#### 重新加载配置文件

```
gpstop -u
```

#### 其它命令

````
gpstart #正常启动 
gpstop #正常关闭 
gpstop -M fast #快速关闭 
gpstop –r #重启 
````





