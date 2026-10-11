# BetterInventory → Minecraft 26.3 Fabric：施工状态

> 更新：2026-10-11 第三阶段（F-2d / F-4 / G 全部完成，21 个提交，6be7e1f…a157c8d，已推送）。
> 详细交接见 workspace 的 `BIAB-交接-第二阶段.md`（本文件是其工程内镜像，信息以交接文档为准）。

## 一、已完成（编译 0 错误 0 警告、全量构建通过）

- **A 美术**：1.4.1 assets/data/vanilla_style 全量替换（哈希对账一致）+ 8 个空槽图标复制到 26.3 GUI 图集路径 `gui/sprites/container/slot/`
- **B 基础/数据/物品/配方**：1.4.1 配置系统、多级升级、旅人结、背包升级配方
- **D 规则/掉落/熔炉**：TabRules/WornToolFloor/BrokenTools/ArmorSwapService/LootTradeTables/FurnaceService
- **E 配装填充**：PlayerLoadout 1.4.1 枢纽、LoadoutFiller 2928 行、LoadoutReturnService、PlayerHooks、SelfTest+SelfTestFeatures、FabricCommonEvents=1.4.1 事件集、ModCommands
- **F-1a 客户端基础**：ClientNet/ClientSync/ClientTheme/RecipeViewers/FtbSidebarCompat/Polymorph×2/BetterCombatMixinPlugin/BetterCombatState/ClientCombatBridge/ClientHudState
- **F-1b 客户端接线**：BetterInventoryClient 1.4.1 入口、OffhandKey、ClientPlatform、ClientHooks、InventorySwap、CreativeInventory、ClientEvents、BetterInventoryPayloads（含 ToggleSafeEntity/SwapArmor/SetBackpackPage）
- **F-2a 渲染叶子类**：EffectRow/CompatButtons/ZoomRender/BetterInventoryHud
- **F-2b 客户端自检**：ClientSelfTest 1045 行（26.3 API 全 javap 核验）+ ClientEvents 接线（`-Dbetterinventory.selftest=true`）
- **F-2c 屏幕**：BackpackScreen/BetterInventoryScreen/BlockChooserScreen/SettingsScreen 1.4.1 化
- **F-2d 渲染层**：BackpackLayer 按 26.3 无状态 RenderLayer 重写（AvatarRenderState + ItemModelResolver/ItemStackRenderState，躯干变换 `ModelPart.translateAndRotate`），经 fabric `LivingEntityRenderLayerRegistrationCallback` 注册（EntityTypes.PLAYER）
- **F-3 菜单**：BackpackMenu/BetterInventoryMenu/SettingsMenu/TabStorage + slots（BackgroundSlot 新增、IconedSlot 删除、HandlerItemStore 适配）
- **F-4 mixin**：12 个 1.4.1 新 mixin 全部按 26.3 字节码重定位（MixinExtras 不可用，@WrapOperation/@WrapMethod/@ModifyExpressionValue → @Redirect 或 @Inject HEAD+RETURN 拆包）；compat 两个挂独立 mixins.json + BetterCombatMixinPlugin 门控；MerchantMenuMixin 跳过（26.3 autofill→tryMoveItems 按 ItemCost 需求量填充，无 getMaxStackSize 锚点）；删除 1.4.1 不存在的 GuiSetScreenMixin/KeyboardKeyPressMixin/InventoryModeToggle/ScreenMode 配置
- **G 收尾**：村民/流浪商人随机报价（VillagerTradesMixin/WanderingTraderTradesMixin 在 updateTrades 尾部注入）、vanilla_style 内置包注册（26.3 版 ResourceManagerHelper 仍在）、维度切换同步（ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL）、推送（21 提交，本地=远端 a157c8d）

## 二、待办（只剩实机验证）

1. 实机装 jar 进 `E:\GAME\MC\HMCL\.minecraft\mods\`（先关游戏，替换前校验哈希+留备份）→ 启动进世界 → 读 `logs\latest.log` 查 mixin 失败/崩溃 → 按 1.4.1 功能点验
2. 可选：JVM 参数 `-Dbetterinventory.selftest=true` 跑登录自检（PASS/FAIL 打日志、截图存游戏目录）
3. 实机确认两处语义：BundleContents.tryInsert 返回语义（SelfTestFeatures 束带场景）、背包背挂的朝向/缩放观感
4. 若 bettercombat 未装：日志应有 "Better Combat patch dormant"；装了才应用 compat mixin

## 三、怎么跑

```bash
# 离线编译（JDK 25 已在机）
E:\TOOL\PC\gradle-9.8.0\bin\gradle.bat -p C:\Users\user\Documents\GitHub\BIAB-F-26.3 build --offline --console=plain --no-daemon
# 产物 build/libs/betterinventory-1.1.5.jar
```

实机验证：装 jar 进 `E:\GAME\MC\HMCL\.minecraft\mods\`（先关游戏）→ 启动进世界 → 读 `logs\latest.log` 查 mixin → 按 1.4.1 功能点验；可选 JVM 参数 `-Dbetterinventory.selftest=true` 跑登录自检。
