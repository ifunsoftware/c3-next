package org.aphreet.c3.platform.client.test

import junit.framework.TestCase
import org.aphreet.c3.platform.client.management.command.CommandTreeNode

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 13, 2010
 * Time: 12:13:55 AM
 * To change this template use File | Settings | File Templates.
 */

import junit.framework.Assert._

class CommandTreeTestCase extends TestCase{

  def testCompleteNames{

    val root = new CommandTreeNode(null)

    root.addCommand(List("command0"), classOf[Command0])
    root.addCommand(List("command1", "exec"), classOf[Command1])
    root.addCommand(List("command1", "command2", "exec"), classOf[Command12])
    root.addCommand(List("command1", "command2", "command3", "command4"), classOf[Command1234])
    root.addCommand(List("command2"), classOf[Command2])
    root.addCommand(List("some"), classOf[SomeCommand])

    assertEquals(classOf[Command0], root.classForInput(List("command0"))._1)
    assertEquals(classOf[Command1], root.classForInput(List("command1", "exec"))._1)
    assertNull(root.classForInput(List("command1"))._1)
    assertEquals(classOf[Command12], root.classForInput(List("command1", "command2", "exec"))._1)
    assertEquals(classOf[Command1234], root.classForInput(List("command1", "command2", "command3", "command4"))._1)
    assertEquals(classOf[Command2], root.classForInput(List("command2"))._1)
    assertEquals(classOf[SomeCommand], root.classForInput(List("some"))._1)

    assertEquals(classOf[SomeCommand], root.classForInput(List("som"))._1)
    assertNull(root.classForInput(List("command"))._1)

    assertEquals(classOf[Command1234], root.classForInput(List("command1", "command2", "c", "c"))._1)
    

  
  }



}

class Command0
class Command1
class Command12
class Command1234
class Command2
class SomeCommand