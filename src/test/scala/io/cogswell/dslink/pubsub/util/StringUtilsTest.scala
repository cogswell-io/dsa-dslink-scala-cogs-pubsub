package io.cogswell.dslink.pubsub.util

import io.cogswell.dslink.pubsub.DslinkTest
import scaldi.Injectable
import java.time.Instant
import org.joda.time.DateTime

class StringUtilsTest extends DslinkTest() {
  val ocho = "12345678"
  val dopp = "12341234"
  
  "StringUtils.Trim.until" should "trim until a target string" in {
    StringUtils trim ocho until "1" should be (ocho)
    StringUtils trim ocho until "12" should be (ocho)
    StringUtils trim ocho until "56" should be ("5678")
    StringUtils trim ocho until "78" should be ("78")
    StringUtils trim ocho until "8" should be ("8")
    StringUtils trim ocho until "9" should be (ocho)
    
    StringUtils trim dopp until "1" should be (dopp)
    StringUtils trim dopp until "12" should be (dopp)
    StringUtils trim dopp until "34" should be ("341234")
    StringUtils trim dopp until "41" should be ("41234")
    StringUtils trim dopp until "5" should be (dopp)
  }
  
  it should "trim until a target code point" in {
    StringUtils trim ocho until '1' should be (ocho)
    StringUtils trim ocho until '5' should be ("5678")
    StringUtils trim ocho until '8' should be ("8")
    StringUtils trim ocho until '9' should be (ocho)
    
    StringUtils trim dopp until '1' should be (dopp)
    StringUtils trim dopp until '3' should be ("341234")
    StringUtils trim dopp until '4' should be ("41234")
    StringUtils trim dopp until '5' should be (dopp)
  }

  "StringUtils.Trim.through" should "trim through a target string" in {
    StringUtils trim ocho through "1" should be ("2345678")
    StringUtils trim ocho through "12" should be ("345678")
    StringUtils trim ocho through "56" should be ("78")
    StringUtils trim ocho through "78" should be ("")
    StringUtils trim ocho through "8" should be ("")
    StringUtils trim ocho through "9" should be (ocho)
    StringUtils trim ocho through ocho should be ("")
    
    StringUtils trim dopp through "1" should be ("2341234")
    StringUtils trim dopp through "12" should be ("341234")
    StringUtils trim dopp through "34" should be ("1234")
    StringUtils trim dopp through "41" should be ("234")
    StringUtils trim dopp through "4" should be ("1234")
    StringUtils trim dopp through "5" should be (dopp)
    StringUtils trim dopp through dopp should be ("")
  }
  
  it should "trim through a target code point" in {
    StringUtils trim ocho through '1' should be ("2345678")
    StringUtils trim ocho through '5' should be ("678")
    StringUtils trim ocho through '8' should be ("")
    StringUtils trim ocho through '9' should be (ocho)
    
    StringUtils trim dopp through '1' should be ("2341234")
    StringUtils trim dopp through '2' should be ("341234")
    StringUtils trim dopp through '4' should be ("1234")
    StringUtils trim dopp through '5' should be (dopp)
  }
  
  "StringUtils.Trim.from" should "trim from a target string" in {
    StringUtils trim ocho from "1" should be ("")
    StringUtils trim ocho from "12" should be ("")
    StringUtils trim ocho from "2" should be ("1")
    StringUtils trim ocho from "56" should be ("1234")
    StringUtils trim ocho from "7" should be ("123456")
    StringUtils trim ocho from "78" should be ("123456")
    StringUtils trim ocho from "8" should be ("1234567")
    StringUtils trim ocho from "9" should be (ocho)
    StringUtils trim ocho from ocho should be ("")
    
    StringUtils trim dopp from "1" should be ("")
    StringUtils trim dopp from "12" should be ("")
    StringUtils trim dopp from "2" should be ("1")
    StringUtils trim dopp from "34" should be ("12")
    StringUtils trim dopp from "4" should be ("123")
    StringUtils trim dopp from "41" should be ("123")
    StringUtils trim dopp from "5" should be (dopp)
    StringUtils trim dopp from dopp should be ("")
  }
  
