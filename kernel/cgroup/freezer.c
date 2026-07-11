// SPDX-License-Identifier: GPL-2.0
#include <linux/cgroup.h>
#include <linux/sched.h>
#include <linux/sched/task.h>
#include <linux/sched/signal.h>

#include "cgroup-internal.h"

/* css_set_lock protects both task and descendant completion counters. */
static bool cgroup_freezer_complete(const struct cgroup *cgrp)
{
	lockdep_assert_held(&css_set_lock);

	return test_bit(CGRP_FREEZE, &cgrp->flags) &&
	       cgrp->freezer.nr_frozen_tasks == __cgroup_task_count(cgrp) &&
	       cgrp->freezer.nr_frozen_descendants == cgrp->nr_descendants;
}

/* Propagate a cgroup's frozen state towards the root. */
static void cgroup_propagate_frozen(struct cgroup *cgrp, bool frozen)
{
	int descendants = 1;

	/*
	 * A newly frozen cgroup can complete one or more freezing ancestors.
	 * A thawed cgroup makes every frozen ancestor incomplete immediately.
	 */
	while ((cgrp = cgroup_parent(cgrp))) {
		if (frozen) {
			cgrp->freezer.nr_frozen_descendants += descendants;
			if (!test_bit(CGRP_FROZEN, &cgrp->flags) &&
			    cgroup_freezer_complete(cgrp)) {
				set_bit(CGRP_FROZEN, &cgrp->flags);
				cgroup_file_notify(&cgrp->events_file);
				descendants++;
			}
		} else {
			cgrp->freezer.nr_frozen_descendants -= descendants;
			WARN_ON_ONCE(cgrp->freezer.nr_frozen_descendants < 0);
			if (test_bit(CGRP_FROZEN, &cgrp->flags)) {
				clear_bit(CGRP_FROZEN, &cgrp->flags);
				cgroup_file_notify(&cgrp->events_file);
				descendants++;
			}
		}
	}
}

/* Re-evaluate a cgroup after its task or descendant counts change. */
void cgroup_update_frozen(struct cgroup *cgrp)
{
	bool frozen;

	lockdep_assert_held(&css_set_lock);

	frozen = cgroup_freezer_complete(cgrp);

	if (frozen) {
		if (test_bit(CGRP_FROZEN, &cgrp->flags))
			return;
		set_bit(CGRP_FROZEN, &cgrp->flags);
	} else {
		if (!test_bit(CGRP_FROZEN, &cgrp->flags))
			return;
		clear_bit(CGRP_FROZEN, &cgrp->flags);
	}

	cgroup_file_notify(&cgrp->events_file);
	cgroup_propagate_frozen(cgrp, frozen);
}

static void cgroup_inc_frozen_cnt(struct cgroup *cgrp)
{
	cgrp->freezer.nr_frozen_tasks++;
}

static void cgroup_dec_frozen_cnt(struct cgroup *cgrp)
{
	cgrp->freezer.nr_frozen_tasks--;
	WARN_ON_ONCE(cgrp->freezer.nr_frozen_tasks < 0);
}

/* Mark current frozen/stopped and account it in its default cgroup. */
void cgroup_enter_frozen(void)
{
	struct cgroup *cgrp;

	if (current->frozen)
		return;

	spin_lock_irq(&css_set_lock);
	current->frozen = true;
	cgrp = task_dfl_cgroup(current);
	cgroup_inc_frozen_cnt(cgrp);
	cgroup_update_frozen(cgrp);
	spin_unlock_irq(&css_set_lock);
}

/*
 * Leave the accounted state unless a concurrent cgroup freeze still needs
 * the task stopped.  In that race, force another pass through get_signal().
 */
