# Oppo/Realme MT6853 Series Universal 4.14 Kernel Automated Build Script
[![Telegram](https://ziadoua.github.io/m3-Markdown-Badges/badges/Telegram/telegram2.svg)](https://t.me/mt6853o) 
[![QQ Group](https://img.shields.io/badge/QQ-1098718272-blue?style=flat&logo=%3Csvg%20role%3D%22img%22%20viewBox%3D%220%200%2024%2024%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Ctitle%3EQQ%3C%2Ftitle%3E%3Cpath%20d%3D%22M21.395%2015.035a40%2040%200%200%200-.803-2.264l-1.079-2.695c.001-.032.014-.562.014-.836C19.526%204.632%2017.351%200%2012%200S4.474%204.632%204.474%209.241c0%20.274.013.804.014.836l-1.08%202.695a39%2039%200%200%200-.802%202.264c-1.021%203.283-.69%204.643-.438%204.673.54.065%202.103-2.472%202.103-2.472%200%201.469.756%203.387%202.394%204.771-.612.188-1.363.479-1.845.835-.434.32-.379.646-.301.778.343.578%205.883.369%207.482.189%201.6.18%207.14.389%207.483-.189.078-.132.132-.458-.301-.778-.483-.356-1.233-.646-1.846-.836%201.637-1.384%202.393-3.302%202.393-4.771%200%200%201.563%202.537%202.103%202.472.251-.03.581-1.39-.438-4.673%22%2F%3E%3C%2Fsvg%3E&logoColor=%231EBAFC&logoSize=auto&label=QQ%E7%BE%A4&labelColor=%2312B7F5&color=%23FFFFFF&cacheSeconds=3600&link=https%3A%2F%2Fqm.qq.com%2Fq%2FYF1CEUBJ0O
)](https://qm.qq.com/q/UbZQsSlIk2)

## android_kernel_4.14_MT6853_Action
>[!note]
>Supported Devices: MT6853 OPPO/Realme OPPO A72, OPPO A53, OPPO K7x, OPPO Reno4SE, Realme Q2, Realme V15, Realme Q2 Pro, OPPO A95, Realme V5 5G, Realme X7 5G Android 12
>
>Version: 4.14.186
| Make sure to upgrade to Android 12 before flashing this kernel
>
***

## Kernel Basic Information
Version: 4.14.186+

Android Version: Android 12

Supported Root Managers:
 - RKSU
 - BakaSU
***

## How to Use?
 - Star this project (it's really, really important to me ><)
 - Fork this project (you can just fork the `main` branch)
 - Click the `Actions` button at the top of the GitHub page
 - Click on the `Build Kernel for MT6853` section
 - Select your root manager and features, then click `Run workflow`
 - Wait for the build to complete and flash it~

 ## How to Flash?
 * First, make sure to root your phone and upgrade the system to Android 12
    * Flash it via recovery
      
      or
    * First, use Magisk to `root` your phone, then use a kernel flashing tool to flash it

      For example, `Franco Kernel Manager`

***

## Features:

Upstream:
- [x] lxc
- [x] docker
- [x] kvm

This Project:
- [x] Compatible with the latest [RKSU](https://github.com/rsuntk/KernelSU)
- [x] Built-in [BakaSU](https://github.com/Baka-SU/BakaSU)
- [x] Open Workshop
     - [x] BBR Acceleration
     - [x] SUSFS-V2.3.0
     - [x] [DroidSpaces](https://github.com/ravindu644/Droidspaces-OSS)
     - [x] Tombstone - [Cirno](https://cirno.shrairo.top/)
     - [x] [NoMount](https://github.com/maxsteeel/nomount)
     - [x] Fixed several CVE vulnerabilities --CVE-2026-43499 & CVE-2022-29581
     - [ ] [BBG](https://github.com/vc-teahouse/Baseband-guard) anti-brick protection
     - [ ] And more...
- [x] Patched bpf
- [x] Automatically published to the [TG Channel](https://t.me/mt6853o)

***
## Acknowledgments
[RKSU](https://github.com/rsuntk/KernelSU) – A KernelSU branch specifically for `no-GIK` kernels

[BakaSU](https://github.com/Baka-SU/BakaSU) – A branch of KernelSU

[NonGKI_Kernel_Build_2nd](https://github.com/JackA1ltman/NonGKI_Kernel_Build_2nd) – A project focused on No-GKI kernel compilation

[Kernel-patch](https://github.com/sanba0519/Kernel-patch) – My own patch file repository

[android_kernel_mtk6853](https://github.com/wu17481748/android_kernel_mtk6853) – The upstream project; without it, this project wouldn't exist

[android_kernel_oplus_mt6853](https://github.com/momo54181/android_kernel_oplus_mt6853) – Thanks to momo for their help

[oppo_kernel_mt6885-mt6853-clean](https://github.com/Winkmoon/oppo_kernel_mt6885-mt6853-clean) – Thanks to xinran for their help, hehehehehe
### Thanks to the Linux Kernel Community – It's because of you and the open-source community that we have today's prosperity

<a href="https://github.com/sanba0519/OPPO-Realme_kernel_4.14_MT6853/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=sanba0519/OPPO-Realme_kernel_4.14_MT6853" />
</a>