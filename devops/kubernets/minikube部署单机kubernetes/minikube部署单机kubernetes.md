# minikube部署单机kubernetes

## 下载Minikube

````java
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
````

## 查看版本

> minikube version

````shell
[root@localhost home]# minikube version
minikube version: v1.31.2
commit: fd7ecd9c4599bef9f04c0986c4a0187f98a4396e
````

##  安装kubectl

> minikube kubectl

````java
[root@localhost home]# minikube kubectl
    > kubectl.sha256:  64 B / 64 B [-------------------------] 100.00% ? p/s 0s
    > kubectl:  46.98 MiB / 46.98 MiB [----------] 100.00% 17.92 KiB p/s 44m45s
kubectl controls the Kubernetes cluster manager.

 Find more information at: https://kubernetes.io/docs/reference/kubectl/

Basic Commands (Beginner):
  create          Create a resource from a file or from stdin
  expose          Take a replication controller, service, deployment or pod and expose it as a new
Kubernetes service
  run             Run a particular image on the cluster
  set             Set specific features on objects

Basic Commands (Intermediate):
  explain         Get documentation for a resource
  get             Display one or many resources
  edit            Edit a resource on the server
  delete          Delete resources by file names, stdin, resources and names, or by resources and
label selector

Deploy Commands:
  rollout         Manage the rollout of a resource
  scale           Set a new size for a deployment, replica set, or replication controller
  autoscale       Auto-scale a deployment, replica set, stateful set, or replication controller

Cluster Management Commands:
  certificate     Modify certificate resources.
  cluster-info    Display cluster information
  top             Display resource (CPU/memory) usage
  cordon          Mark node as unschedulable
  uncordon        Mark node as schedulable
  drain           Drain node in preparation for maintenance
  taint           Update the taints on one or more nodes

Troubleshooting and Debugging Commands:
  describe        Show details of a specific resource or group of resources
  logs            Print the logs for a container in a pod
  attach          Attach to a running container
  exec            Execute a command in a container
  port-forward    Forward one or more local ports to a pod
  proxy           Run a proxy to the Kubernetes API server
  cp              Copy files and directories to and from containers
  auth            Inspect authorization
  debug           Create debugging sessions for troubleshooting workloads and nodes
  events          List events

Advanced Commands:
  diff            Diff the live version against a would-be applied version
  apply           Apply a configuration to a resource by file name or stdin
  patch           Update fields of a resource
  replace         Replace a resource by file name or stdin
  wait            Experimental: Wait for a specific condition on one or many resources
  kustomize       Build a kustomization target from a directory or URL

Settings Commands:
  label           Update the labels on a resource
  annotate        Update the annotations on a resource
  completion      Output shell completion code for the specified shell (bash, zsh, fish, or
powershell)

Other Commands:
  api-resources   Print the supported API resources on the server
  api-versions    Print the supported API versions on the server, in the form of "group/version"
  config          Modify kubeconfig files
  plugin          Provides utilities for interacting with plugins
  version         Print the client and server version information

Usage:
  kubectl [flags] [options]

Use "kubectl <command> --help" for more information about a given command.
Use "kubectl options" for a list of global command-line options (applies to all commands).

````

## 安装yum-utils

>  yum install -y yum-utils

````java
Loaded plugins: fastestmirror, langpacks
Loading mirror speeds from cached hostfile
 * base: mirrors.cqu.edu.cn
 * extras: mirrors.cqu.edu.cn
 * updates: mirrors.cqu.edu.cn
base                                                                                    | 3.6 kB  00:00:00     
extras                                                                                  | 2.9 kB  00:00:00     
updates                                                                                 | 2.9 kB  00:00:00     
Resolving Dependencies
--> Running transaction check
---> Package yum-utils.noarch 0:1.1.31-52.el7 will be updated
---> Package yum-utils.noarch 0:1.1.31-54.el7_8 will be an update
--> Finished Dependency Resolution

Dependencies Resolved

===============================================================================================================
 Package                   Arch                   Version                           Repository            Size
