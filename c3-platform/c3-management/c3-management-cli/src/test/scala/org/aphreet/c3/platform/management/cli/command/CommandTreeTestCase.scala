/*
 * Copyright (c) 2011, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.management.cli.command

import junit.framework.TestCase

import junit.framework.Assert._
import org.aphreet.c3.platform.management.cli.command.CommandTreeNode

class CommandTreeTestCase extends TestCase {

  def testCompleteNames() {

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