# BetterInventory → Minecraft 26.3 Fabric：施工状态

> 更新：2026-10-11 第二阶段（18 个提交，6be7e1f…ce5fc34）。
> 详细交接见 workspace 的 `BIAB-交接-第二阶段.md`（本文件是其工程内镜像，信息以交接文档为准）。

## 一、已完成（编译 0 错误、全量构建通过）

- **A 美术**：1.4.1 assets/data/vanilla_style 全量替换（哈希对账一致）+ 8 个空槽图标复制到 26.3 GUI 图集路径 `gui/sprites/container/slot/`
- **B 基础/数据/物品/配方**：1.4.1 配置系统、多级升级、旅人结、背包升级配方
- **D 规则/掉落/熔炉**：TabRules/WornToolFloor/BrokenTools/ArmorSwapService/LootTradeTables/FurnaceService
- **E 配装填充**：PlayerLoadout 1.4.1 枢纽、LoadoutFiller 2928 行、LoadoutReturnService、PlayerHooks、SelfTest+SelfTestFeatures、FabricCommonEvents=1.4.1 事件集、ModCommands
- **F-1a 客户端基础**：ClientNet/ClientSync/ClientTheme/RecipeViewers/FtbSidebarCompat/Polymorph×2/BetterCombatMixinPlugin/BetterCombatState/ClientCombatBridge/ClientHudState
- **F-1b 客户端接线**：BetterInventoryClient 1.4.1 入口、OffhandKey、ClientPlatform、ClientHooks、InventorySwap、CreativeInventory、ClientEvents、BetterInventoryPayloads（含 ToggleSafeEntity/SwapArmor/SetBackpackPage）
- **F-2a 渲染叶子类**：EffectRow/CompatButtons/ZoomRender/BetterInventoryHud
- **F-2b 客户端自检**：ClientSelfTest 1045 行（26.3 API 全 javap 核验）
- **F-2c 屏幕**：BackpackScreen/BetterInventoryScreen/BlockChooserScreen/SettingsScreen 1.4.1 化
- **F-3 菜单**：BackpackMenu/BetterInventoryMenu/SettingsMenu/TabStorage + slots（BackgroundSlot 新增、IconedSlot 删除、HandlerItemStore 适配）

## 二、待办（按优先级）

### F-2d — BackpackLayer（未开始）
26.3 移除 PlayerRenderer → AvatarRenderer，FeatureRenderer → 无状态 RenderLayer + EntityRenderState。要点：javap `net.minecraft.client.renderer.entity.layers.RenderLayer`、玩家渲染状态类、fabric `LivingEntityRenderLayerRegistrationCallback`、`ItemRenderer.renderItem` 26.3 签名；旧代码用 `((PlayerEntityModel) getContextModel()).body.rotate(pose)` 的躯干变换需找 26.3 等价物。注册调用点在 `BetterInventoryClient` 有 TODO。

### F-4 — 12 个新 mixin + mixins.json（未开始）
BundleContentsMixin、CreativeScreenMixin、ItemEntityPickupMixin、ItemInHandRendererMixin、MerchantMenuMixin、MinecraftScreenMixin、PlayerHandItemMixin、PlayerRendererMixin（目标类已移除，需重定向）、PlayerToolChoiceMixin、ServerPlayerDeathMixin + compat 两个（BetterCombatAttackHelperMixin/BetterCombatWeaponRegistryMixin，插件门控）。
**关键约束**：MixinExtras 不在离线缓存（@WrapMethod/@WrapOperation/@ModifyExpressionValue 不可用），必须改写为纯 mixin 构造（@Redirect/@Inject HEAD+RETURN 拆包）。

### G — 收尾
1. 村民交易：26.3 无 TradeOfferHelper，随机报价需 mixin 注入（工厂在 LootTradeTables：knotOffer/UpgradeForEmeralds.create）
2. vanilla_style 内置包注册：26.3 fabric-resource-loader 无 ResourceManagerHelper（查 BuiltInPackSource）
3. ClientSelfTest 接线：ClientEvents.register() 末尾 TODO
4. 维度切换同步：目前只靠 AFTER_RESPAWN，视需要补钩子
5. BundleContents.tryInsert 返回语义：实机自检确认
6. 推送：18 个提交领先 origin（git 代理 127.0.0.1:7897）

## 三、怎么跑

```bash
# 离线编译（JDK 25 已在机）
E:\TOOL\PC\gradle-9.8.0\bin\gradle.bat -p C:\Users\user\Documents\GitHub\BIAB-F-26.3 build --offline --console=plain --no-daemon
# 产物 build/libs/betterinventory-1.1.5.jar
```

实机验证：装 jar 进 `E:\GAME\MC\HMCL\.minecraft\mods\`（先关游戏）→ 启动进世界 → 读 `logs\latest.log` 查 mixin → 按 1.4.1 功能点验；可选 JVM 参数 `-Dbetterinventory.selftest=true` 跑登录自检。
