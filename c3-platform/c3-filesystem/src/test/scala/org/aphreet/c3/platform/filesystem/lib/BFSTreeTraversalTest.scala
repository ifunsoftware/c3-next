package org.aphreet.c3.platform.filesystem.lib

import junit.framework.{Assert, TestCase}


/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
class BFSTreeTraversalTest extends TestCase{

  case class Vertex(index: Int, children: Set[Vertex])
  val vertexBFSTraversal = new BFSTreeTraversal[Vertex]

  def testSimpleImplementation() {

    val vertices: Vertex = Vertex(index = 1, children = Set(
      Vertex(index = 2, children = Set(
        Vertex(index = 4, children = Set()),
        Vertex(index = 5, children = Set(
          Vertex(index = 11, children = Set())
        )),
        Vertex(index = 6, children = Set())
      )),
      Vertex(index = 3, children = Set(
        Vertex(index = 7, children = Set()),
        Vertex(index = 8, children = Set(
          Vertex(index = 12, children = Set())
        )),
        Vertex(index = 9, children = Set()),
        Vertex(index = 10, children = Set(
          Vertex(index = 13, children = Set())
        ))
      ))
    ))

    val result: Seq[Int] = vertexBFSTraversal.traverseFrom(_.children, vertices).map(_.index)

    Assert.assertEquals( (1 to 13).toList.reverse, result )
  }
}