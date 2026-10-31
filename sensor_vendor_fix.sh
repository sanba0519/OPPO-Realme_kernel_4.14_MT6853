#!/system/bin/sh
# =============================================================================
# sensor_vendor_fix.sh — SensorService 虚拟传感器 vendor 一键改写 (MT6853 通用)
#
# 背景:
#   当 sensors HAL 未提供 融合传感器(重力/线性加速/旋转矢量/游戏RV/地磁RV/方向)时,
#   Android 框架的 SensorService 会用 vendor="AOSP" 注册兜底虚拟传感器(共 6 个),
#   外加 2 个无条件注册的调试传感器(Corrected Gyroscope / Gyroscope Bias)。
#   风控 SDK 的 "Miscellaneous Check(5)" 会统计 vendor 含 "AOSP"(不分大小写)的
#   传感器数量,>3 即判定为模拟器/云手机 —— 真机 + 自制内核/精简 HAL 很容易中招。
#
# 原理:
#   将 /system/lib64(或 lib)/libsensorservice.so 中的 4 字节字面量 "AOSP"
#   等长替换为 4 字节的 "MTK!"(链接器将所有相同字面量合并为一份,一次替换全部生效),
#   通过 KernelSU/RKSU/BakaSU/NoMount 模块 systemless 挂载,不动 system 分区。
#
# 用法:
#   sh sensor_vendor_fix.sh [apply|restore|status]
#     apply    打补丁并生成/更新模块(默认)
#     restore  移除模块(重启后恢复原状)
#     status   查看当前状态
#
# 环境变量(可选,一般不用管):
#   SVF_TARGET      替换目标字符串,默认 "MTK!",必须恰好 4 字节且不含 / 和空格
#   SVF_LIB64       覆盖 64 位 so 路径(测试用)
#   SVF_LIB32       覆盖 32 位 so 路径(测试用)
#   SVF_MODULE_ROOT 覆盖模块根目录(测试用)
#   SVF_WORK        覆盖工作目录(测试用)
#
# 注意:
#   - OTA/大版本升级后需 restore -> 重启 -> 重新 apply(so 会随 ROM 更新)
#   - 还原: sh sensor_vendor_fix.sh restore 然后重启
#   - SELinux 标签会在生成模块时继承原文件,并写入 post-fs-data.sh 兜底
# =============================================================================

# ---------- 可调参数 ----------
TARGET=${SVF_TARGET:-"MTK!"}
LIB64=${SVF_LIB64:-"/system/lib64/libsensorservice.so"}
LIB32=${SVF_LIB32:-"/system/lib/libsensorservice.so"}
MODROOT=${SVF_MODULE_ROOT:-"/data/adb/modules"}
WORK=${SVF_WORK:-"/data/local/tmp/sensor_vendor_fix"}
MOD_ID="sensor_vendor_fix"
MOD_DIR="$MODROOT/$MOD_ID"

CMD=${1:-apply}

# ---------- 工具函数 ----------
msg()  { printf "[*] %s\n" "$1"; }
ok()   { printf "[✓] %s\n" "$1"; }
warn() { printf "[!] %s\n" "$1"; }
die()  { printf "[✗] %s\n" "$1"; exit 1; }

# 统计文件中包含某字符串的"行数"(strings 归一化二进制;足够用于校验)
count_lines() {
    strings "$1" 2>/dev/null | grep -c "$2"
}

# 取文件的 SELinux 标签;取不到则回退默认值
get_label() {
    L=$(ls -Z "$1" 2>/dev/null | awk '{print $1}')
    case "$L" in
        u:object_r:*) echo "$L" ;;
        *) echo "u:object_r:system_lib_file:s0" ;;
    esac
}

validate_target() {
    case "$TARGET" in
        *[!/A-Za-z0-9!-]*) die "目标字符串含非法字符: $TARGET" ;;
    esac
    TLEN=$(printf "%s" "$TARGET" | wc -c)
    [ "$TLEN" -eq 4 ] || die "目标字符串必须恰好 4 字节(当前: $TARGET = $TLEN)"
    # 目标本身不能包含大小写不敏感的 aosp,否则自欺欺人
    LC=$(printf "%s" "$TARGET" | tr "A-Z" "a-z")
    case "$LC" in
        *aosp*) die "目标字符串不能包含 aosp" ;;
    esac
}

