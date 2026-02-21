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
#include <linux/sched.h>

#include "allowlist.h"
#include "arch.h"
#include "syscall_hook_manager.h"
#include "sucompat.h"
#include "setuid_hook.h"
#include "util.h"
#include "ksud.h"

// --- 基础进程标记逻辑 ---
static void handle_process_mark(bool mark) {
    struct task_struct *p, *t;
    read_lock(&tasklist_lock);
    for_each_process_thread(p, t) {
        if (mark) ksu_set_task_tracepoint_flag(t);
        else ksu_clear_task_tracepoint_flag(t);
    }
    read_unlock(&tasklist_lock);
}

// 补全链接器需要的符号 (Undefined Symbols 修复)
void ksu_mark_all_process(void) { handle_process_mark(true); }
void ksu_unmark_all_process(void) { handle_process_mark(false); }
void ksu_mark_running_process(void) { handle_process_mark(true); }

void ksu_clear_task_tracepoint_flag_if_needed(struct task_struct *t) {
    ksu_clear_task_tracepoint_flag(t);
}

int ksu_get_task_mark(pid_t pid) {
    struct task_struct *task;
    int marked = -ESRCH;
    rcu_read_lock();
    task = find_task_by_vpid(pid);
    if (task) {
        marked = test_tsk_thread_flag(task, TIF_SYSCALL_TRACEPOINT) ? 1 : 0;
    }
    rcu_read_unlock();
    return marked;
}

int ksu_set_task_mark(pid_t pid, bool mark) {
    struct task_struct *task;
    int ret = -ESRCH;
    rcu_read_lock();
    task = find_task_by_vpid(pid);
    if (task) {
        if (mark) ksu_set_task_tracepoint_flag(task);
        else ksu_clear_task_tracepoint_flag(task);
        ret = 0;
    }
    rcu_read_unlock();
    return ret;
}

// --- Hook 核心逻辑 ---
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
static void handle_init_exec(const char __user *filename) {
    char path[64];
    if (strncpy_from_user(path, filename, sizeof(path)) > 0) {
        if (strcmp(path, KSUD_PATH) == 0) {
            escape_to_root_for_init();
        }
    }
}

static void ksu_sys_enter_handler(void *data, struct pt_regs *regs, long id) {
    if (!check_syscall_fastpath(id)) return;
    if (ksu_su_compat_enabled) {
        if (id == __NR_execve) {
            const char __user **fname_user = (const char __user **)&regs->regs[0];
            if (current->pid == 1 || is_init(get_current_cred())) {
                handle_init_exec(*fname_user);
            } else {
                ksu_handle_execve_sucompat(fname_user, NULL, NULL, NULL);
            }
            return;
        }
        if (id == __NR_faccessat) {
            int dfd = (int)regs->regs[0];
            const char __user **fname_user = (const char __user **)&regs->regs[1];
            int mode = (int)regs->regs[2];
            ksu_handle_faccessat(&dfd, filename_user, &mode, NULL);
            return;
        }
        if (id == __NR_newfstatat) {
            int dfd = (int)regs->regs[0];
            const char __user **fname_user = (const char __user **)&regs->regs[1];
            int flags = (int)regs->regs[3];
            ksu_handle_stat(&dfd, filename_user, &flags);
            return;
        }
        if (id == __NR_setresuid) {
            ksu_handle_setresuid((uid_t)regs->regs[0], (uid_t)regs->regs[1], (uid_t)regs->regs[2]);
            return;
        }
    }
}
#endif

void ksu_syscall_hook_manager_init(void) {
#ifdef CONFIG_HAVE_SYSCALL_TRACEPOINTS
    if (!register_trace_sys_enter(ksu_sys_enter_handler, NULL)) {
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