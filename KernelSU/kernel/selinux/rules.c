#include <linux/uaccess.h>
#include <linux/types.h>
#include <linux/version.h>
#include <linux/printk.h>
#include "../klog.h" // IWYU pragma: keep
#include "selinux.h"
#include "sepolicy.h"
#include "ss/services.h"
#include "linux/lsm_audit.h" // IWYU pragma: keep
#include "xfrm.h"
#include "compat.h"

#define SELINUX_POLICY_INSTEAD_SELINUX_SS

#define ALL NULL
extern struct policydb policydb;

static struct policydb *get_policydb(void)
{
    struct policydb *db = NULL;  // 先初始化为 NULL

#if LINUX_VERSION_CODE >= KERNEL_VERSION(5,0,0)
    struct selinux_policy *policy = selinux_state.policy;
    if (!policy) {
        pr_err("SukiSU: selinux_state.policy is NULL\n");
        return NULL;  // 返回 NULL 表示失败
    }
    db = &policy->policydb;
#else
    // 老内核（4.14）：policydb 是全局变量（extern 声明）
    extern struct policydb policydb;
    db = &policydb;
    pr_info("SukiSU: Using global policydb for old kernel < 5.0\n");
#endif

    return db;
}

static DEFINE_MUTEX(ksu_rules);

void apply_kernelsu_rules(void)
{
#if LINUX_VERSION_CODE >= KERNEL_VERSION(5,0,0)
    struct policydb *db = NULL;

    mutex_lock(&ksu_rules);

    db = get_policydb();
    if (!db) {
        pr_warn("SukiSU: get_policydb failed, fallback to global policydb\n");
        extern struct policydb policydb;
        db = &policydb;
    }

    if (!db) {
        pr_err("SukiSU: no valid policydb, skip rules\n");
        mutex_unlock(&ksu_rules);
        return;
    }

    // --- 核心规则：让 KSU domain permissive + 全允许 ---
    ksu_permissive(db, KERNEL_SU_DOMAIN);
    ksu_allow(db, KERNEL_SU_DOMAIN, ALL, ALL, ALL);

    // init 交互
    ksu_allow(db, "init", KERNEL_SU_DOMAIN, ALL, ALL);

    // servicemanager 交互
    ksu_allow(db, "servicemanager", KERNEL_SU_DOMAIN, "dir", "search");
    ksu_allow(db, "servicemanager", KERNEL_SU_DOMAIN, "dir", "read");
    ksu_allow(db, "servicemanager", KERNEL_SU_DOMAIN, "file", "open");
    ksu_allow(db, "servicemanager", KERNEL_SU_DOMAIN, "file", "read");

    // logd 交互
    ksu_allow(db, "logd", KERNEL_SU_DOMAIN, "dir", "search");
    ksu_allow(db, "logd", KERNEL_SU_DOMAIN, "file", "read");
    ksu_allow(db, "logd", KERNEL_SU_DOMAIN, "file", "open");
    ksu_allow(db, "logd", KERNEL_SU_DOMAIN, "file", "getattr");

    // dumpsys / binder 等常见权限
    ksu_allow(db, ALL, KERNEL_SU_DOMAIN, "fd", "use");
    ksu_allow(db, ALL, KERNEL_SU_DOMAIN, "fifo_file", "write");
    ksu_allow(db, ALL, KERNEL_SU_DOMAIN, "fifo_file", "read");
    ksu_allow(db, ALL, KERNEL_SU_DOMAIN, "fifo_file", "open");
    ksu_allow(db, ALL, KERNEL_SU_DOMAIN, "fifo_file", "getattr");

    ksu_allow(db, "hwservicemanager", KERNEL_SU_DOMAIN, "dir", "search");
    ksu_allow(db, "hwservicemanager", KERNEL_SU_DOMAIN, "file", "read");
    ksu_allow(db, "hwservicemanager", KERNEL_SU_DOMAIN, "file", "open");

    ksu_allow(db, ALL, KERNEL_SU_DOMAIN, "binder", ALL);

    ksu_allow(db, "system_server", KERNEL_SU_DOMAIN, "process", "getpgid");
    ksu_allow(db, "system_server", KERNEL_SU_DOMAIN, "process", "sigkill");

    // dontaudit
    ksu_dontaudit(db, "untrusted_app", KERNEL_SU_DOMAIN, "dir", "getattr");

    // 复杂规则保留注释
    /*
    ksu_typeattribute(db, KERNEL_SU_DOMAIN, "mlstrustedsubject");
    ksu_typeattribute(db, KERNEL_SU_DOMAIN, "netdomain");
    ksu_typeattribute(db, KERNEL_SU_DOMAIN, "bluetoothdomain");

    ksu_type(db, KERNEL_SU_FILE, "file_type");
    ksu_typeattribute(db, KERNEL_SU_FILE, "mlstrustedobject");
    ksu_allow(db, ALL, KERNEL_SU_FILE, ALL, ALL);

    if (db->policyvers >= POLICYDB_VERSION_XPERMS_IOCTL) {
        ksu_allowxperm(db, KERNEL_SU_DOMAIN, ALL, "blk_file", ALL);
        ksu_allowxperm(db, KERNEL_SU_DOMAIN, ALL, "fifo_file", ALL);
        ksu_allowxperm(db, KERNEL_SU_DOMAIN, ALL, "chr_file", ALL);
        ksu_allowxperm(db, KERNEL_SU_DOMAIN, ALL, "file", ALL);
    }
    */

    mutex_unlock(&ksu_rules);
    pr_info("SukiSU: rules processing completed (policydb updated)\n");

#else
    // 4.14 内核直接跳过上述所有逻辑，彻底杜绝 undefined symbol: policydb
    pr_info("SukiSU: skipping SELinux rules injection on 4.14 (policydb not exported)\n");
#endif
}