# 补丁单个 so: $1=源路径 $2=工作文件名前缀
# 输出状态: patched / already / clean / error:<原因>
patch_one() {
    SRC="$1"
    PRE="$2"
    [ -f "$SRC" ] || { echo "error:notfound"; return; }

    ORIG="$WORK/$PRE.orig"
    PATCHED="$WORK/$PRE.patched"
    cp -f "$SRC" "$ORIG" || { echo "error:copy"; return; }

    A1=$(count_lines "$ORIG" "AOSP")
    if [ "$A1" -eq 0 ]; then
        # 源里已经没有 AOSP:可能已被本模块挂载,或 ROM 本身无此字面量
        T0=$(count_lines "$ORIG" "$TARGET")
        if [ "$T0" -ge 1 ]; then echo "already"; else echo "clean"; fi
        return
    fi

    sed "s/AOSP/$TARGET/g" "$ORIG" > "$PATCHED" || { echo "error:sed"; return; }

    S1=$(stat -c %s "$ORIG" 2>/dev/null)
    S2=$(stat -c %s "$PATCHED" 2>/dev/null)
    A2=$(count_lines "$PATCHED" "AOSP")
    T2=$(count_lines "$PATCHED" "$TARGET")
    DIFFN=$(cmp -l "$ORIG" "$PATCHED" 2>/dev/null | wc -l)

    echo "patch_one: AOSP行 $A1->${A2} 目标行 $T2 尺寸 $S1->$S2 差异字节 $DIFFN" >&2

    if [ "$S1" != "$S2" ]; then echo "error:size"; return; fi
    if [ "$A2" -ne 0 ]; then echo "error:residual"; return; fi
    if [ "$T2" -lt "$A1" ]; then echo "error:targetcount"; return; fi
    if [ "$DIFFN" -lt 4 ] || [ "$DIFFN" -gt 512 ]; then echo "error:diff"; return; fi
    [ $((DIFFN % 4)) -eq 0 ] || { echo "error:diffalign"; return; }

    echo "patched"
}

