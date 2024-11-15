# Docker

![](assets/Docker/image-20241113182042431.png)

Docker 是基于 Go 语言实现的开源容器项目

为什么要使用 Docker？

1. 无视底层操作系统
2. 更便捷的部署和运维
3. 更高效的资源利用

Docker 与 虚拟化：Docker 等容器技术属于操作系统虚拟化的范畴。相比传统方式在硬件层面的虚拟化，需要有额外的虚拟机管理应用和虚拟机操作系统，Docker 容器在操作系统层面实现虚拟化，并直接复用本地主机的操作系统，因此更加轻量级。

## Docker 安装

### Centos 7 环境下安装

1. 为了方便添加软件源以及支持 devicemapper 存储支持

   `sudo yum install -y yum-utils device-mapper-persistent-data lvm2`

2. 添加 Docker 稳定版本的 yum 软件源

   `sudo yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo`

3. 更新 yum 软件源缓存并安装 Docker

   `sudo yum update`

   `sudo yum install -y docker-ce`

4. 确认 Docker 服务启动成功

   `sudo systemctl start docker`

### Ubuntu 环境下安装

1. 安装使用 HTTPS 传输的软件包以及 CA 证书

   `sudo apt-get install apt-transport-https ca-certificates curl gnupg lsb-release`

2. 添加 docker 官方的 GPG 密钥

   `curl -fsSL http://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | sudo apt-key add -`

3. 添加 docker 的软件源

   `sudo add-apt-repository "deb [arch=amd64] http://mirrors.aliyun.com/docker-ce/linux/ubuntu $(lsb_release -cs) stable"`

4. 安装 docker

   `sudo apt-get install docker-ce`

5. 配置用户组（默认情况下，只有 root 和 docker 组的用户才可以运行 docker 命令）

   `sudo usermod -aG docker $USER`

6. 启动 docker

   `systemctl start docker`

## Docker 三大概念

![img](assets/Docker/1387124-20180808221433616-1309217661.png)

<center>镜像、容器、仓库三者关系图</center>

### 镜像 image

Docker 镜像是一个特殊的文件系统，除了提供容器运行时所需的程序、库、资源、配置等文件外，还包含了一些为运行时准备的一些配置参数（如匿名卷、环境变量、用户等）。镜像不包含任何动态数据，其内容在构建之后也不会被改变。

常用命令

- 搜寻镜像：`docker search`
- 拉取镜像：`docker pull`
- 列出所有镜像：`docker images`
- 查看镜像信息：`docker inspect`
- 查看镜像创建过程：`docker history `（过长命令被截断可以使用 --no-trunc 选项输出完整命令）
- 删除镜像：`docker rmi`

- 创建镜像

  **基于现有容器创建**：`docker commit`

  > 当容器与镜像对比发生变化时，可以使用该命令提交一个新的镜像

  语法：`docker commit [OPTIONS] CONTAINER [REPOSITORY[:TAG]]`

  | 参数         | 用途                             |
  | ------------ | -------------------------------- |
  | -a，--author | 著名作者                         |
  | -c，--change | 在导入过程中使用 dockerfile 指令 |
  | -m，massage  | 提交的说明信息                   |
  | -p，--pause  | 提交镜像前暂停容器               |

  PS：该方式适用于例如 *被入侵后保护现场* 等特殊场景

  **基于本地模板导入**：`docker import`

  > 从一个 tar 或者 url 导入容器快照

  语法：`docker import [OPTIONS] file|URL|- [REPOSITORY[:TAG]]`

  | 参数          | 用途                             |
  | ------------- | -------------------------------- |
  | -c，--change  | 在导入过程中使用 dockerfile 指令 |
  | -m，--message | 导入的说明信息                   |

  **基于 Dockerfile 生成**：`docker build`

  > 从 dockerfile 构建 docker 镜像

  语法：`docker buildx build [OPTIONS] PATH | URL | -`

  | 参数       | 用途                 |
  | ---------- | -------------------- |
  | -f，--file | 指定 dockerfile 路径 |
  | -t，--tag  | 指定镜像名称和版本   |

- 保存和加载镜像
  - 保存一个或多个镜像：`docker save -o {FILE} IMAGE [IMAGE...]`
  - 从文件载入镜像：`docker load -i {FILE}`
- 上传镜像：`docker push [REPOSITORY/]IMAGE[:TAG]`

### 容器 container

容器是镜像的一个运行实例

常用命令：

- 创建容器：`docker create`

  | 参数          | 用途         |
  | ------------- | ------------ |
  | --name        | 指定容器名称 |
  | -e，--env     | 设置环境变量 |
  | -v，--volume  | 挂载数据卷   |
  | -p，--publish | 暴露端口     |

- 启动容器：`docker run`

- 创建并运行容器：`docker run` 等同于执行 `docker create` 后再执行 `docker start`

- 查看所有容器：`docker ps`

- 查看容器输出：`docker logs`

- 停止容器：`docker stop`

