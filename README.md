# 欧加真 MT6853 系列通用4.14内核自动化编译脚本
[![Telegram](https://ziadoua.github.io/m3-Markdown-Badges/badges/Telegram/telegram2.svg)](https://t.me/mt6853o) 
[![QQ群](https://img.shields.io/badge/QQ-1076982460-blue?style=flat&logo=%3Csvg%20role%3D%22img%22%20viewBox%3D%220%200%2024%2024%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ctitle%3EQQ%3C%2Ftitle%3E%3Cpath%20d%3D%22M21.395%2015.035a40%2040%200%200%200-.803-2.264l-1.079-2.695c.001-.032.014-.562.014-.836C19.526%204.632%2017.351%200%2012%200S4.474%204.632%204.474%209.241c0%20.274.013.804.014.836l-1.08%202.695a39%2039%200%200%200-.802%202.264c-1.021%203.283-.69%204.643-.438%204.673.54.065%202.103-2.472%202.103-2.472%200%201.469.756%203.387%202.394%204.771-.612.188-1.363.479-1.845.835-.434.32-.379.646-.301.778.343.578%205.883.369%207.482.189%201.6.18%207.14.389%207.483-.189.078-.132.132-.458-.301-.778-.483-.356-1.233-.646-1.846-.836%201.637-1.384%202.393-3.302%202.393-4.771%200%200%201.563%202.537%202.103%202.472.251-.03.581-1.39-.438-4.673%22%2F%3E%3C%2Fsvg%3E&logoColor=%231EBAFC&logoSize=auto&label=QQ%E7%BE%A4&labelColor=%2312B7F5&color=%23FFFFFF&cacheSeconds=3600&link=https%3A%2F%2Fqm.qq.com%2Fq%2FYF1CEUBJ0O
)](https://qm.qq.com/q/YF1CEUBJ0O)

## android_kernel_4.14_MT6853_Action
>[!note]
>适用机型：MT6853 OPPO/Reamle OPPO A72, OPPO A53, OPPO K7x, OPPO Reno4SE, Realme Q2, Realme V15, Realme Q2 pro, OPPO A95, Realme V5 5G, Realme X7 5G Android 12 RKSU kernel
>
>版本：4.14.186
| 务必升级到安卓12再刷入此内核
>
***

## 内核基本信息
版本：4.14.186+

安卓版本：Android12

支持的Root管理器：
 - RKSU
 - ReSukiSU
***

## 如何使用？
 - star本项目（对我真的真的很重要><）
 - fork本项目（可以只fork `main` 分支）
 - 点击 GitHub顶栏的 `Action` 按钮
 - 点击 `Build Kernel for MT6853` 一栏
 - 选择Root管理器以及功能 点击`Run workflow`
 - 等待构建完成并刷入~

 ## 如何刷入？
 * 首先务必Root你的手机并升级系统至安卓12
    * 在rec刷入
      
      or
    * 先用Magisk `root`你的手机 然后使用刷写内核工具刷入

      比如`Franco Kernel Manager`

***

TO DO:
- [x] 适配最新版[RKSU](https://github.com/rsuntk/KernelSU)
- [x] 内置[Re:SukiSU](https://github.com/ReSukiSU/ReSukiSU)
- [x] 开放创意工坊
     - [x] BBR加速
     - [x] SUSFS-V2.2.0
     - [x] [DroidSpaces](https://github.com/ravindu644/Droidspaces-OSS)
     - [ ] [BBG](https://github.com/vc-teahouse/Baseband-guard)防格机
     - [ ] So on...
- [x] 修补bpf
- [x] 自动发布到[TG频道](https://t.me/mt6853o)

***
## 鸣谢
[NonGKI_Kernel_Build_2nd](https://github.com/JackA1ltman/NonGKI_Kernel_Build_2nd) 一个专注于No-GKI内核编译的项目

[android_kernel_oplus_mt6853](https://github.com/momo54181/android_kernel_oplus_mt6853) 感谢momo佬的帮助

### 感谢Linux内核社区 正是因为你们开源社区才有了今天的繁荣

<a href="https://github.com/sanba0519/OPPO-Realme_kernel_4.14_MT6853/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=sanba0519/OPPO-Realme_kernel_4.14_MT6853" />
</a>

