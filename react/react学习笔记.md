### 创建webpack4.x项目

1.  运行``npm init -y ``快速初始化项目
2. 在项目根目录创建``src``源码目录和dist产品目录
3. 在src目录下创建``index.html``
4. 使用npm安装webpack，运行npm i webpack webpack-cli -D
   + 运行全局``npm i npm -g``
5. 注意：webpack4.x提供了约定大于配置的概念：目的是为了尽量减少配置文件的体积；
   + 默认约定了：
   + 打包的入口是src/index.js
   + 打包的输出文件是dist/main.js

### JSX语法

> 什么是JSX语法：就是符合xml规范的JS语法：（语法格式相对来说，要比HTML严谨很多）

* 1、如何启用jsx语法

* * 安装``babel``插件

  * * 运行``npm i babel-core  babel-loader  babel-plugin-transform-runtime -D``
    * 运行``npm i babel-preset-env  babel-preset-stage-0 -D``

  * 安装能够识别jsx语法的包babel-preset-react

  * * 运行``npm i babel-preset-react  -D``

  * 添加.babelrc配置问题

    ````json
    {
        "presets":['env','stage-0','react'],
        "plugins":["transform-runtime"]
    }
    ````

  * 在webpack.config.js中module选项里添加

    ````json
    {
        test:/\.js|jsx$/,
        use:'babel-loader',
        //千万别王家添加exclude项
        exclude:/node_modules/
    }
    ````

    

  

###  基于class关键字创建组件

````jsx
//如果要使用class定义自建，必须让自己的组件继承React.Component
class 组件名称 extends React.Component{
    //在组件内部，必须有render函数
    render(){
        return <div>这是class创建的组件</div>
    }
}
````

> 使用``class``关键字创建的组件，有自己的私有数据和生命周期函数；
>
> 使用function创建的组件，只有props，没有自己的私有数据和生命周期；

1. 用构造函数创建出来的罪案，叫做“无状态组件”

2. 用class关键字创建出来的组件：叫做“有状态组件”

3. 什么情况下使用有状态组件？什么情况下使用无状态组件

   * 如果一个组件需要有自己的私有数据，则推荐使用class创建的有状态的组件；
   * 如果一个组件不需要有私有数据，则推荐使用无状态组件

   从软件工程的角度来说，使用class创建组件的方式更应该被采纳，class组件的可维护性强更有利于复杂代码之间的交互工作

> 有状态组件和无状态组件之间的本质区别就是：有无state属性和有无生命周期函数！

4. 组件中的``props``和``state/data``之间的区别
   * props中的数据是外界传过来的
   * state/data的数据都是组件私有的；（通过ajax获取的数据，一般是私有数据）
   * props中的数据都是只读的数据，不能重新赋值
   * stata/data中的数据是可读可写的

### react的state

js创建的class继承 React.Component后，会继承react的state，该字段可以用于存储该class组件的数据。state字段与props的区别在于。

* 不能通过props来修改属性的值
* state里的数据是可以修改的

````jsx
class Movie extends React.Component{

    constructor(props) {
        super(props);
        console.log(props)
        this.state = {
            mas:'消息'
        }
    }


    render() {
        return <h1>标题1--{this.state.mas}--{this.props.name}</h1>
    }
}
````

### 给webpack处理css样式表的处理器

1. 运行``cnpm i style-loader css-loader -D`` 安装css样式表相关处理器

2. 在webpack.config.js文件里添加

   ````json
   {
       test:/\.css$/,
       use:['style-loader','css-loader']
   }
   ````

   > 注意：这里use的数组顺序是不可变的，在打包的过程中，css资源会先交给css-loader处理，然后将处理的结果交给style-loader处理，然后将处理的结果交给webpack打包

3. 在需要用到css样式表的地方，导入css样式表

   ````jsx
   //需要在该文件中导入css样式表
   import cssobj from '.css/cmtList.css'
   ````
   
   

> 直接导入的css样式表，是全局生效的，没有作用域。

