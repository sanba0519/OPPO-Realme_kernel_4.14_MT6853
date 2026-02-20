#ifndef _KSU_COMPAT_H
#define _KSU_COMPAT_H

#include <linux/version.h>
#include <linux/uaccess.h>
#include <linux/mm.h>

#if LINUX_VERSION_CODE < KERNEL_VERSION(5, 8, 0)

#define strncpy_from_user_nofault strncpy_from_unsafe
#define copy_from_user_nofault(to, from, n) probe_kernel_read(to, from, n)
#define copy_to_user_nofault(to, from, n) probe_kernel_write(to, from, n)

#define mmap_read_trylock(mm) down_read_trylock(&(mm)->mmap_sem)
#define mmap_read_unlock(mm) up_read(&(mm)->mmap_sem)

#endif

#endif /* _KSU_COMPAT_H */