- 进入容器：`docker exec -it image /bin/bash`

- 删除容器：`docker rm`

- 容器导出：`docker export`

- 容器导入：`docker import`

- 查看容器详情：`docker container inspect`

- 查看容器内进程：`docker top`

- 查看容器统计信息：`docker stats`

- 宿主机和容器间复制文件：`docker cp`

- 查看容器变更内容：`docker diff`

- 查看端口映射：`docker port`

- 修改容器运行时配置：`docker update`

### 仓库 repository

仓库（Repository）是集中存放镜像的地方。

一个容易混淆的概念是注册服务器（Registry）。实际上注册服务器是管理仓库的具体服务器，每个服务器上可以有多个仓库，而每个仓库下面有多个镜像。从这方面来说，仓库可以被认为是一个具体的项目或目录。例如对于仓库地址 docker.io/ubuntu 来说，docker.io 是注册服务器地址，ubuntu 是仓库名。

**如何配置镜像源？**

linux

在 `/etc/docker/daemon.json` 内维护注册服务器地址属性 `registry-mirrors`

本案例选用 DaoCloud 组织提供的国内免费镜像源

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io"
  ]
}
```

windows

在 Docker Desktop 设置 Docker Engine 内修改，配置同上

**私有仓库 Registry**

私有库 registry 相当于本地有一个 Docker Hub

安装方式：

1. 拉取 registry：`docker pull registry:2`
2. 启动仓库：`docker run -d -p 5000:5000 --name registry registry:2`

查看私有库上面的镜像：`curl -XGET http://ip:port/v2/_catalog`

上传镜像到指定仓库：`docker push ip:port/image[:tag]`

拉取指定仓库镜像：`docker pull ip:port/image[:tag]`

:star: 配置私有仓库

在 daemon.json 文件内添加非 http 不安全仓库

```json
{
  "insecure-registries": [
    "ip:port"
  ]
}
```

重启 docker：`systemctl restart docker`

拉取私有仓库镜像：`docker pull ip:port/image[:tag]`

## Dockerfile

Dockerfile 是一个文本格式的配置文件，用户可以使用 Dockerfile 来快速创建自定义的镜像

<table>
    <tr>
    	<th align='center'>分类</th>
        <th align='center'>指令</th>
        <th align='center'>说明</th>
    </tr>
    <tr>
    	<td rowspan=13 align='center'>配置指令</td>
        <td>ARG</td>
        <td>定义创建镜像过程中使用的变量</td>
    </tr>
    <tr>
        <td>FROM</td>
        <td>指定所创建镜像的基础镜像</td>
    </tr>
    <tr>
        <td>LABEL</td>
        <td>为生成的镜像添加元数据标签信息</td>
    </tr>
    <tr>
        <td>EXPOSE</td>
        <td>声明镜像内服务监听的端口</td>
    </tr>
    <tr>
        <td>ENV</td>
        <td>指定环境变量</td>
    </tr>
    <tr>
        <td>ENTRYPOINT</td>
        <td>指定镜像的默认入口命令</td>
    </tr>
    <tr>
        <td>VOLUME</td>
        <td>创建一个数据卷挂载点</td>
    </tr>
    <tr>
        <td>USER</td>
        <td>指定运行容器时的用户名或UID</td>
    </tr>
    <tr>
        <td>WORKDIR</td>
        <td>配置工作目录</td>
    </tr>
    <tr>
        <td>ONBUILD</td>
        <td>创建子镜像时指定自动执行的操作指令</td>
    </tr>
    <tr>
        <td>STOPSIGNAL</td>
        <td>指定退出的信号值</td>
    </tr>
    <tr>
        <td>HEALTHCHECK</td>
        <td>配置所启动容器如何进行健康检查</td>
    </tr>
    <tr>
        <td>SHELL</td>
        <td>指定默认的sheel类型</td>
    </tr>
    <tr>
    	<td rowspan=4 align='center'>操作指令</td>
        <td>RUN</td>
        <td>运行指定命令</td>
    </tr>
    <tr>
        <td>CMD</td>
        <td>启动容器时指定默认执行的命令</td>
    </tr>
    <tr>
        <td>ADD</td>
        <td>添加内容到镜像</td>
    </tr>
    <tr>
        <td>COPY</td>
        <td>复制内容到镜像</td>
    </tr>
</table>

**将 Spring Boot 项目成镜像**

编写 Dockerfile 文件

```
# 指定基础镜像
FROM openjdk:11
# 复制jar包到容器中
ADD target/dockerfile-test-image.jar dockerfile-test-image.jar
# 声明服务端口
EXPOSE 8080
# 容器启动运行程序命令
ENTRYPOINT ["java", "-jar", "dockerfile-test-image.jar"]
```

参考资料：

[1]:《Docker技术入门与实践第3版》
[2]:https://zhuanlan.zhihu.com/p/102794942	"使用Dockerfile为SpringBoot应用构建Docker镜像"

