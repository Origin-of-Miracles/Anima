# 学生资源使用指南

> **注意：** 本目录下的学生模型、动画和纹理文件已从主仓库分离。

## 📥 获取资源

### 官方资源包

访问 [Anima-Assets 仓库](https://github.com/Origin-of-Miracles/Anima-Assets) 下载完整资源包。

### 文件结构

```
assets/anima/
├── geo/entity/students/          # GeckoLib 几何模型
│   ├── arona.geo.json            # ✅ 已内置（默认模板）
│   └── aris.geo.json             # ❌ 需从外部获取
├── animations/entity/students/   # 动画定义
│   ├── arona.animation.json      # ✅ 已内置
│   └── aris.animation.json       # ❌ 需从外部获取
└── textures/entity/students/     # 纹理文件
    ├── arona.png                 # ✅ 已内置
    └── aris.png                  # ❌ 需从外部获取
```

## 🔒 为什么分离？

1. **代码仓库轻量化**：模型和纹理文件体积较大
2. **降低合并冲突**：资源文件更新频繁，独立管理更高效
3. **社区贡献友好**：非开发者也能轻松提交新模型

## 📝 贡献指南

想要添加新学生模型？请访问 [贡献指南](../RESOURCES.md#如何贡献资源)。

---

详细说明请查看：[Anima 外部资源指南](../RESOURCES.md)
