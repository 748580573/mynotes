# nginx反向代理



## 背景

公司做了安全限制后，内网的服务器就不能随意访问外网了，因此在操作内网服务器使用Yum下载东西的时候，就不能进行了。但是总有那么几台服务器是可以访问外网。因此我们就打算使用这几台机器作为代理机器反向代理内网机器的请求。

## 硬件条件

一台能上网的centos服务器A，一台不能上网的centos服务器B，并且A与B的网络是互通的。

## 安装nginx

在可连接外网的A中安装nginx

**下载nginx**

```shell
# wget http://nginx.org/download/nginx-1.16.0.tar.gz
# tar -zxvf nginx-1.16.0.tar.gz
# cd nginx-1.16.0
```

**编译配置**

```shell
#nginx最后会被安装在/usr/local/nginx下
[root@localhost.com nginx-1.16.0]# ./configure --prefix=/usr/local/nginx \
--user=nginx \
--group=nginx \
--with-pcre \
--with-http_ssl_module \
--with-http_v2_module \
--with-http_realip_module \
--with-http_addition_module \
--with-http_sub_module \
--with-http_dav_module \
--with-http_flv_module \
--with-http_mp4_module \
--with-http_gunzip_module \
--with-http_gzip_static_module \
--with-http_random_index_module \
--with-http_secure_link_module \
--with-http_stub_status_module \
--with-http_auth_request_module \
--with-http_image_filter_module \
--with-http_slice_module \
--with-mail \
--with-threads \
--with-file-aio \
--with-stream \
--with-mail_ssl_module \
--with-stream_ssl_module 
    
[root@localhost.com nginx-1.16.0]#make && make install
[root@localhost.com nginx-1.16.0]# cd /usr/local/nginx/sbin
[root@localhost.com sbin]# ./nginx              # 启动Nginx
[root@localhost.com sbin]# ./nginx -t           # 验证配置文件是正确
[root@localhost.com sbin]# ./nginx -s reload    # 重启Nginx
[root@localhost.com sbin]# ./nginx -s stop      # 停止Nginx
[root@localhost.com sbin]# ./nginx -v            # 查看是否安装成功
nginx version: nginx/1.16.0
[root@localhost.com sbin]# netstat -ntlp | grep nginx # 查看是否启动
tcp   0    0 0.0.0.0:80     0.0.0.0:*     LISTEN    20949/nginx: master
```

**修改nginx的配置文件**

cd /usr/local/nginx/config/nginx.conf

````shell
 server {
        listen 80;
        #listen [::]:80;
        server_name mirrors.yourdomain.com;
        index index.html index.htm index.php default.html default.htm default.php;
        root  /home/wwwroot/html;

        location /ubuntu/ {
            proxy_pass http://mirrors.aliyun.com/ubuntu/ ;
        }

        location /centos/ {
            proxy_pass http://mirrors.aliyun.com/centos/ ;
        }

        location /epel/ {
            proxy_pass http://mirrors.aliyun.com/epel/ ;
        }
    }
````

##  配置yum repo 源

修改无法连接外网的的主机B的repo文件

```ruby
$ cat /etc/yum.repos.d/CentOS-7.repo
```

```shell
[base]
name=CentOS-$releasever - Base - mirrors.yourdomain.com
failovermethod=priority
baseurl=http://mirrors.yourdomain.com/centos/$releasever/os/$basearch/
        http://mirrors.yourdomain.com/centos/$releasever/os/$basearch/
#mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=os
gpgcheck=1
gpgkey=http://mirrors.yourdomain.com/centos/RPM-GPG-KEY-CentOS-7

#released updates 
[updates]
name=CentOS-$releasever - Updates - mirrors.yourdomain.com
failovermethod=priority
baseurl=http://mirrors.yourdomain.com/centos/$releasever/updates/$basearch/
        http://mirrors.yourdomain.com/centos/$releasever/updates/$basearch/
#mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=updates
gpgcheck=1
gpgkey=http://mirrors.yourdomain.com/centos/RPM-GPG-KEY-CentOS-7

#additional packages that may be useful
[extras]
name=CentOS-$releasever - Extras - mirrors.yourdomain.com
failovermethod=priority
baseurl=http://mirrors.yourdomain.com/centos/$releasever/extras/$basearch/
        http://mirrors.yourdomain.com/centos/$releasever/extras/$basearch/
#mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=extras
gpgcheck=1
gpgkey=http://mirrors.yourdomain.com/centos/RPM-GPG-KEY-CentOS-7

#additional packages that extend functionality of existing packages
[centosplus]
name=CentOS-$releasever - Plus - mirrors.yourdomain.com
failovermethod=priority
baseurl=http://mirrors.yourdomain.com/centos/$releasever/centosplus/$basearch/
        http://mirrors.yourdomain.com/centos/$releasever/centosplus/$basearch/
#mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=centosplus
gpgcheck=1
enabled=0
gpgkey=http://mirrors.yourdomain.com/centos/RPM-GPG-KEY-CentOS-7

#contrib - packages by Centos Users
[contrib]
name=CentOS-$releasever - Contrib - mirrors.yourdomain.com
failovermethod=priority
baseurl=http://mirrors.yourdomain.com/centos/$releasever/contrib/$basearch/
        http://mirrors.yourdomain.com/centos/$releasever/contrib/$basearch/
#mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=contrib
gpgcheck=1
enabled=0
gpgkey=http://mirrors.yourdomain.com/centos/RPM-GPG-KEY-CentOS-7
```

## 配置hosts

```ruby
$ cat /etc/hosts

127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
192.168.1.193 mirrors.yourdomain.com
# 确保A 主机IP 和后面的反向代理地址
```

## 测试是否成功

```ruby
$ yum clean all
$ yum makecache
```

