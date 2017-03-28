package io.cogswell.dslink.pubsub.util

import org.dsa.iot.dslink.util.handler.Handler
import org.slf4j.LoggerFactory
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import org.dsa.iot.dslink.node.actions.Action
import org.dsa.iot.dslink.node.actions.Action
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.actions.ActionResult
import org.dsa.iot.dslink.node.Permission
import org.dsa.iot.dslink.node.actions.Parameter
import org.dsa.iot.dslink.methods.responses.ListResponse
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.methods.requests.ListRequest

/**
 * Class representing a parameter attached to an Action.
 * 
 * @param name the name of the parameter
 * @param valueType the type of the value
 * @param value the optional value
 */
case class ActionParam(
    name: String,
    valueType: ValueType,
    value: Option[Value] = None
) {
  def parameter: Parameter = {
    value match {
      case None => new Parameter(name, valueType)
      case Some(v) => new Parameter(name, valueType, v)
    }
  }
}

/**
 * Class representing the data passed to an Action when
 * it is invoked.
 */
case class ActionData(
    params: Seq[ActionParam],
    result: ActionResult
) {
  val dataMap: Map[String, ActionParam] = params.map {
    case ActionParam(name, valueType, value) =>
      val resultValue = Option(result.getParameter(name, valueType))
      (name -> ActionParam(name, valueType, resultValue))
  } toMap
}

object LinkUtils {
  /**
   * Creates a new Handler wrapped around the supplied action,
   * and logs any un-handled Exceptions thrown by the action.
   * 
   * @param action the action which will be invoked by the Handler
   * 
   * @return the new Handler
   */
  def handler[T](action: (T) => Unit): Handler[T] = new Handler[T] {
    private val logger = LoggerFactory.getLogger(action.getClass)
    
    def handle(value: T): Unit = {
      try {
        action(value)
      } catch {
        case error: Throwable => {
          logger.error("Error in Handler:", error)
          throw error
        }
      }
    }
  }
  
  /**
   * Simplifies the creation of a new Action with parameters.
   * 
   * @param params the parameters which should be attached to this action
   * @param permission 
   */
  def action(
      params: Seq[ActionParam],
      permission: Permission = Permission.READ
  )(action: (ActionData) => Unit): Action = {
    val actionHandler = handler { actionResult: ActionResult =>
      action(ActionData(params, actionResult))
    }
    
    val linkAction = new Action(permission, actionHandler)
    
    params.foreach(p => linkAction.addParameter(p.parameter))
    
    linkAction
  }
  
  /**
   * Listen for updates to the supplied node, and call the supplied
   * function with the updates.
   * 
   * @param link the DSLink
   * @param node the Node to which we should listen for updates
   * @param listener the listener to invoke for each update
   */
  def listen(link: DSLink, node: Node)(listener: (ListResponse) => Unit): Unit = {
    val listRequest = new ListRequest(node.getPath)
    val listHandler = new Handler[ListResponse] {
      override def handle(response: ListResponse) {
        listener(response)
      }
    }

    link.getRequester.list(listRequest, listHandler)
  }
}