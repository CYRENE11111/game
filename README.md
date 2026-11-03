# 实验：使用 Fragment 重构的翻牌游戏程序

**APP 标题（姓名 + 学号）：李飞乐2024110516**

## 一、程序结构

```
com.example.exp4
├── MainActivity.kt                 唯一的 Activity：工具栏 + Fragment 容器 + 底部导航
├── model/                          游戏模型（Model）
│   ├── Card.kt                     一张扑克牌（Serializable）：花色、点数、选中 / 配对状态
│   ├── Deck.kt                     一副 52 张的牌堆，支持随机抽牌
│   ├── CardMatchingGame.kt         配对游戏逻辑（Serializable），支持 2 张 / 3 张配对
│   └── Player.kt                   玩家实体（Serializable）
├── storage/                        序列化持久化
│   ├── ObjectFileStore.kt          openFileOutput / openFileInput + ObjectStream 读写对象
│   ├── PlayerRepository.kt         玩家信息仓库：player.dat
│   └── GameStore.kt                游戏进度快照：game_two_card.dat / game_three_card.dat
├── ui/                             Fragment（Controller）
│   ├── ProfileFragment.kt          第 1 个 Fragment：个人信息展示 + 修改
│   ├── GameFragment.kt             翻牌游戏公共实现（点击处理、计分、进度保存）
│   ├── TwoCardGameFragment.kt      第 2 个 Fragment：2 张牌翻牌游戏
│   └── ThreeCardGameFragment.kt    第 3 个 Fragment：3 张牌翻牌游戏
└── view/                           游戏界面（View）
    ├── CardView.kt                 单张卡牌：牌面绘制 + 翻牌 / 配对动画（正方形）
    └── CardAdapter.kt              RecyclerView.Adapter + 卡牌点击回调
```

## 二、界面设计

| 界面 | 实现 |
| --- | --- |
| 个人信息（Fragment 1） | 头像 + “姓名 / 性别 / 学号 / 电话 / 邮件地址 / 爱好 / 游戏最高分”；点击“修改个人信息”切换到表单，保存后写入文件 |
| 2 张牌翻牌（Fragment 2） | 16 张牌（8 组两张同点数）；竖屏 4 列、横屏 8 列；两张点数相同即配对成功 |
| 3 张牌翻牌（Fragment 3） | 24 张牌（8 组三张同点数）；竖屏 4 列、横屏 8 列；三张点数相同即配对成功 |
| 底部导航 | `BottomNavigationView` + `FragmentContainerView`，三个 Fragment 之间切换 |
| 关于对话框 | 顶部工具栏菜单，显示版本号、作者、学号与玩法说明 |

牌阵使用 `RecyclerView` + `GridLayoutManager`，列数由 `values(-land)/integers.xml` 配置；
`CardView` 在 `onMeasure` 中把每张牌约束为正方形，因此横竖屏都能保持规整的网格。

## 三、序列化存储（退出后恢复）

| 存储内容 | 文件 | 写入时机 | 恢复位置 |
| --- | --- | --- | --- |
| 个人信息 + 历史最高分 | `player.dat` | 点击“保存”时；刷新最高分时 | `MainActivity.onCreate` → `PlayerRepository.load()` |
| 2 张牌游戏进度 | `game_two_card.dat` | 每次翻牌 / 重置 / Fragment 暂停时 | `TwoCardGameFragment.onViewCreated()` |
| 3 张牌游戏进度 | `game_three_card.dat` | 每次翻牌 / 重置 / Fragment 暂停时 | `ThreeCardGameFragment.onViewCreated()` |

- 写文件：`Context.openFileOutput()` + `ObjectOutputStream.writeObject()`；
- 读文件：`Context.openFileInput()` + `ObjectInputStream.readObject()`；
- 写入对象：`Player`、`Card`、`CardMatchingGame`、`SavedGame` 均实现 `java.io.Serializable`；
- 游戏对象保存了牌序、每张牌的选中 / 配对状态与当前得分，因此重新打开 APP 后
  可以直接继续上一局，页面状态栏会提示“已恢复上次保存的游戏进度”。

## 四、实验要求对应关系

| 实验要求 | 实现位置 |
| --- | --- |
| 至少 3 个 Fragment：个人信息、2 张牌游戏、3 张牌游戏 | `ui/ProfileFragment.kt`、`ui/TwoCardGameFragment.kt`、`ui/ThreeCardGameFragment.kt` |
| 个人信息序列化存储 | `PlayerRepository` + `ObjectFileStore`（`player.dat`） |
| 翻牌游戏序列化存储 | `GameStore` + `GameFragment.saveProgress()`（两个独立数据文件） |
| 读 / 写文件 API | `ObjectFileStore` 使用 `openFileInput` / `openFileOutput` |
| 读写对象 API | `ObjectInputStream` / `ObjectOutputStream`，对象派生自 `Serializable` |
| RecyclerView / Adapter / 游戏模型 | `view/CardView.kt`、`view/CardAdapter.kt`、`model/CardMatchingGame.kt` |

## 五、Model–View–Controller 的配合方式

- **Model**：`CardMatchingGame` 保存所有牌与分数，只负责游戏规则。
  配对成功一组 `+16` 分，配对失败 `-2` 分，每翻开一张牌 `-1` 分。
  发牌时按点数分组、每组随机抽取 2 或 3 张不同花色的牌，
  **保证每张牌都属于一个同点数组合，一局必定可以完成**。
- **View**：`RecyclerView` 负责网格排版与视图复用；`CardAdapter` 根据模型状态绑定每张牌，
  `CardView` 绘制牌背纹理和“花色 + 点数”，并封装翻牌、配对高亮动画。
- **Controller**：`GameFragment` 实现 `CardAdapter.OnCardClickListener`，
  接收点击 → 调用 `game.chooseCardAtIndex()` → 通过适配器同步界面，
  并在每次操作后把游戏对象写入文件。

## 六、编译与运行

```bash
# 命令行编译 Debug 包
./gradlew assembleDebug
# 运行游戏模型的单元测试
./gradlew testDebugUnitTest
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`
也可以直接用 Android Studio 打开本工程，点击 Run 运行到模拟器或真机。

## 七、单元测试

`app/src/test/java/com/example/exp4/CardMatchingGameTest.kt` 共 7 个用例，覆盖：

1. 发牌数量与初始分数正确；
2. 每一张牌都属于一组“三张同点数”（重复 50 局验证）；
3. 三张牌玩法全部配对后游戏结束，24 张牌得分为 104 分；
4. 配对失败的扣分与牌的选中状态；
5. `reset()` 后分数、选中状态与未配对数量被重置；
6. 两张牌玩法全部配对后游戏结束，16 张牌得分为 112 分；
7. 游戏对象经对象流序列化再反序列化后进度完全一致（对应文件持久化）。
