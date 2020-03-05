#### 导出csv

impala-shell -q "select * from rk1b1" -d default -B --output_delimiter=","  -o /home/miner/rk1b1.csv

````
　　• -q query （--query=query） 从命令行执行查询，不进入impala-shell
　　• -d default_db （--database=default_db） 指定数据库
　　• -B（--delimited）去格式化输出
　　• --output_delimiter=character 指定分隔符
　　• --print_header 打印列名
　　• -f query_file（--query_file=query_file）执行查询文件，以分号分隔
　　• -o filename （--output_file filename） 结果输出到指定文件
````

