> 转载自：https://www.yangguo.info/2014/12/13/%E7%BC%96%E8%AF%91%E5%8E%9F%E7%90%86-Javacc%E4%BD%BF%E7%94%A8/

# 编译原理之Javacc使用

> 最近由于需要解析Eterm指令结果，对于这种不遵循标准格式（XML/JSON/HTML）的文本，我又不想大量的堆叠大量的正则表达式，因此借助Javacc来解决我的问题。

### 编译知识

对于码农来说，应该不需要再解释编译这回事了。一般我们将语言分为编译型语言和解释型语言，但是不管是哪种语言，都少不了**词法分析**、**语法分析**。

------

### 词法分析（lexing）

词法分析就是将文本分解成token，token就是具有特殊含义的原子单位，如果语言的保留字、标点符号、数字、字符串，当然包含空白等等，只不过有些如空白会在词法分析时忽略掉。



------

### 语法分析（paring）

它的作用是进行语法检查、并构建由输入的token组成的数据结构（一般是语法分析树、抽象语法树等层次化的数据结构）。语法分析器通常使用一个独立的词法分析器从输入字符流中分离出一个个的token，并将token流作为其输入。

语法分析器的任务主要是确定是否可以以及如何从语法的起始符号推导出输入符号串（输入文本），主要可以通过两种方式完成：

1. 自顶向下分析：根据形式语法规则，在语法分析树的**自顶向下**展开中搜索输入符号串可能的**最左推导**。单词按从左到右的顺序依次使用。它对应**LL分析器**，由于Javacc使用**LL分析器**，所以它是本文默认讨论的分析器。
2. 自底向上分析：语法分析器从现有的输入符号串开始，尝试将其根据给定的形式语法规则进行改写，最终改写为语法的起始符号，它对应**LR分析器**。

此处有必要将最左推导（规范规约）和最右推导说明下，例子如下：

```
文法：
S--->AB
A--->a|t
B---->+CD
C--->a
D---->a
最右推导：
S--->AB---->A+CD--->A+Ca---->A+aa----->a+aa
最左推导：
S---->AB----->aB--->a+CD--->a+aD----->a+aa
```

