package org.aphreet.c3.platform.task

/**
 * 
 * User: antey
 * Date: 23.03.13
 * Time: 15:46
 */
trait TaskScheduleObserver {
  def updateSchedule(task: Task, newSchedule: String)
}
