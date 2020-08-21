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