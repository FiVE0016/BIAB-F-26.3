# BetterInventory → Minecraft 26.3 Fabric：施工状态

工程已搭好，**未编译**（沙盒无 JDK 25 / 无 Maven 与 Mojang 下载权限）。
在你本地 `./gradlew build` 才会暴露真实报错。下面按优先级列出已完成与待办。

## 一、已完成

### 构建骨架（对齐官方 26.3 模板）
- `build.gradle` / `gradle.properties` / `settings.gradle`，Gradle 9.7.1 wrapper 已就位
- `minecraft 26.3` · `loader 0.19.5` · `loom 1.18-SNAPSHOT` · `fabric-api 0.161.0+26.3`
- Java 25（`options.release = 25`、`compatibilityLevel: JAVA_25`）
- `fabric.mod.json`：main + client 双入口、mixins、依赖声明

### 兼容 shim 层（`shim/` 包，18 个类）
把 NeoForge 高频 API 映射到 Fabric 原生实现，**56 个业务文件几乎零改动**：

| NeoForge | Fabric 落点 |
|---|---|
| `DeferredRegister` / `DeferredItem` / `DeferredBlock` | 立即 `Registry.register` |
| `IMenuTypeExtension.create` | `ExtendedScreenHandlerType` |
| `ItemStackHandler` / `IItemHandler(Modifiable)` / `SlotItemHandler` | 复用项目自带的 `ItemStore`（已补桥接方法） |
| `PacketDistributor` | `ClientPlayNetworking` / `ServerPlayNetworking` |
| `RegisterPayloadHandlersEvent` / `PayloadRegistrar` | `PayloadTypeRegistry` + `registerGlobalReceiver` |
| `INBTSerializable` / `TriState` | 同名轻量接口 |

### 全库机械改造
- `ResourceLocation` → `Identifier`（26.3 重命名），**全库零 `net.neoforged` 残留**
- `player.getData(LOADOUT)` / `getExistingData(...).orElse(null)` → `ModAttachments.get(player)`（共 9 处）
- 22 个文件的 NeoForge 导入已重定向到 shim

### 新写的 Fabric 侧代码
- `BetterInventory` — 保留类名与 `id()`，`ModInitializer`
- `ModAttachments` — Fabric 原生数据附着（**无需 Cardinal Components**）
- `LoadoutStorage` — 按 UUID 存 NBT，进服读、退服存（附着 API 走 Codec，管不了 NBT 持久化）
- `ItemDefaults` — `DefaultItemComponentEvents.MODIFY`，堆叠上限逻辑原样保留
- `FabricCommonEvents` — 服务端 tick / 死亡同步
- `FabricPlatform` + `TrinketsCompat` — 饰品栏改用 Trinkets（反射软依赖，缺装时走 `NONE`）
- `BetterInventoryClient` / `ClientEvents` — 屏幕、按键、HUD
- `BetterInventoryConfig` — 自研 JSON 配置，保留 `.get()` 调用风格

## 二、待办（按优先级）

### P0 — 不解决无法运行
1. **Mixin 需在 26.3 字节码上重新定位**
   - `ItemStackCountCodecMixin` 注入 `lambda$static$3`（编译产物 lambda 编号）。26.1 起官方不再混淆，此锚点几乎必然失效 → 堆叠升级的序列化命脉
   - `ContainerCursorMixin`（`doClick`，`require=1`，失败即崩）
   - `PlayerCombatMixin`（MixinExtras `@WrapMethod`/`@WrapOperation`；26.3-rc-1 改过战斗逻辑：MC-311607、MC-311705）
2. **6 个 NeoForge 事件在 Fabric 无对应**，需改走 Mixin：
   - `PlayerEvent.BreakSpeed`（工具架挖速）
   - `PlayerEvent.HarvestCheck`
   - `ItemEntityPickupEvent.Pre`（采集中枢；`GatherService.onPickup` 签名已改成 `(Player, ItemEntity)`，调用方待接）
   - `LivingDropsEvent`（死亡掉落）
   - `ScreenEvent.Opening`（需可替换 screen，Fabric `ScreenEvents` 做不到）
   - `MouseScrollingEvent`（副手轮盘 Alt+滚轮）
3. **`CommonEvents` 的 10 个处理器主体未搬**（`onLogin`/`onLogout`/`onRespawn`/`onChangeDimension`/`onClone`/`onLivingDeath`/`onPlayerDrops` 等），只接通了 tick 与死亡同步。需对照原文件逐个搬进 `FabricCommonEvents`

### P1 — API 名称需实机校验
以下我按 26.3 Fabric API 源码推断，编译报错就说明包路径变了：
- `HandledScreens` — 我用的 `client.screenhandling.v1`，26.3 也可能在 `client.screen.v1`
- `ExtendedScreenHandlerType.ExtendedFactory` 的参数类型（`FriendlyByteBuf` vs `RegistryFriendlyByteBuf`）
- `HudRenderCallback` 包路径
- `NbtIo.writeCompressed(tag, Path)` / `readCompressed(Path)`
- `AttachmentRegistry.createDefaulted` + `player.getAttached(...)`
- `Registry.register(...)`、`Identifier.fromNamespaceAndPath(...)`

### P2 — 其他
4. 机械改名 `ResourceLocation → Identifier` 可能有误伤，编译时留意
5. 注册类依赖静态初始化顺序触发，若报空指针改在 `onInitialize` 里显式触碰
6. 资源/数据包格式：26.3 数据包 **121.0**、资源包 **97.1**，`tools/generate.py` 产物需复核
7. **起点源码是 1.0.0**（2026-07-26），落后 CurseForge 1.1.4/1.1.5 约两个月

## 三、怎么跑

```bash
# 需要 JDK 25
./gradlew build          # 产出 build/libs/betterinventory-1.1.5.jar
./gradlew runClient      # 起客户端实测
```

建议顺序：先 `build` 扫清编译错误 → 再逐个验 mixin → 最后补事件层。