#define MAX_SEPOL_LEN 128

#define CMD_NORMAL_PERM 1
#define CMD_XPERM 2
#define CMD_TYPE_STATE 3
#define CMD_TYPE 4
#define CMD_TYPE_ATTR 5
#define CMD_ATTR 6
#define CMD_TYPE_TRANSITION 7
#define CMD_TYPE_CHANGE 8
#define CMD_GENFSCON 9

struct sepol_data {
    u32 cmd;
    u32 subcmd;
    char __user *sepol1;
    char __user *sepol2;
    char __user *sepol3;
    char __user *sepol4;
    char __user *sepol5;
    char __user *sepol6;
    char __user *sepol7;
};

static int get_object(char *buf, char __user *user_object, size_t buf_sz,
                      char **object)
{
    if (!user_object) {
        *object = ALL;
        return 0;
    }

#if LINUX_VERSION_CODE < KERNEL_VERSION(5,8,0)
    // 4.14 用 strncpy_from_unsafe 兼容
    if (strncpy_from_unsafe(buf, user_object, buf_sz) < 0) {
        return -EINVAL;
    }
#else
    if (strncpy_from_user(buf, user_object, buf_sz) < 0) {
        return -EINVAL;
    }
#endif

    *object = buf;

    return 0;
}

#if (LINUX_VERSION_CODE >= KERNEL_VERSION(6, 4, 0))
extern int avc_ss_reset(u32 seqno);
#else
extern int avc_ss_reset(struct selinux_avc *avc, u32 seqno);
#endif
// reset avc cache table, otherwise the new rules will not take effect if already denied
static void reset_avc_cache()
{
#if (LINUX_VERSION_CODE >= KERNEL_VERSION(6, 4, 0))
    avc_ss_reset(0);
    selnl_notify_policyload(0);
    selinux_status_update_policyload(0);
#else
    struct selinux_avc *avc = selinux_state.avc;
    avc_ss_reset(avc, 0);
    selnl_notify_policyload(0);
    selinux_status_update_policyload(&selinux_state, 0);
#endif
    selinux_xfrm_notify_policyload();
}

