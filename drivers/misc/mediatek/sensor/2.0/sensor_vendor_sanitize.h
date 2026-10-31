/* SPDX-License-Identifier: GPL-2.0 */
/*
 * Copyright (C) 2026 - sensor vendor sanitizer
 *
 * Keep AOSP placeholder sensor vendor strings from reaching the sensors
 * HAL / framework, no matter which sensorhub backend is compiled in
 * (CONFIG_NANOHUB or CONFIG_MTK_SENSORHUB).
 *
 * Emulator-detection heuristics (e.g. risk-control SDKs) count sensors
 * whose vendor string contains "AOSP" (case-insensitive) and flag the
 * device when the count is larger than a few. A real device must never
 * report them: sanitize every vendor string that crosses the kernel
 * boundary here.
 */
#ifndef _SENSOR_VENDOR_SANITIZE_H_
#define _SENSOR_VENDOR_SANITIZE_H_

#include <linux/ctype.h>
#include <linux/string.h>
#include <linux/types.h>

/*
 * Return true if the NUL-terminated vendor string contains "aosp"
 * (case-insensitive).
 *
 * C89/gnu89 friendly: all declarations at block start, no gcc/clang
 * extension, no -Wdeclaration-after-statement noise, and "static inline"
 * stays silent under -Wunused-function even in translation units that
 * never call it.
 */
static inline bool vendor_contains_aosp(const char *vendor, size_t size)
{
	static const char pat[] = "aosp";
	const size_t plen = sizeof(pat) - 1;
	size_t i, j;

	if (!vendor || size < plen)
		return false;

	for (i = 0; vendor[i] != '\0' && i + plen <= size - 1; i++) {
		for (j = 0; j < plen; j++) {
			if (tolower((unsigned char)vendor[i + j]) != pat[j])
				break;
		}
		if (j == plen)
			return true;
	}
	return false;
}

/*
 * Rewrite "vendor" (buffer of "size" bytes) to "mtk" if it contains
 * "aosp" (case-insensitive); leave it untouched otherwise.
 */
static inline void sensor_vendor_sanitize(char *vendor, size_t size)
{
	if (vendor_contains_aosp(vendor, size))
		strlcpy(vendor, "mtk", size);
}

#endif /* _SENSOR_VENDOR_SANITIZE_H_ */
