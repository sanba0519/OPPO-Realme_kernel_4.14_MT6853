// SPDX-License-Identifier: GPL-2.0
/*
 * SELinux services exported to the rest of the kernel.
 */
#include <linux/export.h>
#include <linux/types.h>
#include <linux/selinux.h>

#include "security.h"

bool selinux_is_enabled(void)
{
	return selinux_initialized(&selinux_state);
}
EXPORT_SYMBOL_GPL(selinux_is_enabled);