===============================================================================================================
Updating:
 yum-utils                 noarch                 1.1.31-54.el7_8                   base                 122 k

Transaction Summary
===============================================================================================================
Upgrade  1 Package

Total download size: 122 k
Downloading packages:
No Presto metadata available for base
warning: /var/cache/yum/x86_64/7/base/packages/yum-utils-1.1.31-54.el7_8.noarch.rpm: Header V3 RSA/SHA256 Signature, key ID f4a80eb5: NOKEY
Public key for yum-utils-1.1.31-54.el7_8.noarch.rpm is not installed
yum-utils-1.1.31-54.el7_8.noarch.rpm                                                    | 122 kB  00:00:00     
Retrieving key from file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
Importing GPG key 0xF4A80EB5:
 Userid     : "CentOS-7 Key (CentOS 7 Official Signing Key) <security@centos.org>"
 Fingerprint: 6341 ab27 53d7 8a78 a7c2 7bb1 24c6 a8a7 f4a8 0eb5
 Package    : centos-release-7-7.1908.0.el7.centos.x86_64 (@anaconda)
 From       : /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
Running transaction check
Running transaction test
Transaction test succeeded
Running transaction
  Updating   : yum-utils-1.1.31-54.el7_8.noarch                                                            1/2 
  Cleanup    : yum-utils-1.1.31-52.el7.noarch                                                              2/2 
  Verifying  : yum-utils-1.1.31-54.el7_8.noarch                                                            1/2 
  Verifying  : yum-utils-1.1.31-52.el7.noarch                                                              2/2 

Updated:
  yum-utils.noarch 0:1.1.31-54.el7_8                                                                           

Complete!
[root@localhost home]# yum-config-manager \
>   --add-repo\
>   https://download.docker.com/linux/centos/docker-ce.repo
Loaded plugins: fastestmirror, langpacks
adding repo from: https://download.docker.com/linux/centos/docker-ce.repo
grabbing file https://download.docker.com/linux/centos/docker-ce.repo to /etc/yum.repos.d/docker-ce.repo
repo saved to /etc/yum.repos.d/docker-ce.repo
````

## 安装docker

>  sudo yum-config-manager \
>
> ​    --add-repo \
>
> ​    https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
>
> yum install docker-ce docker-ce-cli containerd.io docker-compose-plugin

````shell
[root@localhost home]# yum-config-manager \
   --add-repo \
   https://download.docker.com/linux/centos/docker-ce.repo
[root@localhost home]# yum install docker-ce docker-ce-cli containerd.io docker-compose-plugin
Loaded plugins: fastestmirror, langpacks
Loading mirror speeds from cached hostfile
 * base: mirrors.cqu.edu.cn
 * extras: mirrors.cqu.edu.cn
 * updates: mirrors.cqu.edu.cn
