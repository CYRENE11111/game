# 实验四：包含 3 个 Activity 的翻牌游戏程序

**APP 标题（姓名 + 学号）：李飞乐2024110516**

## 一、程序结构

```
com.example.exp4
├── MainActivity.kt          主 Activity：显示玩家基本信息
├── EditInfoActivity.kt      二级 Activity：修改个人信息
├── GameActivity.kt          二级 Activity：翻牌游戏（Controller）
├── model/                   游戏模型（Model，参照附件代码实现）
│   ├── Card.kt              一张扑克牌：花色、点数、是否选中、是否配对
│   ├── Deck.kt              一副 52 张的牌堆，支持随机抽牌
│   ├── CardMatchingGame.kt  翻牌配对游戏逻辑与计分
│   └── Player.kt            玩家实体 + 内存中的玩家数据仓库
└── view/                    游戏界面（View）
    ├── CardView.kt          单张卡牌：牌面绘制 + 翻牌 / 配对动画
    └── CardBoardView.kt     卡牌网格布局（4 列 × 4 行，正方形卡牌居中）
```

## 二、界面设计（参照《翻牌游戏.pdf》）

| 界面 | 布局要点 |
| --- | --- |
| 主界面 `main` | 头像框 + “姓名 / 性别 / 学号 / 电话 / 邮件地址 / 爱好 / 游戏最高分”七行信息 + 底部“修改个人信息”“翻牌游戏”两个按钮 |
| 信息修改 `personinformation` | 姓名、学号、电话号码、电子邮箱地址四个输入框 + “性别”单选（男 / 女）+ “爱好”复选（旅游 / 阅读 / 游戏 / 购物）+ “保存 / 取消” |
| 翻牌游戏 `cardgame` | 4 列 × 4 行共 16 张牌，底部为“得分：<分数>”和“重置游戏 / 退出” |

游戏界面的牌数、行列与字段名称均按 PDF 中的尺寸与文案实现。

## 三、实验要求对应关系

| 实验要求 | 实现位置 |
| --- | --- |
| （1）主 Activity 显示姓名、性别、电话、当前最高分 | `MainActivity` + `activity_main.xml` |
| （2）信息修改 Activity（修改后不保存到文件） | `EditInfoActivity` + `activity_edit_info.xml` |
| （3a）游戏界面 View：多张牌、规整美观 | `view/CardView.kt`、`view/CardBoardView.kt`、`activity_card_game.xml` |
| （3b）游戏模型 Model：参照附件代码 | `model/Card.kt`、`model/Deck.kt`、`model/CardMatchingGame.kt` |
| （3c）控制器 Controller：接受交互、处理模型、同步界面、重新开始 | `GameActivity` |
| APP 标题为姓名 + 学号 | `res/values/strings.xml` 中的 `app_name` |

## 三、Model–View–Controller 的配合方式

- **Model**：`CardMatchingGame` 保存所有牌和分数，只负责游戏规则。
  计分规则与附件一致：配对成功 `+4 × 4` 分，配对失败 `-2` 分，每翻开一张牌 `-1` 分。
  发牌时先随机抽出若干张牌再复制成“对”后洗牌，**保证每张牌都有配对牌，一局必定可以完成**。
- **View**：`CardBoardView` 把 `CardView` 排成规整网格；`CardView` 自己绘制牌背纹理和“花色 + 点数”，
  并封装翻牌、配对高亮等动画，不包含任何游戏规则。
- **Controller**：`GameActivity` 接收点击事件 → 调用 `game.chooseCardAtIndex()` → 把模型状态同步回界面
  （分数、剩余未配对张数、状态提示、最高分），并提供“重置游戏”按钮重新发牌、清零分数，
  “退出”按钮结束当前界面返回主界面。

## 四、编译与运行

```bash
# 命令行编译 Debug 包
./gradlew assembleDebug
# 运行游戏模型的单元测试
./gradlew testDebugUnitTest
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`
也可以直接用 Android Studio 打开本工程，点击 Run 运行到模拟器或真机。

## 五、单元测试

`app/src/test/java/com/example/exp4/CardMatchingGameTest.kt` 共 5 个用例，覆盖：

1. 发牌数量与初始分数正确；
2. 每一张牌都恰好有一个配对牌（重复 50 局验证）；
3. 全部配对成功后游戏结束，16 张牌全部配对成功时得分为 112 分；
4. 配对失败的扣分与牌的选中状态；
5. 重新开始后分数与未配对数量被重置。
