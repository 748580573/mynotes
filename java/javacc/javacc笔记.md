[官方文档](https://github.com/javacc/javacc/blob/master/docs/documentation/grammar.md)

### 基础命令介绍（以后来写）

#### options

options的选项如下表：

| Option                          | Type      | Default Value     | Description                                                  |
| ------------------------------- | --------- | ----------------- | ------------------------------------------------------------ |
| BUILD_PARSER                    | `boolean` | `true`            | The default action is to generate the parser file (`MyParser.java` in the above example). When set to `false`, the parser file is not generated. Typically, this option is set to `false` when you wish to generate only the token manager and use it without the associated parser. |
| BUILD_TOKEN_MANAGER             | `boolean` | `true`            | The default action is to generate the token manager file (`MyParserTokenManager.java` in the above example). When set to `false` the token manager file is not generated. The only reason to set this option to `false` is to save some time during parser generation when you fix problems in the parser part of the grammar file and leave the lexical specifications untouched. |
| CACHE_TOKENS                    | `boolean` | `false`           | Setting this option to true causes the generated parser to lookahead for extra tokens ahead of time. This facilitates some performance improvements. However, in this case (when the option is `true`), interactive applications may not work since the parser needs to work synchronously with the availability of tokens from the input stream. In such cases, it's best to leave this option at its default value. |
| CHOICE_AMBIGUITY_CHECK          | `integer` | `2`               | This is the number of tokens considered in checking choices of the form `A |
| COMMON_TOKEN_ACTION             | `boolean` | `false`           | When set to `true`, every call to the token manager's method `getNextToken()` (see the description of the JavaCC API) will cause a call to a used defined method `CommonTokenAction` after the token has been scanned in by the token manager. The user must define this method within the `TOKEN_MGR_DECLS` section. The signature of this method is `void CommonTokenAction(Token t)`. |
| DEBUG_LOOKAHEAD                 | `boolean` | `false`           | Setting this option to `true` causes the parser to generate all the tracing information it does when the option `DEBUG_PARSER` is `true`, and in addition, also causes it to generated a trace of actions performed during lookahead operation. |
| DEBUG_PARSER                    | `boolean` | `false`           | This option is used to obtain debugging information from the generated parser. Setting this option to `true` causes the parser to generate a trace of its actions. Tracing may be disabled by calling the method `disable_tracing()` in the generated parser class. Tracing may be subsequently enabled by calling the method `enable_tracing()` in the generated parser class. |
| DEBUG_TOKEN_MANAGER             | `boolean` | `false`           | This option is used to obtain debugging information from the generated token manager. Setting this option to `true` causes the token manager to generate a trace of its actions. This trace is rather large and should only be used when you have a lexical error that has been reported to you and you cannot understand why. Typically, in this situation, you can determine the problem by looking at the last few lines of this trace. |
| ERROR_REPORTING                 | `boolean` | `true`            | Setting it to `false` causes errors due to parse errors to be reported in somewhat less detail. The only reason to set this option to `false` is to improve performance. |
| FORCE_LA_CHECK                  | `boolean` | `false`           | This option setting controls lookahead ambiguity checking performed by JavaCC. By default (when this option is `false`), lookahead ambiguity checking is performed for all choice points where the default lookahead of `1` is used. Lookahead ambiguity checking is not performed at choice points where there is an explicit lookahead specification, or if the option `LOOKAHEAD` is set to something other than `1`. Setting this option to `true` performs lookahead ambiguity checking at all choice points regardless of the lookahead specifications in the grammar file. |
| IGNORE_CASE                     | `boolean` | `false`           | Setting this option to `true` causes the generated token manager to ignore case in the token specifications and the input files. This is useful for writing grammars for languages such as HTML. It is also possible to localize the effect of `IGNORE_CASE` by using an alternate mechanism described later. |
| JAVA_UNICODE_ESCAPE             | `boolean` | `false`           | When set to `true`, the generated parser uses an input stream object that processes Java Unicode escapes `(\u...)` before sending characters to the token manager. By default, Java Unicode escapes are not processed. This option is ignored if either of options `USER_TOKEN_MANAGER`, `USER_CHAR_STREAM` is set to `true`. |
| LOOKAHEAD                       | `integer` | `1`               | The number of tokens to look ahead before making a decision at a choice point during parsing. The smaller this number, the faster the parser. This number may be overridden for specific productions within the grammar as described later. See the description of the lookahead algorithm for complete details on how lookahead works. |
| OTHER_AMBIGUITY_CHECK           | `integer` | `1`               | This is the number of tokens considered in checking all other kinds of choices (i.e. of the forms `(A)*`", `(A)+`, and `(A)?`) for ambiguity. This takes more time to do than the choice checking, and hence the default value is set to `1` rather than `2`. |
| OUTPUT_DIRECTORY                | `String`  | Current directory | This controls where output files are generated.              |
| SANITY_CHECK                    | `boolean` | `true`            | JavaCC performs many syntactic and semantic checks on the grammar file during parser generation. Some checks such as detection of left recursion, detection of ambiguity, and bad usage of empty expansions may be suppressed for faster parser generation by setting this option to `false`. Note that the presence of these errors (even if they are not detected and reported by setting this option to `false`) can cause unexpected behavior from the generated parser. |
| STATIC                          | `boolean` | `true`            | If true, all methods and class variables are specified as static in the generated parser and token manager. This allows only one parser object to be present, but it improves the performance of the parser. To perform multiple parses during one run of your Java program, you will have to call the `ReInit()` method to reinitialize your parser if it is static. If the parser is non-static, you may use the `new` operator to construct as many parsers as you wish. These can all be used simultaneously from different threads. |
| SUPPORT_CLASS_VISIBILITY_PUBLIC | `boolean` | `true`            | The default action is to generate support classes (such as `Token.java`, `ParseException.java` etc) with `public` visibility. If set to `false`, the classes will be generated with package `private` visibility. |
| TOKEN_EXTENDS                   | `String`  | `""`              | The default option means that the generated `Token` class will extend `java.lang.Object`. This option may be set to the name of a class that will be used as the base class for the generated `Token` class. |
| TOKEN_FACTORY                   | `String`  | `""`              | The default option means that `Tokens` will be created by calling `Token.newToken()`. If set the option names a `Token` factory class containing a public static `Token newToken(int ofKind, String image)` method. |
| TOKEN_MANAGER_USES_PARSER       | `boolean` | `false`           | When set to `true`, the generated token manager will include a field called parser that references the instantiating parser instance (of type `MyParser` in the above example). The main reason for having a parser in a token manager is using some of its logic in lexical actions. This option has no effect if the `STATIC` option is set to `true`. |
| UNICODE_INPUT                   | `boolean` | `false`           | When set to `true`, the generated parser uses uses an input stream object that reads Unicode files. By default, ASCII files are assumed. This option is ignored if either of options `USER_TOKEN_MANAGER`, `USER_CHAR_STREAM` is set to `true`. |
| USER_CHAR_STREAM                | `boolean` | `false`           | The default action is to generate a character stream reader as specified by the options `JAVA_UNICODE_ESCAPE` and `UNICODE_INPUT`. The generated token manager receives characters from this stream reader. If this option is set to `true`, then the token manager is generated to read characters from any character stream reader of type `CharStream.java`. This file is generated into the generated parser directory. This option is ignored if `USER_TOKEN_MANAGER` is set to `true`. |
| USER_TOKEN_MANAGER              | `boolean` | `false`           | The default action is to generate a token manager that works on the specified grammar tokens. If this option is set to `true`, then the parser is generated to accept tokens from any token manager of type `TokenManager` - this interface is generated into the generated parser directory. |



#### TOKEN

这个产生式中的正则表达式描述了tokens的语法，主要定义语法分析阶段用到的非终结符。
Token Manager会根据这些正则表达式生成[Token](https://javacc.java.net/doc/apiroutines.html)对象并返回给parser。

#### SPECIAL_TOKEN

这产生式中的正则表达式描述了特殊的Token。特殊的Token是在解析过程中没有意义的Token，也就是本BNF产生式忽略的Token。但是，这些Token还是会被传递给parser，并且parser也可以访问他们。访问特殊Token的方式是通过其相邻的Token的specialToken域。特殊Token在处理像注释这种token的时候会非常有用。可以参考这个文档以了解更多关于特殊token的知识。

#### SKIP

这个产生式的规则命中的Token会被Token Manager丢弃掉。

#### MORE

有时候会需要逐步地构建Token。被这种规则命中的Token会存到一个Buffer中，直到遇到下一个Token或者Special_token，然后他们和最后一个Token或者Special_token会连在一起作为一个Token返回给parser。如果一个More后面紧跟了一个SKIP，那么整个Buffer中的内容都会被丢弃掉。

#### LOOKAHEAD

The number of tokens to look ahead before making a decision at a choice point during parsing. The smaller this number, the faster the parser. This number may be overridden for specific productions within the grammar as described later. See the description of the lookahead algorithm for complete details on how lookahead works.

#### 状态迁移



### 用例

#### 跳过注释块

````shell
MORE : {<"/*"> : IN_BLOCK_COMMENT}
<IN_BLOCK_COMMENT> MORE : {<~[]>}
<IN_BLOCK_COMMENT> SKIP : {<"*/"> : DEFAULT}
````