  it should "trim from a target code point" in {
    StringUtils trim ocho from '1' should be ("")
    StringUtils trim ocho from '2' should be ("1")
    StringUtils trim ocho from '7' should be ("123456")
    StringUtils trim ocho from '8' should be ("1234567")
    StringUtils trim ocho from '9' should be (ocho)
    
    StringUtils trim dopp from '1' should be ("")
    StringUtils trim dopp from '2' should be ("1")
    StringUtils trim dopp from '3' should be ("12")
    StringUtils trim dopp from '4' should be ("123")
    StringUtils trim dopp from '5' should be (dopp)
  }
  
  "StringUtils.Trim.after" should "trim after a target string" in {
    StringUtils trim ocho after "1" should be ("1")
    StringUtils trim ocho after "12" should be ("12")
    StringUtils trim ocho after "56" should be ("123456")
    StringUtils trim ocho after "7" should be ("1234567")
    StringUtils trim ocho after "78" should be (ocho)
    StringUtils trim ocho after "8" should be (ocho)
    StringUtils trim ocho after "9" should be (ocho)
    StringUtils trim ocho after ocho should be (ocho)
    
    StringUtils trim dopp after "1" should be ("1")
    StringUtils trim dopp after "12" should be ("12")
    StringUtils trim dopp after "34" should be ("1234")
    StringUtils trim dopp after "41" should be ("12341")
    StringUtils trim dopp after "5" should be (dopp)
    StringUtils trim dopp after dopp should be (dopp)
  }
  
  it should "trim after a target code point" in {
    StringUtils trim ocho after '1' should be ("1")
    StringUtils trim ocho after '2' should be ("12")
    StringUtils trim ocho after '7' should be ("1234567")
    StringUtils trim ocho after '8' should be (ocho)
    StringUtils trim ocho after '9' should be (ocho)
    
    StringUtils trim dopp after '1' should be ("1")
    StringUtils trim dopp after '2' should be ("12")
    StringUtils trim dopp after '4' should be ("1234")
    StringUtils trim dopp after '5' should be (dopp)
  }
  
  "StringUtils.Split.onFirst" should "split on the first occurence" in {
    StringUtils split ocho onFirst "1" should be (("", "2345678"))
    StringUtils split ocho onFirst "12" should be (("", "345678"))
    StringUtils split ocho onFirst "56" should be (("1234", "78"))
    StringUtils split ocho onFirst "7" should be (("123456", "8"))
    StringUtils split ocho onFirst "78" should be (("123456", ""))
    StringUtils split ocho onFirst "8" should be (("1234567", ""))
    StringUtils split ocho onFirst "9" should be ((ocho, ""))
    StringUtils split ocho onFirst ocho should be ((ocho, ""))
    
    StringUtils split dopp onFirst "1" should be (("", "2341234"))
    StringUtils split dopp onFirst "12" should be (("", "341234"))
    StringUtils split dopp onFirst "34" should be (("12", "1234"))
    StringUtils split dopp onFirst "41" should be (("123", "234"))
    StringUtils split dopp onFirst "5" should be ((dopp, ""))
    StringUtils split dopp onFirst dopp should be ((dopp, ""))
  }
  
  it should "split on the first occurence" in {
    StringUtils split ocho onFirst '1' should be (("", "2345678"))
    StringUtils split ocho onFirst '2' should be (("1", "345678"))
    StringUtils split ocho onFirst '7' should be (("123456", "8"))
    StringUtils split ocho onFirst '8' should be (("1234567", ""))
    StringUtils split ocho onFirst '9' should be ((ocho, ""))
    
    StringUtils split dopp onFirst '1' should be (("", "2341234"))
    StringUtils split dopp onFirst '2' should be (("1", "341234"))
    StringUtils split dopp onFirst '3' should be (("12", "41234"))
    StringUtils split dopp onFirst '4' should be (("123", "1234"))
    StringUtils split dopp onFirst '5' should be ((dopp, ""))
  }
}