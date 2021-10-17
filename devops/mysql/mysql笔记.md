### 允许某个网段的iP访问

````shell
grant select,insert,update,create on *.*  to test@'192.168.8.%' identified by '123456'; 

grant all on *.*  to root@'192.168.8.%' identified by 'wuheng'; 

# MySQL5.7
grant all privileges on *.* to 'root'@'%' identified by '123456' with grant option;

# MySQL 8
use mysql;
update user set host='%' where user ='root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%'WITH GRANT OPTION;
FLUSH PRIVILEGES;

flush privileges;  刷新权限表使其设置生效
#上面的%表示一个通配符，代码一个网段。
````

### 添加一个新用户

````shell
1.mysql->create user 'test'@'localhost' identified by '123';

2.mysql->create user 'test'@'192.168.7.22' identified by '123';

3.mysql->create user 'test'@'%' identified by '123';
  #host="localhost"为本地登录用户，host="ip"为ip地址登录，host="%"，为外网ip登录
````
