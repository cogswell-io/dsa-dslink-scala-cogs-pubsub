package io.cogswell.dslink.pubsub.util

case class Trim(text: String) {
  private def firstOffset(s: String): Option[Int] = Option(text indexOf s).filter(_ >= 0)
  private def firstOffset(c: Int): Option[Int] = Option(text indexOf c).filter(_ >= 0)
  private def lastOffset(s: String): Option[Int] = Option(text lastIndexOf s).filter(_ >= 0)
  private def lastOffset(c: Int): Option[Int] = Option(text lastIndexOf c).filter(_ >= 0)
  
  def until(c: Int): String = firstOffset(c).map(text.substring(_)).getOrElse(text)
  def until(s: String): String = firstOffset(s).map(text.substring(_)).getOrElse(text)
    
  def through(c: Int): String = firstOffset(c).map(_+1).map(text.substring(_)).getOrElse(text)
  def through(s: String): String = {
    firstOffset(s).map(_+s.length).map(text.substring(_)).getOrElse(text)
  }
  
  def from(c: Int): String = lastOffset(c).map(text.substring(0, _)).getOrElse(text)
  def from(s: String): String = lastOffset(s).map(text.substring(0, _)).getOrElse(text)
    
  def after(c: Int): String = lastOffset(c).map(_+1).map(text.substring(0, _)).getOrElse(text)
  def after(s: String): String = {
    lastOffset(s).map(_+s.length).map(text.substring(0, _)).getOrElse(text)
  }
  
}

object StringUtils {
  def trim(text: String): Trim = Trim(text)
}