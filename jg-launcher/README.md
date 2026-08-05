# jg-launcher 🔐
[![Rust](https://img.shields.io/badge/Rust-stable%20%7C%202021%20edition-red)](https://rust-lang.org)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI](https://github.com/kyle-derrick/java-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/kyle-derrick/java-guard/actions/workflows/ci.yml)
[![Java Guard](https://img.shields.io/badge/Integration-Java_Guard-orange)](https://github.com/kyle-derrick/java-guard)

> 专为 Java Guard 设计的轻量级 Native 启动器，通过拦截 `ClassFileLoadHook` 实现运行时动态解密，保护加密 JAR 免受反编译和代码窃取。
>
> 注意：需配合 [Java Guard 加密工具](https://github.com/kyle-derrick/java-guard) 使用
>
> 编译需要 stable Rust（项目使用 Rust 2021 edition）和 JDK；请通过 `JAVA_HOME` 指定 JDK 路径。
>
> 源码位于 Java Guard monorepo 的 [`jg-launcher/`](https://github.com/kyle-derrick/java-guard/tree/main/jg-launcher) 目录。克隆仓库后进入该目录即可构建：`git clone https://github.com/kyle-derrick/java-guard.git && cd java-guard/jg-launcher`。
>
### 1. **工作流程图**
```mermaid
graph TD
A[启动 jg-launcher] --> B[JNI ClassFileLoadHook]
A --> C[扩展URL类]
B --> D[加载加密JAR]
C --> D
D --> E{类加载请求}
E -->|拦截| F[解密字节码]
E -->|资源访问| G[动态解密资源]
F --> H[JVM执行解密后类]
G --> H
```

### 2. **与 Java Guard 集成**
```markdown
## 🔗 Java Guard 集成
jg-launcher 需配合 [Java Guard 加密工具](https://github.com/kyle-derrick/java-guard) 使用：
1. 使用 `java-guard` 加密原始 JAR
2. 通过 `jg-launcher` 启动加密后的 JAR
```

### 3. **贡献指南**
```markdown
## 🤝 如何贡献
- 报告问题: [Issues](https://github.com/kyle-derrick/java-guard/issues)
- 提交 PR: 遵循 Rust 编码规范，附带单元测试
- 安全漏洞: 请邮件至 feng.kyle@outlook.com
```