void cgroup_leave_frozen(bool always_leave)
{
	struct cgroup *cgrp;

	spin_lock_irq(&css_set_lock);
	cgrp = task_dfl_cgroup(current);
	if (always_leave || !test_bit(CGRP_FREEZE, &cgrp->flags)) {
		cgroup_dec_frozen_cnt(cgrp);
		cgroup_update_frozen(cgrp);
		WARN_ON_ONCE(!current->frozen);
		current->frozen = false;
	} else {
		spin_lock(&current->sighand->siglock);
		if (!(current->jobctl & JOBCTL_TRAP_FREEZE)) {
			/* Keep a racing freeze from returning to userspace. */
			current->jobctl |= JOBCTL_TRAP_FREEZE;
			set_tsk_thread_flag(current, TIF_SIGPENDING);
		}
		spin_unlock(&current->sighand->siglock);
	}
	spin_unlock_irq(&css_set_lock);
}

/* Request that a target task enter or leave the freezer trap. */
static void cgroup_freeze_task(struct task_struct *task, bool freeze)
{
	unsigned long flags;

	/*
	 * css_set_lock may be held by migration.  Signal-side freezer paths
	 * release sighand->siglock before acquiring css_set_lock.
	 */
	if (!lock_task_sighand(task, &flags))
		return;

	if (freeze) {
		task->jobctl |= JOBCTL_TRAP_FREEZE;
		signal_wake_up(task, false);
	} else {
		task->jobctl &= ~JOBCTL_TRAP_FREEZE;
		wake_up_process(task);
	}

	unlock_task_sighand(task, &flags);
}

static void cgroup_do_freeze(struct cgroup *cgrp, bool freeze)
{
	struct css_task_iter it;
	struct task_struct *task;

	lockdep_assert_held(&cgroup_mutex);

	spin_lock_irq(&css_set_lock);
	if (freeze)
		set_bit(CGRP_FREEZE, &cgrp->flags);
	else
		clear_bit(CGRP_FREEZE, &cgrp->flags);
	spin_unlock_irq(&css_set_lock);

	css_task_iter_start(&cgrp->self, 0, &it);
	while ((task = css_task_iter_next(&it))) {
		if (task->flags & PF_KTHREAD)
			continue;
		cgroup_freeze_task(task, freeze);
	}
	css_task_iter_end(&it);

	/* Cover empty leaves and descendants already in the desired state. */
	spin_lock_irq(&css_set_lock);
	if (cgrp->nr_descendants == cgrp->freezer.nr_frozen_descendants)
		cgroup_update_frozen(cgrp);
	spin_unlock_irq(&css_set_lock);
}

void cgroup_freezer_migrate_task(struct task_struct *task,
				 struct cgroup *src, struct cgroup *dst)
{
	lockdep_assert_held(&css_set_lock);

	if (task->flags & PF_KTHREAD)
		return;

	if (task->frozen) {
		cgroup_inc_frozen_cnt(dst);
		cgroup_dec_frozen_cnt(src);
	}

	cgroup_update_frozen(dst);
	cgroup_update_frozen(src);
	cgroup_freeze_task(task, test_bit(CGRP_FREEZE, &dst->flags));
}

void cgroup_freezer_frozen_exit(struct task_struct *task)
{
	struct cgroup *cgrp = task_dfl_cgroup(task);

	lockdep_assert_held(&css_set_lock);
	cgroup_dec_frozen_cnt(cgrp);
	cgroup_update_frozen(cgrp);
}

void cgroup_freeze(struct cgroup *cgrp, bool freeze)
{
	struct cgroup_subsys_state *css;
	bool applied = false;

	lockdep_assert_held(&cgroup_mutex);

	if (cgrp->freezer.freeze == freeze)
		return;

	cgrp->freezer.freeze = freeze;

	css_for_each_descendant_pre(css, &cgrp->self) {
		struct cgroup *descendant = css->cgroup;

		if (cgroup_is_dead(descendant))
			continue;

		if (freeze) {
			descendant->freezer.e_freeze++;
			if (descendant->freezer.e_freeze > 1)
				continue;
		} else {
			descendant->freezer.e_freeze--;
			if (descendant->freezer.e_freeze > 0)
				continue;
			WARN_ON_ONCE(descendant->freezer.e_freeze < 0);
		}

		cgroup_do_freeze(descendant, freeze);
		applied = true;
	}

	/*
	 * An ancestor may already enforce the requested effective state.  Wake
	 * pollers even when this write did not need to change any task state.
	 */
	if (!applied)
		cgroup_file_notify(&cgrp->events_file);
}
