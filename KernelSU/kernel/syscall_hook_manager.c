#include <linux/compiler.h>
#include <linux/cred.h>
#include <linux/printk.h>
#include <linux/spinlock.h>
#include <linux/tracepoint.h>
#include <asm/syscall.h>
#include <linux/ptrace.h>
#include <trace/events/syscalls.h>
#include <linux/version.h>
#include <asm/unistd.h>

#include "allowlist.h"
#include "arch.h"
#include "syscall_hook_manager.h"
#include "sucompat.h"
#include "setuid_hook.h"
#include "util.h"
#include "ksud.h"

// 基础标记逻辑
static void handle_process_mark(bool mark) {
    struct task_struct *p, *t;
    read_lock(&tasklist_lock);
    for_each_process_thread(p, t) {
        if (mark) ksu_set_task_tracepoint_flag(t);
        else ksu_clear_task_tracepoint_flag(t);
    }
    read_unlock(&tasklist_lock);
}

// 4.14 核心拦截列表
static inline bool check_syscall_fastpath(int nr) {
    switch (nr) {
        case __NR_faccessat:
        case __NR_execve:
        case __NR_newfstatat:
        case __NR_setresuid:
            return true;
        default:
            return false;
    }
}

#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS

// 处理 init 进程执行 ksud 的特殊逻辑（原版精华）
static void handle_init_exec(const char __user *filename) {
    char path[64];
    if (strncpy_from_user(path, filename, sizeof(path)) > 0) {
        if (strcmp(path, KSUD_PATH) == 0) {
            pr_info("ksu: escape to root for init: %d\n", current->pid);
            escape_to_root_for_init();
        }
    }
}

static void ksu_sys_enter_handler(void *data, struct pt_regs *regs, long id) {
    if (!check_syscall_fastpath(id)) return;

    if (ksu_su_compat_enabled) {
        // execve 拦截
        if (id == __NR_execve) {
            const char __user **fname_user = (const char __user **)&regs->regs[0];
            // 如果是 init 进程在执行，检查是否是 ksud
            if (current->pid == 1 || is_init(get_current_cred())) {
                handle_init_exec(*fname_user);
            } else {
                ksu_handle_execve_sucompat(fname_user, NULL, NULL, NULL);
            }
            return;
        }
        // faccessat 拦截
        if (id == __NR_faccessat) {
            int dfd = (int)regs->regs[0];
            const char __user **fname_user = (const char __user **)&regs->regs[1];
            int mode = (int)regs->regs[2];
            ksu_handle_faccessat(&dfd, fname_user, &mode, NULL);
            return;
        }
        // newfstatat 拦截
        if (id == __NR_newfstatat) {
            int dfd = (int)regs->regs[0];
            const char __user **fname_user = (const char __user **)&regs->regs[1];
            int flags = (int)regs->regs[3];
            ksu_handle_stat(&dfd, fname_user, &flags);
            return;
        }
        // setresuid 拦截
        if (id == __NR_setresuid) {
            ksu_handle_setresuid((uid_t)regs->regs[0], (uid_t)regs->regs[1], (uid_t)regs->regs[2]);
            return;
        }
    }
}
#endif

void ksu_syscall_hook_manager_init(void) {
    pr_info("ksu: init hook manager (4.14 optimized)\n");
#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS
    if (!register_trace_sys_enter(ksu_sys_enter_handler, NULL)) {
        pr_info("ksu: sys_enter registered\n");
        handle_process_mark(true);
    }
#endif
    ksu_setuid_hook_init();
    ksu_sucompat_init();
}

void ksu_syscall_hook_manager_exit(void) {
#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS
    unregister_trace_sys_enter(ksu_sys_enter_handler, NULL);
#endif
    ksu_sucompat_exit();
    ksu_setuid_hook_exit();
}