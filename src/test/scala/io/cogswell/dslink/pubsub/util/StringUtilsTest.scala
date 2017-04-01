package io.cogswell.dslink.pubsub.util

import io.cogswell.dslink.pubsub.DslinkTest
import scaldi.Injectable
import java.time.Instant
import org.joda.time.DateTime

class StringUtilsTest extends DslinkTest() {
  val text = "12345678"
  
  "StringUtils.Trim.until" should "trim until a target string" in {
    StringUtils trim text until "1" should be (text)
    StringUtils trim text until "12" should be (text)
    StringUtils trim text until "56" should be ("5678")
    StringUtils trim text until "78" should be ("78")
    StringUtils trim text until "8" should be ("8")
    StringUtils trim text until "9" should be (text)
  }
  
  it should "trim until a target code point" in {
    StringUtils trim text until '1' should be (text)
    StringUtils trim text until '5' should be ("5678")
    StringUtils trim text until '8' should be ("8")
    StringUtils trim text until '9' should be (text)
  }

  "StringUtils.Trim.through" should "trim through a target string" in {
    StringUtils trim text through "1" should be ("2345678")
    StringUtils trim text through "12" should be ("345678")
    StringUtils trim text through "56" should be ("78")
    StringUtils trim text through "78" should be ("")
    StringUtils trim text through "8" should be ("")
    StringUtils trim text through "9" should be (text)
    StringUtils trim text through text should be ("")
  }
  
  it should "trim through a target code point" in {
    StringUtils trim text through '1' should be ("2345678")
    StringUtils trim text through '5' should be ("678")
    StringUtils trim text through '8' should be ("")
    StringUtils trim text through '9' should be (text)
  }
  
  "StringUtils.Trim.from" should "trim from a target string" in {
    StringUtils trim text from "1" should be ("")
    StringUtils trim text from "12" should be ("")
    StringUtils trim text from "2" should be ("1")
    StringUtils trim text from "56" should be ("1234")
    StringUtils trim text from "7" should be ("123456")
    StringUtils trim text from "78" should be ("123456")
    StringUtils trim text from "8" should be ("1234567")
    StringUtils trim text from "9" should be (text)
    StringUtils trim text from text should be ("")
  }
  
  it should "trim from a target code point" in {
    StringUtils trim text from '1' should be ("")
    StringUtils trim text from '2' should be ("1")
    StringUtils trim text from '7' should be ("123456")
    StringUtils trim text from '8' should be ("1234567")
    StringUtils trim text from '9' should be (text)
  }
  
  "StringUtils.Trim.after" should "trim after a target string" in {
    StringUtils trim text after "1" should be ("1")
    StringUtils trim text after "12" should be ("12")
    StringUtils trim text after "56" should be ("123456")
    StringUtils trim text after "7" should be ("1234567")
    StringUtils trim text after "78" should be (text)
    StringUtils trim text after "8" should be (text)
    StringUtils trim text after "9" should be (text)
    StringUtils trim text after text should be (text)
  }
  
  it should "trim after a target code point" in {
    StringUtils trim text after '1' should be ("1")
    StringUtils trim text after '2' should be ("12")
    StringUtils trim text after '7' should be ("1234567")
    StringUtils trim text after '8' should be (text)
    StringUtils trim text after '9' should be (text)
  }
}