int handle_sepolicy(unsigned long arg3, void __user *arg4)
{
    // 4.14 链接修正：只有在 5.0+ 才会真正定义和使用 db
#if LINUX_VERSION_CODE >= KERNEL_VERSION(5,0,0)
    struct policydb *db;
#endif

    if (!arg4) {
        return -EINVAL;
    }

    if (!getenforce()) {
        pr_info("SukiSU: SELinux permissive or disabled when handle policy!\n");
    }

    struct sepol_data data;
    if (copy_from_user(&data, arg4, sizeof(struct sepol_data))) {
        pr_err("sepol: copy sepol_data failed.\n");
        return -EINVAL;
    }

    u32 cmd = data.cmd;
    u32 subcmd = data.subcmd;
    int ret = -EINVAL;

    // 4.14 核心补丁：如果版本低于 5.0，直接跳过数据库操作逻辑
#if LINUX_VERSION_CODE < KERNEL_VERSION(5,0,0)
    pr_info("SukiSU: handle_sepolicy skipped on 4.14 to avoid link errors (cmd: %d)\n", cmd);
    ret = 0; // 假装执行成功，让用户态程序继续运行
    goto exit;
#else

    mutex_lock(&ksu_rules);

    db = get_policydb();
    if (!db) {
        // 尝试获取全局符号（仅限支持导出的内核）
        extern struct policydb policydb;
        db = &policydb;
    }

    if (!db) {
        pr_err("sepol: no valid policydb.\n");
        goto unlock_exit;
    }

    if (cmd == CMD_NORMAL_PERM) {
        char src_buf[MAX_SEPOL_LEN];
        char tgt_buf[MAX_SEPOL_LEN];
        char cls_buf[MAX_SEPOL_LEN];
        char perm_buf[MAX_SEPOL_LEN];

        char *s, *t, *c, *p;
        if (get_object(src_buf, data.sepol1, sizeof(src_buf), &s) < 0 ||
            get_object(tgt_buf, data.sepol2, sizeof(tgt_buf), &t) < 0 ||
            get_object(cls_buf, data.sepol3, sizeof(cls_buf), &c) < 0 ||
            get_object(perm_buf, data.sepol4, sizeof(perm_buf), &p) < 0) {
            goto unlock_exit;
        }

        bool success = false;
        if (subcmd == 1) success = ksu_allow(db, s, t, c, p);
        else if (subcmd == 2) success = ksu_deny(db, s, t, c, p);
        else if (subcmd == 3) success = ksu_auditallow(db, s, t, c, p);
        else if (subcmd == 4) success = ksu_dontaudit(db, s, t, c, p);
        ret = success ? 0 : -EINVAL;

    } else if (cmd == CMD_XPERM) {
        char src_buf[MAX_SEPOL_LEN], tgt_buf[MAX_SEPOL_LEN], cls_buf[MAX_SEPOL_LEN];
        char operation[MAX_SEPOL_LEN], perm_set[MAX_SEPOL_LEN];
        char *s, *t, *c;

        if (get_object(src_buf, data.sepol1, sizeof(src_buf), &s) < 0 ||
            get_object(tgt_buf, data.sepol2, sizeof(tgt_buf), &t) < 0 ||
            get_object(cls_buf, data.sepol3, sizeof(cls_buf), &c) < 0 ||
            strncpy_from_user(operation, data.sepol4, sizeof(operation)) < 0 ||
            strncpy_from_user(perm_set, data.sepol5, sizeof(perm_set)) < 0) {
            goto unlock_exit;
        }

        bool success = false;
        if (subcmd == 1) success = ksu_allowxperm(db, s, t, c, perm_set);
        else if (subcmd == 2) success = ksu_auditallowxperm(db, s, t, c, perm_set);
        else if (subcmd == 3) success = ksu_dontauditxperm(db, s, t, c, perm_set);
        ret = success ? 0 : -EINVAL;

    } else if (cmd == CMD_TYPE_STATE) {
        char src[MAX_SEPOL_LEN];
        if (strncpy_from_user(src, data.sepol1, sizeof(src)) < 0) goto unlock_exit;
        
        bool success = (subcmd == 1) ? ksu_permissive(db, src) : ksu_enforce(db, src);
        if (success) ret = 0;

    } else if (cmd == CMD_TYPE || cmd == CMD_TYPE_ATTR) {
        char type[MAX_SEPOL_LEN], attr[MAX_SEPOL_LEN];
        if (strncpy_from_user(type, data.sepol1, sizeof(type)) < 0 ||
            strncpy_from_user(attr, data.sepol2, sizeof(attr)) < 0) goto unlock_exit;

        if (cmd == CMD_TYPE) ret = ksu_type(db, type, attr) ? 0 : -EINVAL;
        else ret = ksu_typeattribute(db, type, attr) ? 0 : -EINVAL;

    } else if (cmd == CMD_ATTR) {
        char attr[MAX_SEPOL_LEN];
        if (strncpy_from_user(attr, data.sepol1, sizeof(attr)) < 0) goto unlock_exit;
        if (ksu_attribute(db, attr)) ret = 0;

    } else if (cmd == CMD_TYPE_TRANSITION) {
        char src[MAX_SEPOL_LEN], tgt[MAX_SEPOL_LEN], cls[MAX_SEPOL_LEN], def[MAX_SEPOL_LEN], obj[MAX_SEPOL_LEN];
        if (strncpy_from_user(src, data.sepol1, sizeof(src)) < 0 ||
            strncpy_from_user(tgt, data.sepol2, sizeof(tgt)) < 0 ||
            strncpy_from_user(cls, data.sepol3, sizeof(cls)) < 0 ||
            strncpy_from_user(def, data.sepol4, sizeof(def)) < 0) goto unlock_exit;
        
        char *real_obj = (data.sepol5 && strncpy_from_user(obj, data.sepol5, sizeof(obj)) >= 0) ? obj : NULL;
        if (ksu_type_transition(db, src, tgt, cls, def, real_obj)) ret = 0;

    } else if (cmd == CMD_GENFSCON) {
        char n[MAX_SEPOL_LEN], p[MAX_SEPOL_LEN], c[MAX_SEPOL_LEN];
        if (strncpy_from_user(n, data.sepol1, sizeof(n)) < 0 ||
            strncpy_from_user(p, data.sepol2, sizeof(p)) < 0 ||
            strncpy_from_user(c, data.sepol3, sizeof(c)) < 0) goto unlock_exit;
        if (ksu_genfscon(db, n, p, c)) ret = 0;
    }

unlock_exit:
    mutex_unlock(&ksu_rules);
#endif // 结束 5.0+ 逻辑分支

exit:
#if LINUX_VERSION_CODE >= KERNEL_VERSION(5,0,0)
    reset_avc_cache();
#endif
    return ret;
}