4. 在react中将css模块化

   在webpack.config.js文件rules中css-loader部分添加@modules，这样在引用css的时候就能以模块化的操作方式来为组件添加css样式

   ````jsx
   {
       test:/\.css$/,
       use:['style-loader','css-loader?modules']
   }
   ````

   然后可以通过obj.attr的方式来引用css样式

   ````jsx
   import React from 'react'         //创建组件、虚拟dom元素就，生命周期
   import CmtItem from "./CmtItem";
   
   import cssobj from '../css/cmtList.css'
   
   console.log(cssobj)
   
   export default class CmtList extends React.Component{
       constructor() {
           super();
           this.state = {
               message : [
                   {id:1,user:'张三',content:'哈哈哈'},
                   {id:2,user:'李四',content:'哈哈哈'},
                   {id:3,user:'王五',content:'哈哈哈'},
                   {id:4,user:'拉拉',content:'哈哈哈'}
               ]
           }
       }
   
   
       render() {
           return <div>
               //通过cssobj.title来引用css样式表中.title样式
               <h1 className={cssobj.title}>这是评论列表组件</h1>
               {this.state.message.map(item => {
                   return <CmtItem key={item.id} {...item}></CmtItem>
               })}
           </div>
       }
   }
   ````

5. 使用``localIdentName``自定义生成的类名格式，可选的参数有：

   * [path]：表示样式表相对于项目根目录的路径

   * [name]：表示样式表文件名称

   * [local]：表示样式的类名定义名称

   * [hash:length]：表示32位的hash值

   * 例子：

     ```json
     {
         test:/\.css$/,
         use:['style-loader','css-loader?modules?localIdentName=[path][name]-[local]-[hash:16]']
     }
     ```

5. 使用:global()，是的css类不会被模块化

   ````css
   /** 被global()包裹的的class不会被模块化，其作用域为全局**/
   :global(.test){
        color:red
   }
   ````

6. 给标签添加多个css类

   ````css
   方法一：
   <p className={'test1' + ' test2'}
   方法二：
   <p className={[test,test2].join(" ")}
   ````

   

### 处理字体

安装``url-loader``和``file-loader``，其中url-loader依赖于file-loader，在webpack.config.js文件中添加相应的module。

```json
{
    test:/\.ttf|woff|woff2|eot|svg/,
    use:'url-loader'
}
```

### 处理自定义样式与第三方样式的模块化问题

在开发过程中，项目中会有自己写的样式与引用的第三方样式。如果想只将自己写的样式进行模块化，第三方样式不进行模块化，这个时候就可以将自己写的样式文件统一名命为``.less``或者``.scss``，然后只对自己写的样式文件启用模块化。

1. 运行``cnpm i sass-loader node-sass -D``安装能够解析scss文件的loader

2. 再webpack配置文件中添加配置

   ```json
   {
       test:/\.scss/,
       use:['style-loader','css-loader?modules','sass-loader']
   },
   {
       test:/\.css$/,
       use:['style-loader','css-loader']
   }
   
   ```

然后第三方样式就可以向下面这样用了

```html
<button className="btn">按钮</button>
```

### 修改react的state中的数据

在react中修改数据的值，推荐使用``this.setState({nmae:'hw'},callBack)``的方式进行数据修改





### 最终的webpack.config.js文件



```js
const path = require('path')
const HtmlWebPackPlugin = require('html-webpack-plugin')

const  htmlPlugin = new HtmlWebPackPlugin({
    template:path.join(__dirname,'/src/index.html'),
    filename:'index.html'
})

const matchJs = {
    test:/\.js|jsx$/,
    use:'babel-loader',
    //千万别王家添加exclude项
    exclude:/node_modules/
}

const matchCss = {
    test:/\.css$/,
    use:['style-loader','css-loader']
}

const matchFont = {
    test:/\.ttf|woff|woff2|eot|svg/,
    use:'url-loader'
}

const matchScss = {
    test:/\.scss/,
    use:['style-loader','css-loader?modules','sass-loader']
}


//向外暴露一个打包的配置对象,因为webpack是基于Node构建的，所以webpack支持所有Node API和语法
module.exports={
    mode:'development',
    //在webpack4.x中有一个很大的特性，就是约定大于配置，默认的打包的入口路径是src -> index.js
    plugins: [
        htmlPlugin
    ],
    module: {
        //所有第三方模块的配置规则
        rules: [
            matchJs,
            matchCss,
            matchFont,
            matchScss
        ]
    },
    resolve: {
        //表示这几个文件的后缀可以省略不写
        extensions: ['.js','.jsx','.json']
    }
}
```