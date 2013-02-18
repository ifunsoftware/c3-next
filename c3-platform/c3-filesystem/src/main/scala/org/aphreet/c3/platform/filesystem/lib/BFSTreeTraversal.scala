package org.aphreet.c3.platform.filesystem.lib

import annotation.tailrec
import org.aphreet.c3.platform.filesystem.{NodeRef, Directory, File, Node}

/**
 * @author Dmitry Ivanov (id.ajantis@gmail.com)
 *         iFunSoftware
 */
class BFSTreeTraversal[A]{

  /*
   * Returns a Sequence of A in reverse order (from tree bottom to upper root node)
   */
  def traverseFrom(unfoldFunc: A => Set[A], initial: A): Seq[A] =
    traverseTR(unfoldFunc, Seq(initial), Seq.empty)

  @tailrec
  private def traverseTR(unfoldFunc: A => Set[A], toVisit: Seq[A], acc: Seq[A]): Seq[A] = {
    toVisit.toList match {
      case Nil => acc
      case head :: tail => {
        val toVisitNew = tail ++ unfoldFunc(head) // BFS traversal
          // Depth-first traversal
          // unfoldFunc(head).toSeq ++ tail
        traverseTR(unfoldFunc, toVisitNew, head +: acc)
      }
    }
  }
}

trait FSNodeBFSTreeTraversal extends BFSTreeTraversal[Node]{

  protected def resolveNodeByAddress(ra: String): Node

  def traverseFS(root: Node): Seq[Node] = traverseFrom(unfoldNode, root)

  private def unfoldNode(root: Node): Set[Node] = {
    root match {
      case f: File => Set()
      case d: Directory => d.children.map((ref: NodeRef) =>
        resolveNodeByAddress(ref.address)).toSet
    }
  }
}