docker-ce-stable                                                                                                                  | 3.5 kB  00:00:00     
(1/2): docker-ce-stable/7/x86_64/updateinfo                                                                                       |   55 B  00:00:00     
(2/2): docker-ce-stable/7/x86_64/primary_db                                                                                       | 117 kB  00:00:00     
Resolving Dependencies
--> Running transaction check
---> Package containerd.io.x86_64 0:1.6.22-3.1.el7 will be installed
--> Processing Dependency: container-selinux >= 2:2.74 for package: containerd.io-1.6.22-3.1.el7.x86_64
---> Package docker-ce.x86_64 3:24.0.6-1.el7 will be installed
--> Processing Dependency: docker-ce-rootless-extras for package: 3:docker-ce-24.0.6-1.el7.x86_64
---> Package docker-ce-cli.x86_64 1:24.0.6-1.el7 will be installed
--> Processing Dependency: docker-buildx-plugin for package: 1:docker-ce-cli-24.0.6-1.el7.x86_64
---> Package docker-compose-plugin.x86_64 0:2.21.0-1.el7 will be installed
--> Running transaction check
---> Package container-selinux.noarch 2:2.119.2-1.911c772.el7_8 will be installed
---> Package docker-buildx-plugin.x86_64 0:0.11.2-1.el7 will be installed
---> Package docker-ce-rootless-extras.x86_64 0:24.0.6-1.el7 will be installed
--> Processing Dependency: fuse-overlayfs >= 0.7 for package: docker-ce-rootless-extras-24.0.6-1.el7.x86_64
--> Processing Dependency: slirp4netns >= 0.4 for package: docker-ce-rootless-extras-24.0.6-1.el7.x86_64
--> Running transaction check
---> Package fuse-overlayfs.x86_64 0:0.7.2-6.el7_8 will be installed
--> Processing Dependency: libfuse3.so.3(FUSE_3.2)(64bit) for package: fuse-overlayfs-0.7.2-6.el7_8.x86_64
--> Processing Dependency: libfuse3.so.3(FUSE_3.0)(64bit) for package: fuse-overlayfs-0.7.2-6.el7_8.x86_64
--> Processing Dependency: libfuse3.so.3()(64bit) for package: fuse-overlayfs-0.7.2-6.el7_8.x86_64
---> Package slirp4netns.x86_64 0:0.4.3-4.el7_8 will be installed
--> Running transaction check
---> Package fuse3-libs.x86_64 0:3.6.1-4.el7 will be installed
--> Finished Dependency Resolution

Dependencies Resolved

=========================================================================================================================================================
 Package                                    Arch                    Version                                      Repository                         Size
=========================================================================================================================================================
Installing:
 containerd.io                              x86_64                  1.6.22-3.1.el7                               docker-ce-stable                   34 M
 docker-ce                                  x86_64                  3:24.0.6-1.el7                               docker-ce-stable                   24 M
 docker-ce-cli                              x86_64                  1:24.0.6-1.el7                               docker-ce-stable                   13 M
 docker-compose-plugin                      x86_64                  2.21.0-1.el7                                 docker-ce-stable                   13 M
Installing for dependencies:
 container-selinux                          noarch                  2:2.119.2-1.911c772.el7_8                    extras                             40 k
 docker-buildx-plugin                       x86_64                  0.11.2-1.el7                                 docker-ce-stable                   13 M
 docker-ce-rootless-extras                  x86_64                  24.0.6-1.el7                                 docker-ce-stable                  9.1 M
 fuse-overlayfs                             x86_64                  0.7.2-6.el7_8                                extras                             54 k
 fuse3-libs                                 x86_64                  3.6.1-4.el7                                  extras                             82 k
 slirp4netns                                x86_64                  0.4.3-4.el7_8                                extras                             81 k

Transaction Summary
=========================================================================================================================================================
Install  4 Packages (+6 Dependent packages)

Total download size: 107 M
Installed size: 377 M
Is this ok [y/d/N]: y
Downloading packages:
(1/10): container-selinux-2.119.2-1.911c772.el7_8.noarch.rpm                                                                      |  40 kB  00:00:00     
warning: /var/cache/yum/x86_64/7/docker-ce-stable/packages/docker-buildx-plugin-0.11.2-1.el7.x86_64.rpm: Header V4 RSA/SHA512 Signature, key ID 621e9f35: NOKEY
Public key for docker-buildx-plugin-0.11.2-1.el7.x86_64.rpm is not installed
(2/10): docker-buildx-plugin-0.11.2-1.el7.x86_64.rpm                                                                              |  13 MB  00:00:04     
(3/10): containerd.io-1.6.22-3.1.el7.x86_64.rpm                                                                                   |  34 MB  00:00:05     
(4/10): docker-ce-cli-24.0.6-1.el7.x86_64.rpm                                                                                     |  13 MB  00:00:01     
(5/10): docker-ce-rootless-extras-24.0.6-1.el7.x86_64.rpm                                                                         | 9.1 MB  00:00:01     
(6/10): fuse-overlayfs-0.7.2-6.el7_8.x86_64.rpm                                                                                   |  54 kB  00:00:00     
(7/10): slirp4netns-0.4.3-4.el7_8.x86_64.rpm                                                                                      |  81 kB  00:00:00     
(8/10): fuse3-libs-3.6.1-4.el7.x86_64.rpm                                                                                         |  82 kB  00:00:00     
(9/10): docker-ce-24.0.6-1.el7.x86_64.rpm                                                                                         |  24 MB  00:00:05     
(10/10): docker-compose-plugin-2.21.0-1.el7.x86_64.rpm                                                                            |  13 MB  00:00:02     
---------------------------------------------------------------------------------------------------------------------------------------------------------
Total                                                                                                                    9.9 MB/s | 107 MB  00:00:10     
Retrieving key from https://download.docker.com/linux/centos/gpg
Importing GPG key 0x621E9F35:
 Userid     : "Docker Release (CE rpm) <docker@docker.com>"
 Fingerprint: 060a 61c5 1b55 8a7f 742b 77aa c52f eb6b 621e 9f35
 From       : https://download.docker.com/linux/centos/gpg
