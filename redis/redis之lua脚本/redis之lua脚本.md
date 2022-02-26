# redis之lua脚本

Redis是高性能的KV内存数据库，除了做缓存中间件的基本作用外还有很多用途，比如胖哥以前分享的[Redis GEO地理位置信息计算](https://link.segmentfault.com/?enc=%2BEmdpyFywjUOfQV7SkRYbQ%3D%3D.AeSUaEN62j453PLoYst7dkNvlEpFMe9Povt9A9t3rYWRAQGPcnNJTppjRmmhrZh%2FG3O4PkWDHC6whXKjEg5z2A%3D%3D)。Redis提供了丰富的命令来供我们使用以实现一些计算。Redis的单个命令都是原子性的，有时候我们希望能够组合多个Redis命令，并让这个组合也能够原子性的执行，甚至可以重复使用，在软件热更新中也有一席之地。Redis开发者意识到这种场景还是很普遍的，就在2.6版本中引入了一个特性来解决这个问题，这就是Redis执行Lua脚本。

下面我们会使用lua脚本来实现redis的基本命令。这样可以让大家快速了解lua脚本的使用，也不至于增加太多的学习负担。

## String

### 普通字符串操作

```java
public static void set(Jedis jedis,String key,String value){
    String lua = "redis.call('set',KEYS[1],ARGV[1])";
    jedis.eval(lua, Arrays.asList(key),Arrays.asList(value));
}
```

### 删除key

```java
public static void del(Jedis jedis,String key){
    String lua = "redis.call('del',KEYS[1])";
    jedis.eval(lua,Arrays.asList(key),Arrays.asList());
}
```

有了上面的例子，String的其他操作就可以类推了。

## List

### lpush

```
public static void lpush(Jedis jedis,String key,String value){
    String lua = "redis.call('lpush',KEYS[1],ARGV[1])";
    jedis.eval(lua,Arrays.asList(key),Arrays.asList(value));
}
```

### lrange

```java
public static void lrange(Jedis jedis,String key,String start,String top){
    String lua = "return redis.call('lrange',KEYS[1],ARGV[1],ARGV[2])";
    List<String> valus = (List<String>) jedis.eval(lua, Arrays.asList(key), Arrays.asList(start,top));
    System.out.println();
}
```

## Hash

### hset

```java
public static void hset(Jedis jedis,String key,String field,String value){
    String lua = "redis.call('hset',KEYS[1],ARGV[1],ARGV[2])";
    jedis.eval(lua,Arrays.asList(key),Arrays.asList(field,value));
}
```

### hgetAll

```java
public static void hgetall(Jedis jedis,String key){
    String lua = "return redis.call('hgetall',KEYS[1])";
    List<String> valus = (List<String>) jedis.eval(lua, Arrays.asList(key), Arrays.asList());
    Map<String,Object> map = new HashMap<>();

    for (int i = 0;i < valus.size() / 2;i++){
        map.put(valus.get(i * 2),valus.get(i * 2 + 1));
    }
    System.out.println();
}
```