# Better Inventory And Backpacks — Minecraft 26.3 / Fabric 移植版

> 把 [falling_colud](https://github.com/falling-colud/better-inventory) 的 **Better Inventory And Backpacks**
> （NeoForge 1.21.1，MIT）移植到 **Minecraft 26.3 + Fabric**。
>
> 本仓库是移植工程，**不是原作者的官方版本**；上游的 bug 与建议请提给[上游仓库](https://github.com/falling-colud/better-inventory)，
> 移植引入的问题请提到[本仓库 Issues](https://github.com/FiVE0016/BIAB-F-26.3/issues)。

| 项目 | 值 |
|---|---|
| 游戏版本 | Minecraft **26.3**（Fabric） |
| 加载器 | Fabric Loader ≥ 0.19.5 |
| Java | **25** |
| 前置 | Fabric API |
| 移植基线 | 上游 **1.4.1**（NeoForge 1.21.1） |
| 许可证 | MIT（见 [LICENSE](LICENSE)，版权归原作者 falling_colud） |

---

## 它做了什么

按下 **E**，原版生存物品栏被 BetterInventory 界面取代：

- **页签式存储** —— 顶部五个页签：自身物品栏 + 最多四个已装备的**背包**。始终选中一个；**切换页签就是切换物品栏**：27 个主槽位被真实换掉，所以箱子、其它模组的界面、任何读取你物品栏的东西看到的都是当前页签，那边的改动也会回流到该页签。上方 2×9 的**收集仓**随页签一起切换。背包内容存放在背包物品本身上。
- **收集仓** —— 为某页签启用后，地上捡起的物品先进这里。从快捷栏 Shift 点击**不会**填充它，从物品栏 Shift 点击会。
- **工具架**（左侧 2×3）—— 锄 + 锹、两把镐、斧 + 剑。破坏匹配方块时**自动取用**架子上的工具（速度、掉落、附魔、耐久全部来自该工具），近战同理（剑/斧按页签设置）。当前生效的工具显示在快捷栏右侧的 HUD 槽位；未取用工具时它镜像你选中的物品。每个页签有一份方块清单（带搜索）决定哪些方块优先用第二把镐。
- **副手轮盘**（左侧 2×2）—— 四个副手物品。游戏中按住 **Alt** 滚动即可切换；一条竖向小快捷栏会滑出动画，然后收拢到你的选择上。
- **护甲 2×2、背包 + 升级 2×3，以及两列饰品槽**在右侧（**注意：本移植版未实现 Curios 饰品联动**，见下）。
- **槽位锁定** —— 光标持物时中键点击空的存储槽位，可把该槽位锁定为该物品；槽位会渲染一个幽灵图标，其它物品放不进去。
- **堆叠升级** —— 七个等级，把 BetterInventory 存储的堆叠上限提升到 96 → 1024（若配置为 HUNDRED 模式，则普通堆叠变为 100、升级档为 200 → 999）。
- **背包** —— 五个等级，分别容纳 2/4/6/8/10 个能力升级（磁吸、喂养、补充、虚空、拾取）。右键打开背包（手持**或**放置的都可）；潜行 + 右键可放置为方块或装备。每个页签的设置（收集仓、自动补充、战斗武器——每一项都可选择在所有页签间共享）在齿轮按钮里，升级槽也在那里。
- **附加功能** —— 悬停物品会在侧边圆圈里放大渲染（任意物品栏界面）、右侧面板渲染你的角色、快捷栏物品耗尽时自动从存储补充。

## 与上游的差异（请认真读）

移植不是逐字节照搬，以下是**已知的功能差异与限制**：

| # | 差异 | 说明 |
|---|---|---|
| 1 | **饰品（Curios）联动未移植** | 上游依赖 NeoForge 的 Curios 提供两列饰品槽。Fabric 侧没有对应实现，本移植版**不提供**饰品槽；其余功能不受影响 |
| 2 | 联动适配按 Fabric 侧模组重写 | 保留并适配了 `bettercombat`、`polymorph`、`ftb`（FTB 侧边栏）、`emi`、`jei`、`rei` 的联动（检测到才生效，未检测到则完全休眠）；上游对应 NeoForge 版本的联动 |
| 3 | 事件系统重写 | NeoForge 事件总线改为 mixin / Fabric API 事件；玩家登录、维度切换等触发点行为等价但实现不同 |
| 4 | 配置格式 | 沿用上游的配置项语义；文件由 Fabric 侧的配置库读写 |
| 5 | mixin 注入点按 26.3 字节码重定位 | 26.3 移除/改名的方法（如 `Inventory`、`Slot#remove` 的委托链）已逐个改用等价注入点 |

**未在实机验证过的部分**（诚实标注）：`-Dbetterinventory.selftest=true` 自检覆盖了核心交互，但**没有**覆盖所有方块、所有升级组合与多人联机场景。详细验证状态见 [STATUS.md](STATUS.md)。

## 安装

1. 安装 **Fabric Loader ≥ 0.19.5**（Minecraft 26.3）；
2. 把 `betterinventory-<版本>.jar` 放进 `.minecraft/mods/`；
3. 确保装有 **Fabric API**；
4. 启动游戏。日志里出现 `betterinventory` 的加载信息即成功。

> 与其它模组同装时若出现问题（尤其是同样大改物品栏的模组），请带上 `logs/latest.log` 提 issue。

## 构建

```bash
# 需要 JDK 25；离线构建（依赖已在本地 Gradle 缓存中）
gradle build --offline --console=plain --no-daemon
# 产物: build/libs/betterinventory-<版本>.jar
```

资源与数据 JSON 由 `python tools/generate.py` 生成（需要 Pillow）。

## 已知问题 / 待办

- 饰品槽未移植（见上表 #1）；
- 悬停高亮的对齐、工具架/升级槽的取出行为等细节在 26.3 上做过专门修复，但**仍需更广泛的实机确认**；
- 第三人称/动画类联动（如 BetterCombat）未逐项验证。

## 致谢与许可

- 原作：**falling_colud** — [Better Inventory And Backpacks](https://github.com/falling-colud/better-inventory)（MIT）；
- 移植：**FiVE0016**；
- 本项目按 **MIT** 分发，版权声明与许可全文见 [LICENSE](LICENSE)。上游素材中的第三方资源（若有）遵循其原始许可。

如果这个移植版对你有用，请优先去支持原作者。