Is this ok [y/N]: y
Running transaction check
Running transaction test
Transaction test succeeded
Running transaction
  Installing : 2:container-selinux-2.119.2-1.911c772.el7_8.noarch                                                                                   1/10 
  Installing : containerd.io-1.6.22-3.1.el7.x86_64                                                                                                  2/10 
  Installing : docker-buildx-plugin-0.11.2-1.el7.x86_64                                                                                             3/10 
  Installing : slirp4netns-0.4.3-4.el7_8.x86_64                                                                                                     4/10 
  Installing : docker-compose-plugin-2.21.0-1.el7.x86_64                                                                                            5/10 
  Installing : 1:docker-ce-cli-24.0.6-1.el7.x86_64                                                                                                  6/10 
  Installing : fuse3-libs-3.6.1-4.el7.x86_64                                                                                                        7/10 
  Installing : fuse-overlayfs-0.7.2-6.el7_8.x86_64                                                                                                  8/10 
  Installing : 3:docker-ce-24.0.6-1.el7.x86_64                                                                                                      9/10 
  Installing : docker-ce-rootless-extras-24.0.6-1.el7.x86_64                                                                                       10/10 
  Verifying  : fuse3-libs-3.6.1-4.el7.x86_64                                                                                                        1/10 
  Verifying  : docker-compose-plugin-2.21.0-1.el7.x86_64                                                                                            2/10 
  Verifying  : slirp4netns-0.4.3-4.el7_8.x86_64                                                                                                     3/10 
  Verifying  : 2:container-selinux-2.119.2-1.911c772.el7_8.noarch                                                                                   4/10 
  Verifying  : docker-ce-rootless-extras-24.0.6-1.el7.x86_64                                                                                        5/10 
  Verifying  : 3:docker-ce-24.0.6-1.el7.x86_64                                                                                                      6/10 
  Verifying  : containerd.io-1.6.22-3.1.el7.x86_64                                                                                                  7/10 
  Verifying  : docker-buildx-plugin-0.11.2-1.el7.x86_64                                                                                             8/10 
  Verifying  : fuse-overlayfs-0.7.2-6.el7_8.x86_64                                                                                                  9/10 
  Verifying  : 1:docker-ce-cli-24.0.6-1.el7.x86_64                                                                                                 10/10 

Installed:
  containerd.io.x86_64 0:1.6.22-3.1.el7 docker-ce.x86_64 3:24.0.6-1.el7 docker-ce-cli.x86_64 1:24.0.6-1.el7 docker-compose-plugin.x86_64 0:2.21.0-1.el7

Dependency Installed:
  container-selinux.noarch 2:2.119.2-1.911c772.el7_8    docker-buildx-plugin.x86_64 0:0.11.2-1.el7    docker-ce-rootless-extras.x86_64 0:24.0.6-1.el7   
  fuse-overlayfs.x86_64 0:0.7.2-6.el7_8                 fuse3-libs.x86_64 0:3.6.1-4.el7               slirp4netns.x86_64 0:0.4.3-4.el7_8                

