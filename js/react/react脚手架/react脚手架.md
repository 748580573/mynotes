# react脚手架

## 简介

**npm:** node包管理器，使用npm去下载包,类似于java的maven

**npx:** 执行二进制文件，从5.2版本增加了npx命令

## 全局安装yarn

> npm   i  -g  yarn

## 查看yarn版本

> yarn -v

````shell
PS C:\Users\O_o\Desktop> yarn -v
1.22.19
````

## 初始化项目

> yarn init -y

````shell
PS F:\tmp\create_react> yarn init -y
yarn init v1.22.19
warning The yes flag has been set. This will automatically answer yes to all questions, which may have security implications.
success Saved package.json
Done in 0.07s.
````

## 本地安装依赖

>  yarn add -D create-react-app

````shell
PS F:\tmp\create_react> yarn add -D create-react-app
yarn add v1.22.19
info No lockfile found.
[1/4] Resolving packages...
warning create-react-app > tar-pack > tar@2.2.2: This version of tar is no longer supported, and will not receive security updates. Please upgrade asap.
[2/4] Fetching packages...
[3/4] Linking dependencies...
[4/4] Building fresh packages...

success Saved lockfile.
success Saved 54 new dependencies.
info Direct dependencies
└─ create-react-app@5.0.1
info All dependencies
├─ ansi-styles@4.3.0
├─ balanced-match@1.0.2
├─ block-stream@0.0.9
├─ brace-expansion@1.1.11
├─ buffer-from@0.1.2
├─ builtins@1.0.3
├─ chalk@4.1.2
├─ color-convert@2.0.1
├─ color-name@1.1.4
├─ commander@4.1.1
├─ concat-map@0.0.1
├─ create-react-app@5.0.1
├─ cross-spawn@7.0.3
├─ debug@2.6.9
├─ duplexer2@0.0.2
├─ envinfo@7.11.0
├─ fs-extra@10.1.0
├─ fs.realpath@1.0.0
├─ fstream-ignore@1.0.5
├─ fstream@1.0.12
├─ graceful-fs@4.2.11
├─ has-flag@4.0.0
├─ hyperquest@2.1.3
├─ inflight@1.0.6
├─ isexe@2.0.0
├─ jsonfile@6.1.0
├─ kleur@3.0.3
├─ lru-cache@6.0.0
├─ minimatch@3.1.2
├─ minimist@1.2.8
├─ mkdirp@0.5.6
├─ ms@2.0.0
├─ path-is-absolute@1.0.1
├─ path-key@3.1.1
├─ process-nextick-args@2.0.1
├─ prompts@2.4.2
├─ readable-stream@2.3.8
├─ rimraf@2.7.1
├─ safe-buffer@5.1.2
├─ semver@7.5.4
├─ shebang-command@2.0.0
├─ shebang-regex@3.0.0
├─ sisteransi@1.0.5
├─ supports-color@7.2.0
├─ tar-pack@3.4.1
├─ tar@2.2.2
├─ through2@0.6.5
├─ tmp@0.2.1
├─ uid-number@0.0.6
├─ util-deprecate@1.0.2
├─ validate-npm-package-name@3.0.0
├─ which@2.0.2
├─ xtend@4.0.2
└─ yallist@4.0.0
Done in 3.38s.
````

## 创建项目

> npx create-react-app react-demo1

````shell
PS F:\tmp\create_react> npx create-react-app react-demo1

Creating a new React app in F:\tmp\create_react\react-demo1.

Installing packages. This might take a couple of minutes.
Installing react, react-dom, and react-scripts with cra-template...


added 1463 packages in 1m

Initialized a git repository.

Installing template dependencies using npm...

added 69 packages, and changed 1 package in 8s
Removing template package using npm...


removed 1 package in 2m

Created git commit.

Success! Created react-demo1 at F:\tmp\create_react\react-demo1
Inside that directory, you can run several commands:

  npm start
    Starts the development server.

  npm run build
    Bundles the app into static files for production.

  npm test
    Starts the test runner.

  npm run eject
    Removes this tool and copies build dependencies, configuration files
    and scripts into the app directory. If you do this, you can’t go back!

We suggest that you begin by typing:

  cd react-demo1
  npm start

Happy hacking!
PS F:\tmp\create_react> npx create-react-app react-demo1
The directory react-demo1 contains files that could conflict:

  node_modules/
  package-lock.json
  package.json
  public/
  src/

Either try using a new directory name, or remove the files listed above.
````

## 查看创建项目的版本

> npx create-react-app --version

````shell
PS F:\tmp\create_react> npx create-react-app --version
5.0.1
````

## 启动项目

> cd .\react-demo1\
>
> yarn start

````shell
PS F:\tmp\create_react> cd .\react-demo1\
PS F:\tmp\create_react\react-demo1>
PS F:\tmp\create_react\react-demo1> yarn start
yarn run v1.22.19
$ react-scripts start
(node:9760) [DEP_WEBPACK_DEV_SERVER_ON_AFTER_SETUP_MIDDLEWARE] DeprecationWarning: 'onAfterSetupMiddleware' option is deprecated. Please use the 'setupMiddlewares' option.
(Use `node --trace-deprecation ...` to show where the warning was created)
(node:9760) [DEP_WEBPACK_DEV_SERVER_ON_BEFORE_SETUP_MIDDLEWARE] DeprecationWarning: 'onBeforeSetupMiddleware' option is deprecated. Please use the 'setupMiddlewares' option.
Starting the development server...

One of your dependencies, babel-preset-react-app, is importing the
"@babel/plugin-proposal-private-property-in-object" package without
declaring it in its dependencies. This is currently working because
"@babel/plugin-proposal-private-property-in-object" is already in your
node_modules folder for unrelated reasons, but it may break at any time.

babel-preset-react-app is part of the create-react-app project, which
is not maintianed anymore. It is thus unlikely that this bug will
ever be fixed. Add "@babel/plugin-proposal-private-property-in-object" to
your devDependencies to work around this error. This will make this message
go away.
Compiled successfully!

You can now view react-demo1 in the browser.

  Local:            http://localhost:3000
  On Your Network:  http://192.168.6.1:3000

Note that the development build is not optimized.
To create a production build, use npm run build.

webpack compiled successfully
````

