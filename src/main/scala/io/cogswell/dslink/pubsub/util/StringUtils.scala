package io.cogswell.dslink.pubsub.util

case class Trim(text: String) {
  def until(c: Int): String = StringUtils.firstOffset(text, c).map(text.substring(_)).getOrElse(text)
  def until(s: String): String = StringUtils.firstOffset(text, s).map(text.substring(_)).getOrElse(text)
    
  def through(c: Int): String = StringUtils.firstOffset(text, c).map(_+1).map(text.substring(_)).getOrElse(text)
  def through(s: String): String = {
    StringUtils.firstOffset(text, s).map(_+s.length).map(text.substring(_)).getOrElse(text)
  }
  
  def from(c: Int): String = StringUtils.lastOffset(text, c).map(text.substring(0, _)).getOrElse(text)
  def from(s: String): String = StringUtils.lastOffset(text, s).map(text.substring(0, _)).getOrElse(text)
    
  def after(c: Int): String = StringUtils.lastOffset(text, c).map(_+1).map(text.substring(0, _)).getOrElse(text)
  def after(s: String): String = {
    StringUtils.lastOffset(text, s).map(_+s.length).map(text.substring(0, _)).getOrElse(text)
  }
}

case class Split(text: String) {
  def onFirst(c: Int): (String, String) = {
    StringUtils.firstOffset(text, c)
    .map(i => (text.substring(0, i), text.substring(i+1))).getOrElse((text, ""))
  }
  def onFirst(s: String): (String, String) = {
    StringUtils.firstOffset(text, s)
    .map(i => (text.substring(0, i), text.substring(i+s.length))).getOrElse((text, ""))
  }
  
  def onLast(c: Int): (String, String) = {
    StringUtils.lastOffset(text, c)
    .map(i => (text.substring(0, i), text.substring(i+1))).getOrElse((text, ""))
  }
  def onLast(s: String): (String, String) = {
    StringUtils.lastOffset(text, s)
    .map(i => (text.substring(0, i), text.substring(i+s.length))).getOrElse((text, ""))
  }
}

object StringUtils {
  def firstOffset(text: String, s: String): Option[Int] = Option(text indexOf s).filter(_ >= 0)
  def firstOffset(text: String, c: Int): Option[Int] = Option(text indexOf c).filter(_ >= 0)
  def lastOffset(text: String, s: String): Option[Int] = Option(text lastIndexOf s).filter(_ >= 0)
  def lastOffset(text: String, c: Int): Option[Int] = Option(text lastIndexOf c).filter(_ >= 0)
  
  def trim(text: String): Trim = Trim(text)
  def split(text: String): Split = Split(text)
}