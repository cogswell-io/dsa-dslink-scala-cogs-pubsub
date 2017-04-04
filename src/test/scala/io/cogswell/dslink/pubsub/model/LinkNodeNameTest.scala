package io.cogswell.dslink.pubsub.model

import io.cogswell.dslink.pubsub.DslinkTest

class LinkNodeNameTest extends DslinkTest() {
  "LinkNodeName.fromNode" should "correctly identify an action" in {
    LinkNodeName.fromNodeId(null, "alias") should be (None)
    LinkNodeName.fromNodeId("", "alias") should be (None)
    LinkNodeName.fromNodeId("action", "alias") should be (None)
    LinkNodeName.fromNodeId("action:", "alias") should be (None)
    
    LinkNodeName.fromNodeId("action:act", null) should be (Some(ActionNodeName("act", "act")))
    LinkNodeName.fromNodeId("action:act", "") should be (Some(ActionNodeName("act", "act")))
    LinkNodeName.fromNodeId("action:act", "alias") should be (Some(ActionNodeName("act", "alias")))
  }
  
  it should "correctly identify a connection" in {
    LinkNodeName.fromNodeId(null, "alias") should be (None)
    LinkNodeName.fromNodeId("", "alias") should be (None)
    LinkNodeName.fromNodeId("connection", "alias") should be (None)
    LinkNodeName.fromNodeId("connection:", "alias") should be (None)
    
    LinkNodeName.fromNodeId("connection:conn", null) should be (Some(ConnectionNodeName("conn")))
    LinkNodeName.fromNodeId("connection:conn", "") should be (Some(ConnectionNodeName("conn")))
    LinkNodeName.fromNodeId("connection:conn", "alias") should be (Some(ConnectionNodeName("conn")))
  }
  
  it should "correctly identify a info" in {
    LinkNodeName.fromNodeId(null, "alias") should be (None)
    LinkNodeName.fromNodeId("", "alias") should be (None)
    LinkNodeName.fromNodeId("info", "alias") should be (None)
    LinkNodeName.fromNodeId("info:", "alias") should be (None)
    
    LinkNodeName.fromNodeId("info:status", null) should be (Some(InfoNodeName("status", "status")))
    LinkNodeName.fromNodeId("info:status", "") should be (Some(InfoNodeName("status", "status")))
    LinkNodeName.fromNodeId("info:status", "alias") should be (Some(InfoNodeName("status", "alias")))
  }
  
  it should "correctly identify a publisher" in {
    LinkNodeName.fromNodeId(null, "alias") should be (None)
    LinkNodeName.fromNodeId("", "alias") should be (None)
    LinkNodeName.fromNodeId("publisher", "alias") should be (None)
    LinkNodeName.fromNodeId("publisher:", "alias") should be (None)
    
    LinkNodeName.fromNodeId("publisher:pub", null) should be (Some(PublisherNodeName("pub")))
    LinkNodeName.fromNodeId("publisher:pub", "") should be (Some(PublisherNodeName("pub")))
    LinkNodeName.fromNodeId("publisher:pub", "alias") should be (Some(PublisherNodeName("pub")))
  }
  
  it should "correctly identify a subscriber" in {
    LinkNodeName.fromNodeId(null, "alias") should be (None)
    LinkNodeName.fromNodeId("", "alias") should be (None)
    LinkNodeName.fromNodeId("subscriber", "alias") should be (None)
    LinkNodeName.fromNodeId("subscriber:", "alias") should be (None)
    
    LinkNodeName.fromNodeId("subscriber:sub", null) should be (Some(SubscriberNodeName("sub")))
    LinkNodeName.fromNodeId("subscriber:sub", "") should be (Some(SubscriberNodeName("sub")))
    LinkNodeName.fromNodeId("subscriber:sub", "alias") should be (Some(SubscriberNodeName("sub")))
  }
  
  it should "not mis-identify an unknown id" in {
    LinkNodeName.fromNodeId(null, null) should be (None)
    LinkNodeName.fromNodeId(null, "alias") should be (None)
    LinkNodeName.fromNodeId("", null) should be (None)
    LinkNodeName.fromNodeId("", "alias") should be (None)
    LinkNodeName.fromNodeId(":", null) should be (None)
    LinkNodeName.fromNodeId(":", "alias") should be (None)
    LinkNodeName.fromNodeId(":huh", null) should be (None)
    LinkNodeName.fromNodeId(":huh", "alias") should be (None)
    LinkNodeName.fromNodeId("huh", null) should be (None)
    LinkNodeName.fromNodeId("huh", "alias") should be (None)
    LinkNodeName.fromNodeId("huh:", null) should be (None)
    LinkNodeName.fromNodeId("huh:", "alias") should be (None)
    LinkNodeName.fromNodeId("huh:huh", null) should be (None)
    LinkNodeName.fromNodeId("huh:huh", "alias") should be (None)
  }
  
  "LinkNodeName.id" should "be correctly assembled for each category" in {
    ActionNodeName("act", "Action").id should be ("action:act")
    ConnectionNodeName("conn").id should be ("connection:conn")
    InfoNodeName("status", "alias").id should be ("info:status")
    PublisherNodeName("pub").id should be ("publisher:pub")
    SubscriberNodeName("sub").id should be ("subscriber:sub")
  }
  
  it should "supply a key which is only identified by using its id" in {
    ActionNodeName("act", "alias").key.hashCode should be (ActionNodeName("act", "otro").key.hashCode)
    ActionNodeName("act", "alias").key.equals(ActionNodeName("act", "otro").key) should be (true)
  }
}