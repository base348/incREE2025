# 静态/增量的近似拒绝约束（DC）挖掘

# Static/Incremental approximate denial constraints mining

Implemented in Java 17

## CLI usage
1. Static mining
```shell
java -jar IncDC.jar [filename] static [lines] --errThreshold=N --dcLength=L
```
Example:
```shell
java -jar IncDC.jar adult.csv static 1000 --errThreshold=50 --dcLength=6
```

2. inc
```shell
java -jar IncDC.jar [filename] inc [lines] [inclines] --errThreshold=N --dcLength=L
```
Example:
```shell
java -jar IncDC.jar adult.csv inc 1000 50 --errThreshold=50 --dcLength=6
```

- 执行增量挖掘前，需确保已完成对应 lines 数量的挖掘（通过 static 或 inc 模式完成）
- `filename` - 放在`.\input\`路径下，必须为`.csv`格式，第一行为字段名（header），具体可以查看`adult.csv`示例
- `errThreshold` - 整数，允许违反 DC 的最大错误数量，`0`表示精确挖掘
- `dcLength` - 整数，一条DC包含的谓词数上限


- Before performing incremental mining, ensure that mining for the base dataset with lines has already been completed (via either `static` or `inc` mode);
- `filename` - input file in `.csv` format under the directory `.\input\`;
- `errThreshold` - Maximum number of violations allowed per DC; 0 for exact mining;
- `dcLength` - Maximum number of predicates per DC.