Complete
````

## 启动docker

>[root@localhost home]# systemctl start docker
>[root@localhost home]# systemctl enable docker

````java
[root@localhost home]# systemctl start docker
[root@localhost home]# systemctl enable docker
Created symlink from /etc/systemd/system/multi-user.target.wants/docker.service to /usr/lib/systemd/system/docker.service
````

## 启动kubernetes

> [root@localhost home]# minikube start --kubernetes-version=v1.23.3 --image-mirror-country='cn' --force



````java
[root@localhost yum.repos.d]# minikube start --kubernetes-version=v1.23.3 --image-mirror-country='cn' --force
* minikube v1.31.2 on Centos 7.7.1908
! minikube skips various validations when --force is supplied; this may lead to unexpected behavior
* Automatically selected the docker driver. Other choices: none, ssh
* The "docker" driver should not be used with root privileges. If you wish to continue as root, use --force.
* If you are running minikube within a VM, consider using --driver=none:
*   https://minikube.sigs.k8s.io/docs/reference/drivers/none/
* Using image repository registry.cn-hangzhou.aliyuncs.com/google_containers
* Using Docker driver with root privileges
* Starting control plane node minikube in cluster minikube
* Pulling base image ...
    > index.docker.io/kicbase/sta...:  447.62 MiB / 447.62 MiB  100.00% 6.25 Mi
! minikube was unable to download registry.cn-hangzhou.aliyuncs.com/google_containers/kicbase:v0.0.40, but successfully downloaded docker.io/kicbase/stable:v0.0.40 as a fallback image
* Creating docker container (CPUs=2, Memory=2200MB) ...
    > kubectl.sha256:  64 B / 64 B [-------------------------] 100.00% ? p/s 0s
    > kubelet.sha256:  64 B / 64 B [-------------------------] 100.00% ? p/s 0s
    > kubectl:  44.43 MiB / 44.43 MiB [--------------] 100.00% 3.14 MiB p/s 14s
    > kubelet:  118.75 MiB / 118.75 MiB [------------] 100.00% 4.35 MiB p/s 28s                                                                        
  - Generating certificates and keys ...
  - Booting up control plane ...
  - Configuring RBAC rules ...
  - Using image registry.cn-hangzhou.aliyuncs.com/google_containers/storage-provisioner:v5
* Verifying Kubernetes components...
* Enabled addons: storage-provisioner, default-storageclass
* kubectl not found. If you need it, try: 'minikube kubectl -- get pods -A'
* Done! kubectl is now configured to use "minikube" cluster and "default" namespace by default
````



## 测试kubectl命令

> minikube kubectl -- get pods -A

````java
[root@localhost yum.repos.d]# minikube kubectl -- get pods -A
NAMESPACE     NAME                               READY   STATUS    RESTARTS       AGE
kube-system   coredns-65c54cc984-97f8k           1/1     Running   0              2m9s
kube-system   etcd-minikube                      1/1     Running   0              2m22s
kube-system   kube-apiserver-minikube            1/1     Running   0              2m24s
kube-system   kube-controller-manager-minikube   1/1     Running   0              2m22s
kube-system   kube-proxy-k949r                   1/1     Running   0              2m10s
kube-system   kube-scheduler-minikube            1/1     Running   0              2m22s
kube-system   storage-provisioner 
````



## 为minikube取别名

> vim ~/.bashrc
>
> source ~/.bashrc
>
> kubectl get nodes

`````java
在最后一行添加
alias kubectl='minikube kubectl --'
`````

## 安装命令补全工具

> yum install bash-completion -y
>
> source /usr/share/bash-completion/bash_completion
>
> source <(kubectl completion bash)

##  尝试安装镜像

> kubectl run ngx --image=nginx:alpine
>
> kubectl get pows -w

## 打开dashboard

> minikube dashboard

## 开启dashboard远程访问

> kubectl proxy --port=8000 --address='192.168.73.128' --accept-hosts='^.*' &