讲完分析方式和推导方式，那么接下来，我介绍[EBNF](https://zh.wikipedia.org/wiki/扩展巴科斯范式)(扩展巴科斯范式),它是描述语言的**元语法**。

代码，是由[终结符](https://zh.wikipedia.org/wiki/終結符與非终結符)即可视字符、数字、标点符号、空白等字符组成的 计算机源代码。**EBNF**定义了把**终结符**指派到**非终结符**的产生规则。

#### 扩展巴科斯范式的一些规则

```
digit excluding zero = "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
digit                = "0" | digit excluding zero ;

这个产生规则定义了在这个指派的左端的非终结符digit。竖杠表示可供选择，而终结符被引号包围，最后跟着分号作为终止字符，所以digit可以是一个0-9中的任意一个数。
natural number = digit excluding zero , { digit } ;

可以省略或重复的表达式可以通过花括号 { ... } 表示。在这种情况下，字符串 1，2，...，10，...，12345，... 都是正确的表达式。要表示这种情况，于花括号内设立的所有东西可以重复任何次，包括根本不出现。
integer = "0" | [ "-" ] , natural number ;

可选项可以通过方括号 [ ... ] 表示，所以integer是一个零(0)或可能前导可选的负号的一个自然数。
twelve                          = "1" , "2" ;
two hundred one                 = "2" , "0" , "1" ;
three hundred twelve            = "3" , twelve ;
twelve thousand two hundred one = twelve , two hundred one ;

产生规则还可以包括由逗号分隔的一序列终结符或非终结符。
```

### JavaCC

为了简化基于**Java**语言的词法分析器或者语法分析器的开发，Sun公司的开发人员开发了JavaCC(Java Compiler Compiler)。JavaCC是一个基于**Java**语言的分析器的自动生成器。用户只要按照JavaCC的语法规范编写JavaCC的源文件，然后使用JavaCC的编译器编译，就能够生成基于Java语言的某种特定语言的分析器。JavaCC允许我们用类似EBNF的方式来定义语法规则，这样就使得从EBNF语法规则到JavaCC格式的语法规则的转换很容易。这也是我前面讲**EBNF的原因**，并且JavaCC已经成为最受欢迎的Java解析器创建工具，它还自带了的预先设定好的JavaCC语法，以作为应用起点，从而使使用难度进一步降低。

javaCC抛弃了java中的“<<”，“>>”，“>>>”，“<<=”，“=>>”，“>>>=”被javacc从token列表中抛弃，加入了自己的保留字。

| TOKEN          | TOKEN_MGR_DECLS  | SPECIAL_TOKEN | SKIP          |
| :------------- | :--------------- | :------------ | :------------ |
| **PARSER_END** | **PARSER_BEGIN** | **MORE**      | **LOOKAHEAD** |
| **JAVACODE**   | **IGNORE_CASE**  | **EOF**       |               |

### JavaCC语法：

```
javacc_options
"PARSER_BEGIN" "(" <IDENTIFIER> ")"
java_compilation_unit
"PARSER_END" "(" <IDENTIFIER> ")"
( production )*
<EOF>
```

JavaCC语法文件以options列表开始，这个不是必须的，因为这些选项都有默认值。

#### 常见的选项如下：

1. **LOOKAHEAD**：设置在解析过程中面临choice point可以look ahead的token数量。缺省的值是1，这个值越小，解析的速度就越快。这个设置可能会被特定产生式内部的声明给覆盖掉。
2. **CHOICE_AMBIGUITY_CHECK**：这个选项的值是数值型，缺省的值是2。这是在形如”A | B | …”这种选择点出现文法二义性的时候，作为参考的token的数量。例如，A和B有着两个相同的开始token序列，但是从第3个token开始就不一样了（在这个例子中假设这个option的值设置成3），这个时候javacc就会告诉你使用值为3的lookahead就可以消除二义性。如果A和B有着3个共同Token的前缀，那么javacc就是告诉你需lookahead 3个或者更多的token了。增加这个选项的值可以以解析的速度换了更详细的二义性信息。但是，对于复杂的语法，比如java，增加这个数字就会导致增加太多的解析时间。
3. **OTHER_AMBIGUITY_CHECK**：这是一个数值型的选项，默认值是1。这个设置了在其他类型的二义性发生的时候选择lookahead的token数量。例如”(A)*”, “(A)+”, and “(A)?”形式的二义性。这个比上choice checking更加耗时（也就是上面那个选项），所以其默认值是1而不是2。
4. **STATIC**：boolean类型的选项，默认值是true。如果为true，那么所有在parser和token manager中生成的方法和属性，都会被声明成static。这样做会导致只允许一个parser对象被创建，但是可以提高parser的性能。如果static为true，那么在需要多parser对象的时候，需要调用ReInit方法去重新初始化parser。如果static为false，就可以通过new操作符来创建对象了，然后他们可以在多个线程中同时被使用。这个得非常注意，特别是使用javacc来并行处理一些业务逻辑。
5. **DEBUG_LOOKAHEAD**：boolean类型的选项，初始值是false。设置为true的时候，可以显示在paraser执行lookahead 的时候把动作打印出来。
6. **DEBUG_TOKEN_MANAGER**：boolean类型的选项，默认值为false。用于打开token manager的debug日志。当选项为true的时候，token manager就会打印出其所有的动作。这中类型的日志非常多，所有应该在你面对一个文法错误，但是又不知道是怎么回事的时候才被使用。一般来说，你只需要看日志的最后几行应该就能够定位到错误了。
7. **ERROR_REPORTING**：boolean类型的选项，默认值为true。设置为false的时候，会导致解析错误的信息以更简洁的形式提供。只有在为了提高效率的情况下才需要把这个选项设置为false。
8. **JAVA_UNICODE_ESCAPE**：boolean类型的选项，默认值为false。当设置为true的时候，生成的解析器会在把字符串传递给token manager之前处理其中的unicode字符。否则形如\u…的字符串是不会被特殊处理的。如果USER_TOKEN_MANAGER, USER_CHAR_STREAM选项设置为true后，这个选项将会被忽略。
9. **UNICODE_INPUT**：boolean类型的选项，默认值是false。设置为true的时候，生成的解析器将使用一个读取unicode文件的输入流，默认情况下是一个读取ascii文件的输入流。在选项USER_TOKEN_MANAGER，USER_CHAR_STREAM被设置成true的时候，这个选项会被忽略。
10. **IGNORE_CASE**：boolean类型的选项，默认值是false。当设置成true的时候，生成的token manager会忽略输入的文件和token的声明中的大小写。在书写类似如html的语言的时候，这个选项就会非常有用了，当然你也可以定制化IGNORE_CASE。
11. **USER_TOKEN_MANAGER**：boolean类型的选项，默认值是false。默认的情况下，生成的token manager仅仅工作在特定的token范围内。如果这个选项设置为true，生成的解析器会接受来自于任何实现了TokenManager对象。
12. **SUPPORT_CLASS_VISIBILITY_PUBLIC**：boolean类型的选项，默认值是true。在默认的情况下，生成的支撑类（例如Token.java, ParseException.java等等）是具有Public 可见性的对象，如果这个选项被设置成false，那么这些类的可见性将会被设置成package-private级别。
13. **USER_CHAR_STREAM**：boolean类型的选项，默认值是true。在默认的情况下，按照**JAVA_UNICODE_ESCAPE**和**UNICODE_INPUT**选项生成一个字符stream reader。生成的token manager会从这个reader中接受字符串。如果这个选项被设置成true，token manager将从任意一个实现了”CharStream.java”的reader中接受字符串。这个文件可以在解析器生成的文件夹下。在**USER_TOKEN_MANAGER**选项设置成true的时候，这个选项会被忽略。
14. **BUILD_PARSER**：boolean类型的选项，默认值为true。在默认的情况下，javacc会生成一个解析器对象，例如上面提到的MyParser.java文件。在这个选项被设置成false的时候，解析器将不会被生成。一般情况下，如果仅仅需要生成token manager，那个就可以使用这个选项。
15. **BUILD_TOKEN_MANAGER**：boolean类型的选项，默认值是true。在默认情况下，javacc会生成一个token manager文件。如果这个选项设置成false，那么token manager文件就不会被生成了。这样做的一个场景是，你在修改语法文件的时候仅仅修改了语法，而且没有修改任何的文法规则，那么就可以通过这个选项来节约解析器的生成时间了。
16. **TOKEN_MANAGER_USES_PARSER**：boolean类型的选项，默认值是false。当设置为true的时候，生成的token manager会包含一个域指向生成的解析器实例。这样做的好处是，可以在文法分析中使用解析器的一些逻辑。如果static选项设置成true了，那么这个选项将不起作用。
17. **TOKEN_EXTENDS**：字符串类型的选项，默认值是“”，这意味着生成的Token对象将继承自java.lang.Object。这个选项的值可以设置成一个希望Token继承的基类。
18. **TOKEN_FACTORY**：字符串类型的选项，默认值是“”，意味着Token是通过调用Token.newToken()方法生成的。通过这个选项可以指定一个Token factory，factory类需要有一个static的newToken(int ofKind, String image)方法。
19. **SANITY_CHECK**：boolean类型的选项，默认值是true。在解析器生成的过程中，javacc会进行很多的语法和语义检查。诸如左递归、二义性。通过把这个选项设置成false，可以减少这些检查，从而加快生成速度。需要注意的是，即使不做这些检查，上面的现象仍然会导致解析器没有按照你想要的方式去工作。
20. **COMMON_TOKEN_ACTION**：boolean类型的选项，默认值是false。如果这个选项被设置成true。那么在token被扫描进token manager后，每次调用token manager的getNextToken方法后，会触发一个对方法CommonTokenAction的访问。这个函数必须要在TOKEN_MGR_DECLS中定义。CommonTokenAction的签名如下：void CommonTokenAction(Token t)
21. **CACHE_TOKENS**：boolean类型的选项，默认值是false。这个选项设置成true会导致解析器提前拿token。这样做可以提高一些性能，但是交互型的工作就能不能完成了。
22. **OUTPUT_DIRECTORY**：字符串类型的选项，默认值是当前文件夹。这个选项可以控制生成的文件的输出位置。
23. **DEBUG_PARSER**：boolean类型的选项，默认值是false。这个选项用于从parser中获取debug信息。为true的时候，parser会打印很多日志来显示其工作的路径。日志的跟踪也可以通过调用方法disable_tracing()方法来关闭。然后还可以通过enable_tracing()方法来打开tracing。
24. **FORCE_LA_CHECK**: boolean类型的选项，默认值是false。这个选项可以控制javacc对二义性的检查。在默认情况下，二义性检查仅仅在使用LOOKAHEAD 为1的choice point进行。对于明确声明了LOOKAHEAD 或者LOOKAHEAD 的值不等于1的choice point是不做检查的。如果这个选项被设置成true，那么二义性检查将在所有的choice point处执行。

------

#### 语法分析器类

```
PARSER_BEGIN(parser_name)
 . . .
 class parser_name . . . {
   . . .
 }
 . . .
 PARSER_END(parser_name)
```

语法分析器类的定义以标志**PARSER_BEGIN**和**PARSER_END**括起来，而且语法分析器类的名称必须与**PARSER_BEGIN**和**PARSER_END**后的名称相同。

> **特别注意**：javaCC不会详细地检查语法文件的中java代码，java代码就是一个黑盒，所以即使通过了javaCC编译的语法文件生成的解析器还是有可能会通不过java编译器。

生成的解析器会对语法文件中的每一个**非终结符**(java产生式和bnf产生式)生成一个对应的public方法。

#### JavaCC产生式

1. `javacode_production`
2. `regular_expr_production`
3. `bnf_production`
4. `token_manager_decls`

[javacode_production](https://javacc.java.net/doc/javaccgrm.html#prod9)和[bnf_production](https://javacc.java.net/doc/javaccgrm.html#prod11)用来定义parser的生成规则，[regular_expr_production](https://javacc.java.net/doc/javaccgrm.html#prod11)用于定义Token的语法，并且javacc将依据这个信息去生成Token Manager（还会使用在Paraser语法中嵌套的Token）。[token_manager_decls](https://javacc.java.net/doc/javaccgrm.html#prod12)是插入到token manager中的一些申明信息。

------

#### javacode_production

Javacode产生式，是用java代码来书写产生式的一种手段。如果有些规则不好用EBNF来描述，并且上下文相关，就可以使用Javacode产生式，下面的例子是获取流中的一个“)”。

```
JAVACODE
    void skip_to_matching_brace() {
      Token tok;
      int nesting = 1;
      while (true) {
        tok = getToken(1);
        if (tok.kind == LBRACE) nesting++;
        if (tok.kind == RBRACE) {
          nesting--;
          if (nesting == 0) break;
        }
        tok = getNextToken();
      }
    }
```

上面的代码如果用在[选择点](https://javacc.java.net/doc/lookahead.html)的时候可能会出现问题，JavaCC中有4种选择点，分别如下:

1. ( exp1 | exp2 | … )：或
2. ( exp )?：一次或一次都没有
3. ( exp )*：零次或者多次
4. ( exp )+：一次或者多次

有问题的代码:

```
void NT() :
  {}
  {
    skip_to_matching_brace()
  |
    some_other_production()
  }
```

无问题的代码:

```
void NT() :
  {}
  {
    "{" skip_to_matching_brace()
  |
    "(" parameter_list() ")"
  }
```

Javacode产生式用在选择点的时候，也可以在其前面加上语义上的或者语法上的LOOKAHEAD来帮助解析器做出选择。例如:

```
void NT() :
 {}
 {
   LOOKAHEAD( {errorOccurred} ) skip_to_matching_brace()
 |
   "(" parameter_list() ")"
 }
```

Javacode产生式的访问权限是`package private`

------

#### bnf_production

```
java_access_modifier java_return_type java_identifier "(" java_parameter_list ")" ":"

java_block

"{" [expansion_choices](https://javacc.java.net/doc/javaccgrm.html#prod16) "}"
```

BNF产生式是书写javacc语法的标准产生式。每个BNF产生式的左边是一个非终结符，然后BNF产生式会在右边用BNF展开式定义这个非终结符。非终结符的写法就跟声明一个java方法的函数一样。这种写非终结符的方式非常明显，因为每个非终结符都会翻译成一个paraser类中的方法。非终结符的名字就是函数的名字，参数和返回值就是传进解析树的值和传出解析树的值。后面我们还将看见，右边的非终结符使用写起来就像一个函数调用。在解析树上传递的值跟函数的参数和返回值具有相同的范型。BNF产生式的范文权限默认是public。

一个BNF产生式的右边有**两个部分**。首先是一个任意的java块（包括java声明和java code）。这些代码会插入生成的方法的开头。因此，每次在解析过程中使用这个非终结符的时候，这块java代码就会被执行。任何在BNF展开式中间的java代码都可以使用这块的java代码。javacc不会对这块代码做任何处理，仅仅是收集这块的代码。因此，经过javacc处理的代码还是有可能通不过java编译器的。

------

#### regular_expr_production

**[ [lexical_state_list](https://javacc.java.net/doc/javaccgrm.html#newprod1) ]**

**[regexpr_kind](https://javacc.java.net/doc/javaccgrm.html#prod17) [ “[“ “IGNORE_CASE” “]” ] “:”**

**“{“ [regexpr_spec](https://javacc.java.net/doc/javaccgrm.html#prod18) ( “|” [regexpr_spec](https://javacc.java.net/doc/javaccgrm.html#prod18) )\* “}”**

正则表达式产生式用于定于被token manager处理的词法实体。关于token manager是怎么工作的可以参考：[this minitutorial (click here)](https://javacc.java.net/doc/tokenmanager.html)。在这里介绍的是文法实体的语法。[这个教程](https://javacc.java.net/doc/tokenmanager.html)会告诉你这些语法构造是怎样和token manager的实际工作联系起来的。

一个正则表达式产生式以一个文法状态开始，可以通过[lexical state list](https://javacc.java.net/doc/javaccgrm.html#newprod1)指定状态。token manager有一个默认的词法状态叫”DEFAULT”。如果[lexical state list](https://javacc.java.net/doc/javaccgrm.html#newprod1)被省略了，那么DEFAULT状态就会被使用。

状态声明的后面是一个对正则表达产生式类型的描述。[(see below for what this means)](https://javacc.java.net/doc/javaccgrm.html#prod17).

然后是可选的”IGNORE_CASE”。如果出现了这个选项，那么这个正则表达式就是大小写无关的，和前面提到的[IGNORE_CASE](https://javacc.java.net/doc/javaccgrm.html#prod6)选项有同样的作用，区别仅仅是作用于的不同。前面提到的IGNORE_CASE的作用域是全局的。

接下来就是一些对这个正则表达式产生式的词法实体进行更详细描述的正则表达式了。

------

#### token_manager_decls

```
"TOKEN_MGR_DECLS" ":" java_block
```

Token manager的声明以”TOKEN_MGR_DECLS”保留字开始，然后是 “:” ，然后再是一些列的java声明和语句（也就是一个java block）。这些声明和语句会被写进生成的Token Manager，并且可以在[lexical actions](https://javacc.java.net/doc/javaccgrm.html#prod18)中访问。更多详情见 [the minitutorial on the token manager](https://javacc.java.net/doc/tokenmanager.html)。

在一个javacc语法文件中仅仅只能有一个Token Manager声明。

```
lexical_state_list::="<" "*" ">"|
"<" java_identifier ( "," java_identifier )* ">"
```

文法的状态列表描述的是对应的正则表达式产生式生效的范围，可以视为一个作用域。如果使用了“<>”，那么这个正则表达式产生式可以在所用的状态中使用。否则对应的正则表达式产生式仅仅能够在尖括号中指定的状态中使用。

```
regexpr_kind::="TOKEN"|
"SPECIAL_TOKEN"|
"SKIP"|
"MORE"
```

这个定义了正则表达式产生式的类型，包括：

- TOKEN: 这个产生式中的正则表达式描述了tokens的语法，主要定义语法分析阶段用到的非终结符。
  Token Manager会根据这些正则表达式生成[Token](https://javacc.java.net/doc/apiroutines.html)对象并返回给parser。
- SPECIAL_TOKEN: 这产生式中的正则表达式描述了特殊的Token。特殊的Token是在解析过程中没有意义的Token，也就是本BNF产生式忽略的Token。但是，这些Token还是会被传递给parser，并且parser也可以访问他们。访问特殊Token的方式是通过其相邻的Token的specialToken域。特殊Token在处理像注释这种token的时候会非常有用。可以参考这个文档以了解更多关于特殊token的知识。
- SKIP: 这个产生式的规则命中的Token会被Token Manager丢弃掉。
- MORE: 有时候会需要逐步地构建Token。被这种规则命中的Token会存到一个Buffer中，直到遇到下一个Token或者Special_token，然后他们和最后一个Token或者Special_token会连在一起作为一个Token返回给parser。如果一个More后面紧跟了一个SKIP，那么整个Buffer中的内容都会被丢弃掉。

以上就是最核心的部分，还有很多没有讲到。下面再附带一个二元运算计算器的例子，仅供大家参考，文章有部分是借用了网友的文字，特别说明。如果想更详细的理解，请移步[官方文档](https://javacc.java.net/doc/javaccgrm.html)。

**例子：**

```
/**
 * Author: yangguo@outlook.com
 */
options
{
  LOOKAHEAD= 1;

  CHOICE_AMBIGUITY_CHECK = 2;
  OTHER_AMBIGUITY_CHECK = 1;
  STATIC = false;
  DEBUG_PARSER = false;
  DEBUG_LOOKAHEAD = false;
  DEBUG_TOKEN_MANAGER = false;
  ERROR_REPORTING = true;
  JAVA_UNICODE_ESCAPE = false;
  UNICODE_INPUT = false;
  IGNORE_CASE = false;
  USER_TOKEN_MANAGER = false;
  USER_CHAR_STREAM = false;
  BUILD_PARSER = true;
  BUILD_TOKEN_MANAGER = true;
  SANITY_CHECK = true;
  FORCE_LA_CHECK = false;
}

PARSER_BEGIN(Calculator)
package info.yangguo.test1.javacc;
public class Calculator
{
 public static void main(String args[]) throws ParseException
 {
 Calculator parser = new Calculator(System.in);
 while (true)
 {
 parser.parse();
 }
 }
}
PARSER_END(Calculator)
SKIP :
{
  " "
| "\r"
| "\t"
}

TOKEN:
{
 < NUMBER: (<DIGIT>)+ ( "." (<DIGIT>)+ )? >
| < DIGIT: ["0"-"9"] >
| < EOL: "\n" >
}


double parse():
{
 double result;
}
{
 result=binaryOperation() <EOL>
  {
    System.out.println(result);
    return result;
  }
 | <EOF> { System.exit(-1); }
 | <EOL>
}


/**二元运算**/
double binaryOperation():
{
 double result;
 double tmp;
}
{
 result=binaryOperationHighPriority()
 (
 "+" tmp=binaryOperation() { result += tmp; }
 | "-" tmp=binaryOperation() { result -= tmp; }
 )*
 { return result; }
}

/**二元运算中高优先级的部分**/
double binaryOperationHighPriority():
{
 double result;
 double tmp;
}
{
 result=unaryOperation()
 (
 "*" tmp=binaryOperationHighPriority() { result *= tmp; }
   | "/" tmp=binaryOperationHighPriority() { result /= tmp; }
 )*
 { return result; }
}
/**一元运算**/
double unaryOperation():
{
 double result;
}
{
 "-" result=element() { return -result; }
 | result=element() { return result; }
}


double element():
{
 Token token;
 double result;
}
{
 token=<NUMBER> { return Double.parseDouble(token.toString()); }
 | "(" result=binaryOperation() ")" { return result; }
}
```