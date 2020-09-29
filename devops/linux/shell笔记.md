### shell变量

#### 变量的命名规则

shell的变量是弱类型的，甚至不用加修饰符来显性的指出变量。比如下面这样就表明申明了一个变量了。

````shell
my_name="dong wang"
````

shell的变量的命名规则如下：

* 命名只能以字母、数字和下划线，且不能以数字开头
* 变量名字中间不能有空格
* 不能使用标点符号
* 不能使用bash中

#### 使用变量

使用一个定义的变量，只需要在变量名前加$

````shell
my_name="dong wang"
echo $my_name
#{}用于区分$的边界
echo ${my_name}
````



#### shell 用语句给变量赋值

````shell
for file in `ls /usr`;do
    echo "${file}"
done
````

以上将/usr目录下的的文件名循环打印出来

#### 只读变量

使用readyonly修饰符将变量设置为只读，只读的变量不可被修改

````shell
#!/bin/bash
url="123"
readonly url
url="311"
````

运行的结果

````shell
./my.sh: line 4: url: readonly variable
````

#### 删除变量

使用unset修饰符将变量删除，被删除的变量不可再被引用

````shell
#!/bin/bash
url="123"
unset url
echo ${url}
````

该脚本的运行结果为空

#### 变量类型

运行shell时，会同时存在三种变量：

- **局部变量** 局部变量在脚本或命令中定义，仅在当前shell实例中有效，其他shell启动的程序不能访问局部变量。
- **环境变量** 所有的程序，包括shell启动的程序，都能访问环境变量，有些程序需要环境变量来保证其正常运行。必要的时候shell脚本也可以定义环境变量。
- **shell变量** shell变量是由shell程序设置的特殊变量。shell变量中有一部分是环境变量，有一部分是局部变量，这些变量保证了shell的正常运行

### 字符串

#### 字符串基础

字符串在可以说在其他的编程语言中最常用的类型，在shell中自然也不例外（除了数字和字符串也没其他好用的类型了），在shell中可以用单引号表示字符串，也可以用双引号表示字符串。

**单引号**

````shell
#!/bin/bash
name = 'dongwang'
str='hello${name}'
echo ${str}
````

输出结果

````
hello${name}
````

单引号的使用限制：

* 单引号中的任何字符串都会被原样输出，单引号字符串中的变量也会变得无效
* 单引号字符串中不能出现``单独一个``的单引号（使用转义符后也不行），但``可成对``出现，作为字符串的拼接使用

````shell
#单引号字符串中出现单独一个单引号
#!/bin/bash
name='dongwang'
str='hello'${name}'
echo ${str}
````

输出结果为

````shell
./my.sh: line 2: unexpected EOF while looking for matching `''
./my.sh: line 4: syntax error: unexpected end of file
````

````shell
#单引号字符串中出现一对单引号
#!/bin/bash
name='dongwang'
str='hello'${name}''
echo ${str}
````

输出结果为

````
hellodongwang
````

**双引号**

````shell
#!/bin/bash
name="dongwang"
str="hel\"\"lo${name}"
echo ${str}
````

输出结果为

````
hel""lodongwang
````

算引号的优点

* 双引号里可以有变量
* 双引号里可以出现转义字符

#### 拼接字符串

````shell
name="dongwang"
#使用双引号拼接
str1="hello,${name}"
str2="bye!${name}"
echo ${str1},${str2}
#使用单引号拼接
str3='hello,'${name}''
str4='bye!${name}'
echo ${str3},${str4}
````

输出结果为：

````
hello,dongwang,bye!dongwang
hello,dongwang,bye!${name}
````

#### 获取字符串长度

````shell
name="dongwang"
echo ${#name} #输出结果为8
````

#### 提取字符串

````shell
name="dongwang"
echo ${name:0:3} # 输出结果为don，这里0:3表示从第一个字符串开始，截取3个字符
````

#### 查找子字符串

查找字符 **w** 或 **a** 的位置(哪个字母先出现就计算哪个)：

````shell
name="dongwang"
echo `expr index "$name" wa`  # 输出 5
````

**注意：** 以上脚本中 **`** 是反引号，而不是单引号 **'**，不要看错了哦。

### Shell 数组

bash支持一维数组（不支持多维数组），并且没有限定数组的大小。

类似于 C 语言，数组元素的下标由 0 开始编号。获取数组中的元素要利用下标，下标可以是整数或算术表达式，其值应大于或等于 0。

#### 定义数组

在 Shell 中，用括号来表示数组，数组元素用"空格"符号分割开。定义数组的一般形式为：

```
数组名=(值1 值2 ... 值n)
```

例如：

```shell
array_name=(value0 value1 value2 value3)
```

或者

```shell
array_name=(
value0
value1
value2
value3
)
```

还可以单独定义数组的各个分量：

```shell
array_name[0]=value0
array_name[1]=value1
array_name[n]=valuen
```

可以不使用连续的下标，而且下标的范围没有限制。

#### 读取数组

读取数组元素值的一般格式是：

```
${数组名[下标]}
```

例如：

```
valuen=${array_name[n]}
```

使用 **@** 符号可以获取数组中的所有元素，例如：

```
echo ${array_name[@]}
```

#### 获取数组的长度

获取数组长度的方法与获取字符串长度的方法相同，例如：

```shell
# 取得数组元素的个数
length=${#array_name[@]}
# 或者
length=${#array_name[*]}
# 取得数组单个元素的长度
lengthn=${#array_name[n]}
```



### 运算符

#### 普通运算符

