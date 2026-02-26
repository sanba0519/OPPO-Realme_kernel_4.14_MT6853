# android_kernel_OPPO_mtk6853_PERM00
专为OPPOK7X（尚未测试realme机型）编译的内核并适配sukisu-ultra ~SUSFS有点难就不做了~[![Build OppoK7x (PERM00) (Color A12)（MT6853）](https://github.com/sanba0519/android_kernel_4.14_MT6853_PERM00/actions/workflows/build-oppo-k7x-color-a12-PERM00.yml/badge.svg)](https://github.com/sanba0519/android_kernel_4.14_MT6853_PERM00/actions/workflows/build-oppo-k7x-color-a12-PERM00.yml)


*适用:*

系统版本:安卓12

内核版本:4.14.186

处理器:天玑720/天玑800u(尚未测试） 代号mtk6853

***

如何构建？

1.fork本项目

2.点击Action 运行工作流程

Build OppoK7x (PERM00) (Color A12)（MT6853）是集成了SukiSU　~~但是不能用 只能开机~~

编译OPPO/Realme-安卓12-通用mt6853机型内核 支持ksu docker lxc kvm Kali-Nethunter是可以正常使用的 你需要下载[V0.95](https://github.com/tiann/KernelSU/releases/tag/v0.9.5)的KernelSU 来使用

3.等待构建完成 打开编译好的AK3 并解压进去 打开anykernel.sh 搜索BLOCK=字段 找到那一行并改成`BLOCK=/dev/block/by-name/boot;`

4. 在TWRP刷入食用

~~enjoy it!~~

###
尚未开发完全 敬请期待

~你可以通过关注[TG频道](https://t.me/oppok7x)来关注开发者动态~
***

~~实在不知道为啥 刷入后sukisu不显示运行中 原作者已弃坑 接下来交给[@Eridian1628](https://github.com/Eridian1628)接手~~~

~~`后会有期`~~

###

已集成[RKSU](https://github.com/rsuntk/KernelSU) 源码可见另一个仓库（也是我的）https://github.com/Eridian1628/kernel_mtk6853_RKSU
