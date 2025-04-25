## Kubernetes

### 安装

**kubectl**

1）下载 kubectl

```shell
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
```

PS：如需下载某个指定的版本，请用指定版本号替换该命令的这一部分 `$(curl -L -s https://dl.k8s.io/release/stable.txt)`。

2）安装

```shell
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

3）验证测试

```shell
kubectl version --client
```

摘自官方安装教程：https://kubernetes.io/zh-cn/docs/tasks/tools/install-kubectl-linux

**minikube**

1）安装 minikube

```shell
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
```

2）启动 cluster

```shell
minikube start --kubernetes-version=v1.30.3
```

摘自官方安装教程：https://minikube.sigs.k8s.io/docs/start

## Etcd 键值存储系统

etcd 是 CoreOS 团队于 2013 年 6 月发起的开源项目，它的目标是构建一个高可用的分布式键值数据库

> A highly-available key value store for shared configuration and service discovery. 
>
> 一个用于配置共享和服务发现的高可用键值存储系统。

**特点：**

- 简单：支持 Restful API 和 gRPC API
- 安全：支持 TLS 方式实现的安全连接访问
- 快速：支持每秒一万次的并发写操作，超时控制在毫秒量级
- 可靠：支持分布式结构，基于 Raft 算法实现一致性

**使用场景**

1. 键值对存储
2. 服务注册与发现
3. 消息发布与订阅
4. 分布式通知与协调
5. 分布式锁

**安装指南**

docker 安装（镜像仓库 `qury.io/coreos/etcd` | `bitnami/etcd`）

```shell
docker run -it -p 2379:2379 -p 2380:2380 --name io-etcd {registry}/etcd[:tag]
```

**etcdctl**

etcdctl 是 Etcd 官方提供的命令行客户端，它支持基于 HTTP API 封装的命令，供用户直接根 Etcd 服务打交道

**Raft 共识算法**

参考资料：

[1]:https://zhuanlan.zhihu.com/p/143564728	"初识 etcd"