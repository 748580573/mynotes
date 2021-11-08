# htstrix参数说明

## @DefaultProperties 注解

该注解用于为 Hystrix 命令指定默认参数，例如：defaultFallback = "defaultFallback"。如果我们需要为某个特定方法指定参数，可以使用 @HystrixCommand 注解。

- **groupKey**：默认情况下指定的每个 Hystrix 命令使用默认组密钥，除非命令显式指定组密钥。默认为空字符串
- **threadPoolKey**：默认情况下指定的每个 Hystrix 命令使用默认线程池密钥，除非命令显式指定了线程池密钥。默认为空字符串
- **commandProperties**：将用于为每个 Hystrix 命令的命令属性指定默认属性，除非在 @HystrixCommand 中显式指定命令属性。
- **threadPoolProperties**：将用于为每个 Hystrix 命令的线程池属性指定默认值，除非在 @HystrixCommand 中显式指定线程池属性。
- **ignoreExceptions**：定义应该被忽略的异常。如果 raiseHystrixExceptions 包含 RUNTIME_EXCEPTION，则这些可以被包装在 HystrixRuntimeException 中。
- **raiseHystrixExceptions**：当包含 RUNTIME_EXCEPTION 时，任何未被忽略的异常都会被包装在 HystrixRuntimeException 中。
- **defaultFallback**：为给定类中的每个命令指定默认回退方法。类中的每个命令都应该有一个与默认回退方法返回类型兼容的返回类型。注意：默认回退方法不能有参数。



## @HystrixCommand

@HystrixCommand 注解 能对某个一个接口定制 Hystrix的超时时间。

* 通过修改 execution.isolation.thread.timeoutInMilliseconds 属性可以设置超时时间，

* 通过设置 fallbackMethod 可以设置超时后响应的格式
* 设置 **fallbackMethod** 指定的 返回值方法类型要跟目标方法一致，否则将报错。
* 如果方法内部有明显的异常,将不走目标方法，直接返回 fallback 方法的返回值。
* 如果@HystrixCommand 注解同时指定了目标方法的 timeoutInMilliseconds，同时又在配置文件 application.yml 中配置了hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds ，甚至设置了Ribbon.ReadTimeout

````java
 @HystrixCommand(fallbackMethod = "sleepFallback", commandProperties =
            {
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000")
            })
@PostMapping("/sleep2")
public ResultBean test02(@RequestParam(value = "sleep") Integer sleep) throws InterruptedException {
        log.info("开始睡眠" + sleep + "毫秒");
        String s = null;
        s.trim();//编译通过，但明显空指针异常
        int i = 1 / 0;编译通过，但明显算术异常
        Thread.sleep(sleep);
        log.info("睡眠结束...");
        return ResultBean.result("请求结束!");
}
````



## 熔断器相关

- **circuitBreaker.enabled**：是否启用熔断器，默认为 true;

- **circuitBreaker.forceOpen**： **circuitBreaker.forceClosed**：是否强制启用/关闭熔断器，强制启用关闭都想不到什么应用的场景，保持默认值，不配置即可。

- **circuitBreaker.requestVolumeThreshold**：启用熔断器功能窗口时间内的最小请求数。试想如果没有这么一个限制，我们配置了 50% 的请求失败会打开熔断器，窗口时间内只有 3 条请求，恰巧两条都失败了，那么熔断器就被打开了，5s 内的请求都被快速失败。此配置项的值需要根据接口的 QPS 进行计算，值太小会有误打开熔断器的可能，值太大超出了时间窗口内的总请求数，则熔断永远也不会被触发。建议设置为 `QPS * 窗口秒数 * 60%`。

- **circuitBreaker.errorThresholdPercentage**：在通过滑动窗口获取到当前时间段内 Hystrix 方法执行的失败率后，就需要根据此配置来判断是否要将熔断器打开了。 此配置项默认值是 50，即窗口时间内超过 50% 的请求失败后会打开熔断器将后续请求快速失败。

- **circuitBreaker.sleepWindowInMilliseconds**：熔断器打开后，所有的请求都会快速失败，但何时服务恢复正常就是下一个要面对的问题。熔断器打开时，Hystrix 会在经过一段时间后就放行一条请求，如果这条请求执行成功了，说明此时服务很可能已经恢复了正常，那么会将熔断器关闭，如果此请求执行失败，则认为服务依然不可用，熔断器继续保持打开状态。此配置项指定了熔断器打开后经过多长时间允许一次请求尝试执行，默认值是 5000。

  

## 统计器相关参数

**`滑动窗口`**： Hystrix 的统计器是由滑动窗口来实现的，我们可以这么来理解滑动窗口：一位乘客坐在正在行驶的列车的靠窗座位上，列车行驶的公路两侧种着一排挺拔的白杨树，随着列车的前进，路边的白杨树迅速从窗口滑过，我们用每棵树来代表一个请求，用列车的行驶代表时间的流逝，那么，列车上的这个窗口就是一个典型的滑动窗口，这个乘客能通过窗口看到的白杨树就是 Hystrix 要统计的数据。

**`桶`**： bucket 是 Hystrix 统计滑动窗口数据时的最小单位。同样类比列车窗口，在列车速度非常快时，如果每掠过一棵树就统计一次窗口内树的数据，显然开销非常大，如果乘客将窗口分成十分，列车前进行时每掠过窗口的十分之一就统计一次数据，开销就完全可以接受了。 Hystrix 的 bucket （桶）也就是窗口 N分之一 的概念。

- **metrics.rollingStats.timeInMilliseconds**：此配置项指定了窗口的大小，单位是 ms，默认值是 1000，即一个滑动窗口默认统计的是 1s 内的请求数据。
- **metrics.healthSnapshot.intervalInMilliseconds**：它指定了健康数据统计器（影响 Hystrix 熔断）中每个桶的大小，默认是 500ms，在进行统计时，Hystrix 通过 `metrics.rollingStats.timeInMilliseconds / metrics.healthSnapshot.intervalInMilliseconds` 计算出桶数，在窗口滑动时，每滑过一个桶的时间间隔时就统计一次当前窗口内请求的失败率。
- **metrics.rollingStats.numBuckets**：Hystrix 会将命令执行的结果类型都统计汇总到一块，给上层应用使用或生成统计图表，此配置项即指定了，生成统计数据流时滑动窗口应该拆分的桶数。此配置项最易跟上面的 `metrics.healthSnapshot.intervalInMilliseconds` 搞混，认为此项影响健康数据流的桶数。 此项默认是 10，并且需要保持此值能被 `metrics.rollingStats.timeInMilliseconds` 整除。
- **metrics.rollingPercentile.enabled**：是否统计方法响应时间百分比，默认为 true 时，Hystrix 会统计方法执行的 `1%,10%,50%,90%,99%` 等比例请求的平均耗时用以生成统计图表。
- **metrics.rollingPercentile.timeInMilliseconds**：统计响应时间百分比时的窗口大小，默认为 60000，即一分钟。
- **metrics.rollingPercentile.numBuckets**：统计响应时间百分比时滑动窗口要划分的桶用，默认为6，需要保持能被metrics.rollingPercentile.timeInMilliseconds整除。
- **metrics.rollingPercentile.bucketSize**：统计响应时间百分比时，每个滑动窗口的桶内要保留的请求数，桶内的请求超出这个值后，会覆盖最前面保存的数据。默认值为 100，在统计响应百分比配置全为默认的情况下，每个桶的时间长度为 10s = 60000ms / 6，但这 10s 内只保留最近的 100 条请求的数据。
