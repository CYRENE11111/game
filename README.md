# 实验四：包含 3 个 Activity 的翻牌游戏程序

**APP 标题（姓名 + 学号）：李飞乐2024110516**

## 一、程序结构

```
com.example.exp4
├── MainActivity.kt          主 Activity：显示玩家基本信息 + “关于”菜单
├── EditInfoActivity.kt      二级 Activity：修改个人信息
├── GameActivity.kt          二级 Activity：翻牌游戏（Controller）
├── model/                   游戏模型（Model）
│   ├── Card.kt              一张扑克牌：花色、点数、是否选中、是否配对 + 三张牌配对规则
│   ├── Deck.kt              一副 52 张的牌堆，支持随机抽牌
│   ├── CardMatchingGame.kt  三张牌配对游戏逻辑、计分与 reset()
│   └── Player.kt            玩家实体 + 内存中的玩家数据仓库
└── view/                    游戏界面（View）
    ├── CardView.kt          单张卡牌：牌面绘制 + 翻牌 / 配对动画（正方形）
    └── CardAdapter.kt       RecyclerView.Adapter 适配器 + 卡牌点击回调
```

## 二、界面设计

| 界面 | 布局要点 |
| --- | --- |
| 主界面 `main` | 头像框 + “姓名 / 性别 / 学号 / 电话 / 邮件地址 / 爱好 / 游戏最高分”七行信息 + 底部“修改个人信息”“翻牌游戏”两个按钮；顶部工具栏菜单含“关于”，点击弹出关于对话框 |
| 信息修改 `personinformation` | 姓名、学号、电话号码、电子邮箱地址四个输入框 + “性别”单选（男 / 女）+ “爱好”复选（旅游 / 阅读 / 游戏 / 购物）+ “保存 / 取消” |
| 翻牌游戏 `cardgame` | RecyclerView 网格：竖屏 4 列 × 6 行共 24 张牌；横屏改用 `layout-land` 布局，6 列 × 3 行共 18 张牌，右侧为状态与“重置游戏 / 退出”面板 |
| 关于对话框 | 显示版本号、作者、学号及游戏玩法说明 |

牌数通过资源限定符 `values/integers.xml`（24 张 / 4 列）与
`values-land/integers.xml`（18 张 / 6 列）配置，旋转屏幕后自动切换。

## 三、实验要求对应关系

| 实验要求 | 实现位置 |
| --- | --- |
| （1）主界面菜单 + 关于对话框 | `MainActivity.onCreateOptionsMenu / showAboutDialog`、`res/menu/menu_main.xml` |
| （2a）游戏界面 View：RecyclerView 实现 24 张牌，规整美观 | `GameActivity` + `activity_card_game.xml` + `view/CardView.kt` |
| （2b）适配器：派生自 `RecyclerView.Adapter`，重载方法并处理点击回调 | `view/CardAdapter.kt`（`onCreateViewHolder / onBindViewHolder / getItemCount`、`OnCardClickListener`） |
| （2c）游戏模型：`CardMatchingGame` 添加 `reset()` | `model/CardMatchingGame.kt` |
| （2d）游戏控制器：可玩游戏，横竖屏不同牌数 | `GameActivity`、`values(-land)/integers.xml`、`layout(-land)/activity_card_game.xml` |
| （5）三张牌游戏逻辑 | `CardMatchingGame.chooseCardAtIndex()`、`Card.match()` |

## 四、Model–View–Controller 的配合方式

- **Model**：`CardMatchingGame` 保存所有牌和分数，只负责游戏规则。
  三张牌玩法为“翻开三张点数相同的牌即配对成功”：配对成功 `+4 × 4` 分，
  配对失败 `-2` 分，每翻开一张牌 `-1` 分。
  发牌时按点数把整副牌分组，每个点数取三张（三种花色）作为一组，
  **保证每张牌都属于一个“三张同点数”组合，一局必定可以完成**。
  `reset()` 重新发牌并清零分数。
- **View**：`RecyclerView` 负责网格排版与视图复用；`CardAdapter` 根据模型状态绑定每张牌，
  `CardView` 自己绘制牌背纹理和“花色 + 点数”，并封装翻牌、配对高亮等动画，不包含任何游戏规则。
- **Controller**：`GameActivity` 实现 `CardAdapter.OnCardClickListener`，
  接收点击事件 → 调用 `game.chooseCardAtIndex()` → 通过适配器把模型状态同步回界面
  （分数、剩余未配对张数、状态提示、最高分），并提供“重置游戏”“退出”操作。

## 五、编译与运行

```bash
# 命令行编译 Debug 包
./gradlew assembleDebug
# 运行游戏模型的单元测试
./gradlew testDebugUnitTest
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`
也可以直接用 Android Studio 打开本工程，点击 Run 运行到模拟器或真机。

## 六、单元测试

`app/src/test/java/com/example/exp4/CardMatchingGameTest.kt` 共 5 个用例，覆盖：

1. 发牌数量与初始分数正确；
2. 每一张牌都属于一组“三张同点数”（重复 50 局验证）；
3. 全部配对成功后游戏结束，24 张牌全部配对成功时得分为 104 分；
4. 配对失败的扣分与牌的选中状态；
5. `reset()` 后分数、选中状态与未配对数量被重置。
