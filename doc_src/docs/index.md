---
layout: post
title:  "应用介绍"
summary: "Thanox工作原理"
date:   2018-07-20 15:50:00
categories: jekyll
---

<!-- more -->



## 开源计划

目前Thanox代码约80%已经公开在[Github](https://github.com/Tornaco/Thanox/tree/master/android)上。

## 版本发布

Thanox目前在酷安和Google play商店发布。

* [酷安](https://www.coolapk.com/)发布的版本为国内稳定版，如果要体验测试版，请前往[Github-Release](https://github.com/Tornaco/Thanox/releases)页面下载

* [Google play](https://play.google.com/store/apps/details?id=github.tornaco.android.thanos.pro&hl=en&gl=US)发布的版本为pro版本，功能与国内版本基本一致；得益于Google play的便利性，你可以选择加入Beta计划体验pro的测试版本

## 工作原理

**Thanox**整体架构分两层，分别是**App**层，**Framework**层。

* **Framework**层运行于`system_server`进程，负责核心管理逻辑，拥有system级别的权限。
* **App**层仅是一个普通的应用，负责为用户提供UI交互。

### 架构图



![thanox-arch](assets/images/thanox-arch.png)





## 数据存储

由于**Thanox**整体架构分两层，其数据也分两部分存储。

* **Framework**层的数据存储在`/data/system/thanos${16位随机字母}`下，Thanox各个功能的数据也存在此处。
* **App**层仅仅存储一些简单的UI配置数据，使用系统设置清除数据并不会清除Thanox各个功能的数据。

如果想要卸载**Thanox**模块并清除其所有数据，可以前往**Thanox**的*设置-备份与还原-立即卸载*。

如果依然无法删除，请在卸载Thanox应用后，手动删除`/data/system/thanos_${xxxxxx}`目录。