| 运算符 | 说明                                          | 举例                          |
| :----- | :-------------------------------------------- | :---------------------------- |
| +      | 加法                                          | `expr $a + $b` 结果为 30。    |
| -      | 减法                                          | `expr $a - $b` 结果为 -10。   |
| *      | 乘法                                          | `expr $a \* $b` 结果为  200。 |
| /      | 除法                                          | `expr $b / $a` 结果为 2。     |
| %      | 取余                                          | `expr $b % $a` 结果为 0。     |
| =      | 赋值                                          | a=$b 将把变量 b 的值赋给 a。  |
| ==     | 相等。用于比较两个数字，相同则返回 true。     | [ $a == $b ] 返回 false。     |
| !=     | 不相等。用于比较两个数字，不相同则返回 true。 | [ $a != $b ] 返回 true。      |



#### 关系运算符

| 运算符 | 说明                                                  | 举例                       |
| :----- | :---------------------------------------------------- | :------------------------- |
| -eq    | 检测两个数是否相等，相等返回 true。                   | [ $a -eq $b ] 返回 false。 |
| -ne    | 检测两个数是否不相等，不相等返回 true。               | [ $a -ne $b ] 返回 true。  |
| -gt    | 检测左边的数是否大于右边的，如果是，则返回 true。     | [ $a -gt $b ] 返回 false。 |
| -lt    | 检测左边的数是否小于右边的，如果是，则返回 true。     | [ $a -lt $b ] 返回 true。  |
| -ge    | 检测左边的数是否大于等于右边的，如果是，则返回 true。 | [ $a -ge $b ] 返回 false。 |
| -le    | 检测左边的数是否小于等于右边的，如果是，则返回 true。 | [ $a -le $b ] 返回 true。  |



##### 布尔运算符

| 运算符 | 说明                                                | 举例                                     |
| :----- | :-------------------------------------------------- | :--------------------------------------- |
| !      | 非运算，表达式为 true 则返回 false，否则返回 true。 | [ ! false ] 返回 true。                  |
| -o     | 或运算，有一个表达式为 true 则返回 true。           | [ $a -lt 20 -o $b -gt 100 ] 返回 true。  |
| -a     | 与运算，两个表达式都为 true 才返回 true。           | [ $a -lt 20 -a $b -gt 100 ] 返回 false。 |

**注意：**复制运算符两边不能有空格。



### 流程控制

#### case

Shell case语句为多选择语句。可以用case语句匹配一个值与一个模式，如果匹配成功，执行相匹配的命令。case语句格式如下

````shell
echo '输入 1 到 4 之间的数字:'
echo '你输入的数字为:'
read aNum
case $aNum in
    1)  echo '你选择了 1'
    ;;
    2)  echo '你选择了 2'
    ;;
    3)  echo '你选择了 3'
    ;;
    4)  echo '你选择了 4'
    ;;
    *)  echo '你没有输入 1 到 4 之间的数字'
    ;;
esac
````

case工作方式如上所示。取值后面必须为单词in，每一模式必须以右括号结束。取值可以为变量或常数。匹配发现取值符合某一模式后，其间所有命令开始执行直至 ;;   , “ ;;”表示break,即执行结束，跳出整个case...esac语句。

取值将检测匹配的每一个模式。一旦模式匹配，则执行完匹配模式相应命令后不再继续其他模式。如果无一匹配模式，使用星号 * 捕获该值，再执行后面的命令。



### shell的一些参数的作用

**-f**

````shell
#检查是否存在这个文件，用例
if[ -f /etc/sysconfig/network ]; then
    ./etc/sysconfig/network
fi
[ -f /usr/bin/grep ]
````

**-z**

````shell
“STRING” 的长度为零则为真[ -z "$myvar" ]
````

**-d**

````shell
如果 filename为目录，则为真[ -e /var/log/syslog ]
````

**-e**

````shell
如果 filename存在，则为真 	[ -e /var/log/syslog ]
````

**-L**

````shell
如果 filename为符号链接，则为真 	[ -L /usr/bin/grep ]
````

**-r**

````shell
如果 filename可读，则为真 	[ -r /var/log/syslog ]
````

**-x **

````shell
如果 filename可执行，则为真 	[ -L /usr/bin/grep ]
````

**-z**

````shell
如果 string长度为零，则为真 	[ -z "$myvar" ]
````

**-n**

````shell
如果 string长度不为零，则为真 	[ -n "$myvar" ]
````



### read 

* 基本读取

````shell
read -p "Enter you name:" name
echo "hello $name,welcome to my program"
exit
````



### exec的用法

先总结一个表：

| exec命令    | 作用                                            |
| ----------- | ----------------------------------------------- |
| exec ls     | 在shell中执行ls，ls结束后不返回原来的shell中了  |
| exec <file  | 将file中的内容作为exec的标准输入                |
| exec >file  | 将file中的内容作为标准写出                      |
| exec 3<file | 将file读入到fd3中                               |
| sort <&3    | fd3中读入的内容被分类                           |
| exec 4>file | 将写入fd4中的内容写入file中                     |
| ls >&4      | Ls将不会有显示，直接写入fd4中了，即上面的file中 |
| exec 5<&4   | 创建fd4的拷贝fd5                                |
| exec 3<&-   | 关闭fd3                                         |







1.shell script:

有两种方法执行shell scripts，一种是新产生一个shell，然后执行相应的shell scripts；一种是在当前shell下执行，不再启用其他shell。

新产生一个shell然后再执行scripts的方法是在scripts文件开头加入以下语句