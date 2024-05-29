# springboot-flyway

Spring Boot 集成 flyway 数据库版本管理工具

> flyway 可以实现管理并跟踪数据库变更，支持数据库版本自动升级，而且不需要复杂的配置，能够帮助团队更加方便、合理的管理数据库变更。

快速入门

1）引入依赖

可以直接集成自 spring-boot-dependencies 的版本

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

2）配置 flyway 脚本地址

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migration
```

3）使用

增量迁移脚本：V{version}__name.sql

重复迁移脚本：R__name.sql

PS：注意V和R后面的都是两个下划线

4）启动迁移

运行 flyway 之后，数据库会自动创建一张迁移历史表 flyway_schema_history，可以在表中查询已经执行过的脚本

迁移过的脚本不允许被修改，每次启动项目时 flyway 都会依次校验每个脚本在历史表内的校验码是否一致防止脚本变动
