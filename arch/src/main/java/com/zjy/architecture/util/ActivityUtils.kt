package com.zjy.architecture.util

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Process
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * This provides methods to help Activities load their UI.
 */
object ActivityUtils {

    private val activityList: MutableList<Activity> = ArrayList()
    private val activityCount = AtomicInteger(0)

    fun registerActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                registerActivity(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                activityStart()
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activityStop()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                unRegisterActivity(activity)
            }
        })
    }

    @Synchronized
    private fun registerActivity(activity: Activity) {
        activityList.add(activity)
    }

    @Synchronized
    private fun unRegisterActivity(activity: Activity) {
        activityList.remove(activity)
    }

    private fun activityStart() {
        val oldBg = isBackground
        activityCount.incrementAndGet()
        if (oldBg) {
            for (l in listeners) {
                l.onResumeApp()
            }
        }
    }

    private fun activityStop() {
        activityCount.decrementAndGet()
        if (isBackground) {
            for (l in listeners) {
                l.onLeaveApp()
            }
        }
    }

    fun getTopActivity(): Activity? {
        return activityList.lastOrNull()
    }

    fun isActivityTop(context: Context, clazz: Class<*>): Boolean {
        return isActivityTop(context, clazz.name)
    }

    /**
     *
     * ?????????activity??????????????????
     * @return  true????????? false????????????
     */
    fun isActivityTop(context: Context, clazzName: String): Boolean {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val name = manager.getRunningTasks(1)[0].topActivity!!.className
            name == clazzName
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ???????????????activity
     */
    @Synchronized
    fun popUpTo(destination: String, inclusive: Boolean = false) {
        val list = ArrayList(activityList)
        while (list.isNotEmpty()) {
            val activity = list.last()
            if (activity.javaClass.name == destination) {
                if (inclusive) {
                    activity.finish()
                }
                return
            }
            activity.finish()
            list.remove(activity)
        }
    }

    /**
     * ????????????
     */
    @Synchronized
    fun exitApp(kill: Boolean = false) {
        // ??????activity??????????????????activity
        for (activity in activityList) {
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
        if (kill) {
            Process.killProcess(Process.myPid())
        }
    }

    val isBackground: Boolean
        get() = activityCount.get() == 0

    fun getActivityCount(): Int {
        return activityList.size
    }

    fun isActivityExist(activityName: String): Boolean {
        for (activity in activityList) {
            if (activity.javaClass.name == activityName) {
                return true
            }
        }
        return false
    }

    @Synchronized
    fun finish(activityName: String) {
        for (activity in activityList) {
            if (activity.javaClass.name == activityName) {
                activity.finish() // ?????????destroy???????????????????????????removeActivity(activity)?????????
            }
        }
    }

    private val listeners = Collections.synchronizedList(ArrayList<OnAppStateChangedListener>())

    fun addOnAppStateChangedListener(listener: OnAppStateChangedListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeOnAppStateChangedListener(listener: OnAppStateChangedListener?) {
        listeners.remove(listener)
    }

    interface OnAppStateChangedListener {
        /**
         * app????????????
         */
        fun onResumeApp()

        /**
         * app????????????
         */
        fun onLeaveApp()
    }
}