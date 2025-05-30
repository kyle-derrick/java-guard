# jg-launcher 🔐
[![Rust](https://img.shields.io/badge/Rust-1.41+-red)](https://rust-lang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java Guard](https://img.shields.io/badge/Integration-Java_Guard-orange)](https://github.com/java-guard/java-guard)

> 专为 Java Guard 设计的轻量级 Native 启动器，通过拦截 `ClassFileLoadHook` 实现运行时动态解密，保护加密 JAR 免受反编译和代码窃取。
> 
> 注意：需配合 [Java Guard 加密工具](https://github.com/java-guard/java-guard) 使用
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
jg-launcher 需配合 [Java Guard 加密工具](https://github.com/java-guard/java-guard) 使用：
1. 使用 `java-guard` 加密原始 JAR
2. 通过 `jg-launcher` 启动加密后的 JAR
```

### 3. **贡献指南**
```markdown
## 🤝 如何贡献
- 报告问题: [Issues](https://github.com/java-guard/jg-launcher/issues)
- 提交 PR: 遵循 Rust 编码规范，附带单元测试
- 安全漏洞: 请邮件至 feng.kyle@outlook.com
```