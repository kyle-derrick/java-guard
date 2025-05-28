# java-guard

用于jar包代码及资源加密的工具，提供class字节码及常量加密，及运行时解密。

> 目前主要是将完整流程及功能实现出来了，后续还需进一步完善，比如：代码注释、错误处理及提示优化、class执行支持、jre环境签名校验等。
> 

## 核心逻辑简介
### class加密及运行时解密
1. 通过解析class、提取并替换空值到常量池以及方法代码中（常量池会跳过支撑class文件基本结构的一下常量）
2. 将提取出的常量及字节码加密并存入class属性中
3. 通过launcher启动时，调用jvm库创建jvm并设置ClassFileLoadHook用于拦截并解密class文件

### 资源加密及运行时解密
1. 直接分块加密并填充文件头
2. 通过launcher启动时，会修改URL类，在openConnection方法末尾添加InternalResourceURLConnection.handleConnection的调用，用以判断资源是否为加密资源，并使用解密流替换原本的数据输入流。

> 其实原理挺简单的，通过launcher启动的好处时，避免so库或者agent jar包暴露导致解密方法泄露，且可以做到更多检测或拦截等操作，比如目前已做的agent参数拦截、jar签名校验。
> 

## 快速开始
### 1. 环境说明
我的测试环境如下：
* JDK: 1.8 / 21
* Maven: 3.8.3
* Rust: 1.82.0
### 2. 克隆仓库并初始化子库
```shell
git clone git@github.com:java-guard/java-guard.git
cd java-guard
git submodule update --init
```
### 3. 编译
```shell
mvn clean package
```
编译得到jar：target/java-guard-*.jar

### 4. 添加加密配置

* 生成公私钥
```shell
ssh-keygen -t ed25519 -f example_config/id_ed25519
# 一直回车即可
```

* 参考[config.yml](example_config%2Fconfig.yml)
```yaml
# 需加密的文件，通配符
matches:
  - "org/antlr/v4/*"
# 加密密钥，可不填，未配置时随机生成
key: test_key
# ed25519私钥密钥
privateKey: example_config/id_ed25519
# ed25519公钥密钥，未指定时根据私钥生成
publicKey: example_config/id_ed25519.pub
```

### 5. 执行jar包加密传入的jar包
```shell
usage: java-guard
 -c,--config <arg>   config files (default: ./config.yml)
 -h,--help           print usage
 -m,--mode <arg>     encrypt/decrypt/signature mode (default encrypt)
 -o,--output <arg>   output dir
```
例如将当前目录的 antlr-4.13.2-complete.jar 按example_config/config.yml加密并输出到out目录：
```shell
java -jar target/java-guard-1.0-SNAPSHOT.jar -c example_config/config.yml -o out antlr-4.13.2-complete.jar
```
> 生成的启动器路径为 out/jg-launcher/jg-launcher （windows为jg-launcher.exe）
> 

### 6. 启动加密后的jar包
```shell
./out/jg-launcher/jg-launcher -jar out/antlr-4.13.2-complete.jar
```

