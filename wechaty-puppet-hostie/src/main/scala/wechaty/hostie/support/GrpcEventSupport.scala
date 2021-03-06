package wechaty.hostie.support

import com.typesafe.scalalogging.LazyLogging
import io.github.wechaty.grpc.puppet.Event.{EventResponse, EventType}
import io.grpc.stub.StreamObserver
import wechaty.puppet.events.EventEmitter
import wechaty.puppet.schemas.Event._
import wechaty.puppet.schemas.Puppet
import wechaty.puppet.schemas.Puppet.PuppetEventName

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2020-06-02
  */
trait GrpcEventSupport extends StreamObserver[EventResponse] {
  self: LazyLogging with EventEmitter with GrpcSupport with ContactRawSupport with MessageRawSupport =>
  private[wechaty] var idOpt: Option[String] = None

  override def onNext(v: EventResponse): Unit = {
    try {
      if (v.getType != EventType.EVENT_TYPE_HEARTBEAT) {
        val hearbeat = new EventHeartbeatPayload
        hearbeat.data = "onGrpcStreamEvent(%s)".format(v.getType)
        emit(PuppetEventName.HEARTBEAT, hearbeat)
      }
      v.getType match {
        case EventType.EVENT_TYPE_UNSPECIFIED =>
          logger.error("PuppetHostie onGrpcStreamEvent() got an EventType.EVENT_TYPE_UNSPECIFIED ")
        case other =>
          val payload = processEvent(other, v.getPayload)
          val eventName = Puppet.pbEventType2PuppetEventName.getOrElse(other, throw new IllegalAccessException("unsupport event " + other))
          emit(eventName, payload)
      }
    } catch {
      case e: Throwable =>
        logger.error("Grpc onNext", e)
    }
  }

  private def processEvent(eventType: EventType, data: String): EventPayload = {
    logger.debug("receive event:{},data:{}", eventType, data)
    eventType match {
      case EventType.EVENT_TYPE_SCAN =>
        Puppet.objectMapper.readValue(data, classOf[EventScanPayload])
      case EventType.EVENT_TYPE_DONG =>
        Puppet.objectMapper.readValue(data, classOf[EventDongPayload])
      case EventType.EVENT_TYPE_ERROR =>
        Puppet.objectMapper.readValue(data, classOf[EventErrorPayload])
      case EventType.EVENT_TYPE_HEARTBEAT =>
        Puppet.objectMapper.readValue(data, classOf[EventHeartbeatPayload])
      case EventType.EVENT_TYPE_FRIENDSHIP =>
        Puppet.objectMapper.readValue(data, classOf[EventFriendshipPayload])
      case EventType.EVENT_TYPE_LOGIN =>
        val value = Puppet.objectMapper.readValue(data, classOf[EventLoginPayload])
        idOpt = Some(value.contactId)
        value
      case EventType.EVENT_TYPE_LOGOUT =>
        idOpt = None
        Puppet.objectMapper.readValue(data, classOf[EventLogoutPayload])
      case EventType.EVENT_TYPE_MESSAGE =>
        Puppet.objectMapper.readValue(data, classOf[EventMessagePayload])
      case EventType.EVENT_TYPE_READY =>
        Puppet.objectMapper.readValue(data, classOf[EventReadyPayload])
      case EventType.EVENT_TYPE_ROOM_INVITE =>
        Puppet.objectMapper.readValue(data, classOf[EventRoomInvitePayload])
      case EventType.EVENT_TYPE_ROOM_JOIN =>
        Puppet.objectMapper.readValue(data, classOf[EventRoomJoinPayload])
      case EventType.EVENT_TYPE_ROOM_LEAVE =>
        Puppet.objectMapper.readValue(data, classOf[EventRoomLeavePayload])
      case EventType.EVENT_TYPE_ROOM_TOPIC =>
        Puppet.objectMapper.readValue(data, classOf[EventRoomTopicPayload])
      case EventType.EVENT_TYPE_RESET =>
        logger.warn("PuppetHostie onGrpcStreamEvent() got an EventType.EVENT_TYPE_RESET ?")
        Puppet.objectMapper.readValue(data, classOf[EventResetPayload])
      case other =>
        throw new IllegalAccessException("event not supported ,event:" + other)
    }

  }

  override def onError(throwable: Throwable): Unit = {
    logger.error("Grpc onError", throwable)
    logger.info("reconnect.....")
    new Thread(() => {
      reconnectStream()
    }).run()
  }

  override def onCompleted(): Unit = {
    logger.info("completed")
  }
}