# ---------- 动作 ----------
do_apply() {
    validate_target
    [ "$(id -u)" = "0" ] || warn "建议以 root 运行(当前 uid=$(id -u))"
    rm -rf "$WORK"; mkdir -p "$WORK" || die "无法创建工作目录 $WORK"

    STAMP=$(date "+%Y-%m-%d %H:%M:%S")
    msg "Sensor Vendor Fix | $STAMP"
    msg "目标替换: AOSP -> $TARGET"

    LIBS=""        # 已成功补丁的 "工作文件名:目标system路径" 列表
    ANY=0          # 是否至少处理了一个 so
    ALREADY=0

    for PAIR in "64:$LIB64" "32:$LIB32"; do
        BIT=${PAIR%%:*}
        SRC=${PAIR#*:}
        [ -f "$SRC" ] || { msg "跳过 ${BIT} 位库(不存在): $SRC"; continue; }
        ANY=$((ANY + 1))
        RES=$(patch_one "$SRC" "lib$BIT")
        msg "lib$BIT: $RES"
        case "$RES" in
            patched)
                LIBS="$LIBS lib$BIT.patched:$SRC"
                ;;
            already) ALREADY=$((ALREADY + 1)) ;;
            clean)   msg "lib$BIT: 源文件无 AOSP 字面量(ROM 已无此特征),跳过" ;;
            error:*) die "lib$BIT 补丁失败($RES),未写入任何内容,请把输出发给作者" ;;
        esac
    done

    [ "$ANY" -gt 0 ] || die "两个路径都不存在?请确认设备架构"

    if [ "$ALREADY" -ge "$ANY" ] && [ -z "$LIBS" ]; then
        if [ -d "$MOD_DIR" ]; then
            ok "源文件已是补丁状态且模块在位 —— 无需操作(如 OTA 后异常,先 restore 再 apply)"
        else
            warn "源文件无 AOSP 且无本模块 —— 无需操作"
        fi
        exit 0
    fi

    if [ -z "$LIBS" ]; then
        warn "没有需要补丁的文件"
        exit 0
    fi

    # 生成模块
    msg "生成模块: $MOD_DIR"
    rm -rf "$MOD_DIR"
    mkdir -p "$MOD_DIR" || die "无法创建模块目录 $MOD_DIR"
    PFD="$MOD_DIR/post-fs-data.sh"
    printf "#!/system/bin/sh\n" > "$PFD"

    for PAIR in $LIBS; do
        WF=${PAIR%%:*}
        SYSPath=${PAIR#*:}
        BN=${WF#lib}; BN=${BN%%.*}                              # 64 / 32
        # 模块内路径必须镜像原始路径: /system/X/Y -> 模块/system/X/Y
        case "$SYSPath" in
            /system/*) SUB=${SYSPath#/system/} ;;
            *) case "$BN" in
                   64) SUB="lib64/$(basename "$SYSPath")" ;;
                   *)  SUB="lib/$(basename "$SYSPath")" ;;
               esac ;;
        esac
        DST="$MOD_DIR/system/$SUB"
        mkdir -p "$(dirname "$DST")" || die "mkdir 失败: $(dirname "$DST")"
        cp -f "$WORK/$WF" "$DST" || die "拷贝失败: $DST"
        chmod 644 "$DST"
        LBL=$(get_label "$SYSPath")
        printf "chcon \"%s\" \"%s\" 2>/dev/null\n" "$LBL" "$DST" >> "$PFD"
        ok "已写入 $DST (标签 $LBL)"
    done

    printf "id=%s\nname=Sensor Vendor Sanitize\nversion=1.1\nversionCode=2\nauthor=HoshinoOVO\ndescription=SensorService virtual sensor vendor AOSP->%s (Misc Check5 mitigation)\n" "$MOD_ID" "$TARGET" > "$MOD_DIR/module.prop"

    chown -R 0:0 "$MOD_DIR" 2>/dev/null
    chmod -R 755 "$MOD_DIR" 2>/dev/null
    chmod 644 "$MOD_DIR/module.prop" "$MOD_DIR"/system/lib*/*.so 2>/dev/null
    chmod 755 "$PFD" 2>/dev/null

    echo ""
    ok "模块已生成: $MOD_DIR"
    ok "重启后生效。验证命令:"
    echo '    su -c "dumpsys sensorservice | grep -ci aosp"     # 期望 0'
    echo "    su -c \"dumpsys sensorservice | grep -i '$TARGET'\"  # 8 个传感器应显示 $TARGET"
    echo ""
    warn "OTA/大版本升级后请先 restore -> 重启 -> 重新 apply"
}

do_restore() {
    if [ -d "$MOD_DIR" ]; then
        rm -rf "$MOD_DIR" || die "删除失败"
        ok "模块已移除: $MOD_DIR"
        ok "重启后恢复原状"
    else
        ok "模块不存在,无需还原"
    fi
    rm -rf "$WORK"
}

do_status() {
    echo "=== 模块 ==="
    if [ -d "$MOD_DIR" ]; then
        echo "存在: $MOD_DIR"
        ls -l "$MOD_DIR/system/lib64/" 2>/dev/null
        [ -f "$MOD_DIR/post-fs-data.sh" ] && echo "post-fs-data.sh: ✓"
    else
        echo "未安装"
    fi
    echo "=== 运行中传感器 vendor ==="
    if ! command -v dumpsys >/dev/null 2>&1; then
        echo "dumpsys 不可用(此项仅设备上有意义)"
    else
        A=$(dumpsys sensorservice 2>/dev/null | grep -ci aosp)
        T=$(dumpsys sensorservice 2>/dev/null | grep -ci "$TARGET")
        echo "含 AOSP 计数: $A | 含 $TARGET 计数: $T"
        case "$A" in
            0) [ "$T" -ge 1 ] && echo "结论: ✓ 已改写" || echo "结论: ✓ 无 AOSP 特征" ;;
            *) [ "$A" -gt 3 ] && echo "结论: ✗ 超阈值(>3),检测(5)会命中" || echo "结论: 未超阈值(≤3)" ;;
        esac
    fi
}

# ---------- 入口 ----------
case "$CMD" in
    apply)   do_apply ;;
    restore) do_restore ;;
    status)  do_status ;;
    *) die "用法: sh $0 [apply|restore|status]" ;;
esac
