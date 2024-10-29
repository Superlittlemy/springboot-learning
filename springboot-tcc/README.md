#### TCC 柔性事务

> 最早是由 Pat Helland 于2007年发表的一篇名为《Life beyond Distributed Transactions:an Apostate’s Opinion》的论文提出

柔性事务和刚性事务

- 柔性事务满足 BASE 理论（基本可用，最终一致性）
- 刚性事务满足 ACID 理论

TCC 是一种补偿型事务，该模型要求应用的每个服务提供 try、confirm、cancel 三个接口，它的核心思想是通过对资源的预留（提供中间态，如账户状态、冻结金额等），尽早释放对资源的加锁，如果事务可以提交，则完成对预留资源的确认，如果事务要回滚，则释放预留的资源。

TCC将事务提交分为 Try-Confirm-Cancel 3个操作：

- Try：预留业务资源/数据效验
- Confirm：确认执行业务操作
- Cancel：取消执行业务操作

TCC 事务处理流程和 2PC 二阶段提交类似，不过 2PC 通常都是在跨库的 DB 层面，而 TCC 本质就是一个应用层面的 2PC。TCC 模型完全交由业务实现，每个子业务都需要实现 Try-Confirm-Cancel 三个接口，对业务侵入大，资源锁定交由业务方。

**TCC 框架对参与者的要求**

- 幂等性

  所有操作 Try、Confirm、Cancel 三个方法均需满足保证幂等。一旦发生网络波动重试或事务补偿执行，不幂等的接口重复执行后便会有数据正确性的风险

- 允许空回滚

  由于网络原因在阶段一部分参与者没有收到 try 请求却收到了阶段二的 cancel 请求，这种情况下需要进行空回滚

- 放置资源悬挂

  由于网络原因导致两个阶段无法保证顺序执行，如果 cancel 请求先到达并执行了空回滚，后续到达的 try 请求就无需再执行从而避免锁定资源无法释放的问题

此外，在 TCC 的设计内认定 Confirm 和 Cancel 阶段一定会成功，故 C/C 阶段尽量不要产生服务通信，缩小处理范围。如果 C/C 出现失败，需要由事务组件实现重试机制，重试失败后需要有兜底定时任务执行最终一致性对齐！

参考资料：

[1]:https://www.cnblogs.com/yuhushen/p/15520976.html	"分布式事务（四）之TCC"
[2]:https://dl.acm.org/doi/pdf/10.1145/3012426.3025012	"《Life beyond Distributed Transactions:an Apostate’s Opinion》"
[3]:https://zhuanlan.zhihu.com/p/148747139	"分布式柔性事务的TCC方案"
[4]:https://zhuanlan.zhihu.com/p/163864897	"分布式事务——两阶段提交、三阶段提交和TCC框架"
