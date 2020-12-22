# Pom参数说明

## distributionManagement

mvn install 会将项目生成的构件安装到本地Maven仓库，mvn deploy 用来将项目生成的构件分发到远程Maven仓库,如果不定义该标签，就无法将本地打的包提交的远程仓库去。本地Maven仓库的构件只能供当前用户使用，在分发到远程Maven仓库之后，所有能访问该仓库的用户都能使用你的构件。

我们需要配置POM的<distributionManagement>来指定Maven分发构件的位置，如下

````xml
<!-- 定义snapshots库和releases库的nexus地址 -->
<distributionManagement>
    <repository>
        <!-- 库的id -->
        <id>nexus-releases</id>
        <!-- 库的url -->
        <url>https://172.17.103.59:8081/nexus/content/repositories/releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>https://172.17.103.59:8081/nexus/content/repositories/snapshots/</url>
    </snapshotRepository>
</distributionManagement>
````

如果是快照版本，执行mvn deploy时会自动发布到快照版本库中。而使用快照版本的模块,在不更改版本号的情况下,直接编译打包时,maven会自动从镜像服务器上下载最新的快照版本。

如果是正式发布版本，那么在执行mvn deploy时会自动发布到正式版本库中，而使用正式版本的模块。在不更改版本号的情况下，编译打包时，如果本地已经存在该版本的模块则使用本地的而不是主动去镜像服务器上下载。需要注意的是，settings.xml中server元素下id的值必须与POM中repository或snapshotRepository下id的值完全一致。将认证信息放到settings下而非POM中，是因为POM往往是它人可见的，而settings.xml是本地的。代码如下：

````xml
<settings>
    ...    
    <servers>
        <server>
            <!--这里的id需要与distributionManagement里的repository中的id保持一直，不然会报权限错误-->
            <id>nexus-releases</id>
            <username>admin</username>
            <password>admin123</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>admin</username>
            <password>admin123</password>
        </server>
    </servers>
    ...
</settings>
````



## mirror

**1.Maven镜像（mirror）的概念、作用**

  mirror相当于一个拦截器，它会拦截maven对remote repository的相关请求，把请求里的remote repository地址，重定向到mirror里配置的地址。

  如果仓库X可以提供仓库Y存储的所有内容，那么就可以认为X是Y的一个镜像。换句话说，任何一个可以从仓库Y获得的构件，都能够从它的镜像中获取。

  **Some reasons to use a mirror are:**

- There is a synchronized mirror on the internet that is geographically closer and faster
- You want to replace a particular repository with your own internal repository which you have greater control over
- You want to run a repository manager to provide a local cache to a mirror and need to use its URL instead.

  例如， 有一个项目，需要在公司和住所都编码，并在项目pom.xml配置了A Maven库。在公司，是电信网络，访问A库很快，所以maven管理依赖和插件都从A库下载；在住所，是网通网络，访问A库很慢，但是访问B库很快。这时，在住所的setting.xml里，只要配置一下<mirrors><mirror>....</mirror></mirrors>，让B库成为A库的mirror，即可不用更改项目pom.xml里对于A库的相关配置。

  如果该镜像仓库需要认证，则配置setting.xml中的<server></server>即可。

**2.镜像配置**

  <mirrorOf></mirrorOf>标签里面放置的是要被镜像的Repository ID。

  为了满足一些复杂的需求，Maven还支持更高级的镜像配置： 

- <mirrorOf>*</mirrorOf> 匹配所有远程仓库。 
- <mirrorOf>repo1,repo2</mirrorOf> 匹配仓库repo1和repo2，使用逗号分隔多个远程仓库。 
- <mirrorOf>external:*,!repo1</miiroOf> 匹配所有远程仓库，repo1除外，使用感叹号将仓库从匹配中排除。 