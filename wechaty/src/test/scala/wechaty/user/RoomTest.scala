package wechaty.user

import io.github.wechaty.grpc.PuppetGrpc
import io.github.wechaty.grpc.puppet.Message.{MessagePayloadResponse, MessageType}
import io.github.wechaty.grpc.puppet.Room.{RoomListResponse, RoomPayloadRequest, RoomPayloadResponse}
import org.grpcmock.GrpcMock._
import org.junit.jupiter.api.{Assertions, BeforeEach, Test}
import wechaty.TestBase
import wechaty.puppet.schemas.Event.{EventMessagePayload, EventRoomJoinPayload, EventRoomLeavePayload, EventRoomTopicPayload}
import wechaty.puppet.schemas.Puppet.PuppetEventName

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2020-06-15
  */
class RoomTest extends TestBase{
  val roomId = "roomIdxxxx"
  val contactId="id1"
  val inviterId="inviterId"
  val removerId=inviterId
  val messageId="messageId"
  var room:Room = _
  @BeforeEach
  def constructTestCase: Unit ={
    val request = RoomPayloadRequest.newBuilder()
    request.setId(roomId)
    val response = RoomPayloadResponse.newBuilder()
    response.setId(roomId)
    stubFor(unaryMethod(PuppetGrpc.getRoomPayloadMethod)
        .withRequest(request.build())
      .willReturn(response.build())
    )
    room = Room.load(roomId)(instance).get
  }
  @Test
  def testRoomMessageEvent: Unit ={
    val response = MessagePayloadResponse.newBuilder()
      .setText("testMessage")
      .setType(MessageType.MESSAGE_TYPE_TEXT)
      .setRoomId(roomId)
      .build()
    stubFor(unaryMethod(PuppetGrpc.getMessagePayloadMethod)
      .willReturn(response))

    val payload = new EventMessagePayload
    payload.messageId=messageId

//    mockEvent(EventType.EVENT_TYPE_MESSAGE->payload)


    var reachFlag = false
    room.onMessage(message=>{
      reachFlag = true
    })

    instance.puppet.emit(PuppetEventName.MESSAGE,payload)
//    awaitEventCompletion(10,TimeUnit.SECONDS)
    Assertions.assertTrue(reachFlag)
  }
  @Test
  def testRoomMessageEvent2: Unit ={
    val response2 = MessagePayloadResponse.newBuilder()
      .setText("testMessage")
      .setType(MessageType.MESSAGE_TYPE_TEXT)
      .setRoomId("roomId2")
      .build()
    stubFor(unaryMethod(PuppetGrpc.getMessagePayloadMethod)
      .willReturn(response2))

    var reachFlag = false
    room.onMessage(message=>{
      reachFlag = true
    })

    val payload = new EventMessagePayload
    payload.messageId=messageId
    instance.puppet.emit(PuppetEventName.MESSAGE,payload)
    Assertions.assertFalse(reachFlag)
  }
  @Test
  def testJoinEvent: Unit ={
    var reachFlag = false
    room.onJoin({case (list,inviter,date)=>
      reachFlag = true
      Assertions.assertEquals(inviterId,inviter.id)
    })

    val payload = new EventRoomJoinPayload
    payload.inviteeIdList = Array(contactId)
    payload.inviterId = inviterId
    payload.roomId = roomId
    instance.puppet.emit(PuppetEventName.ROOM_JOIN,payload)

    Assertions.assertTrue(reachFlag)
  }
  @Test
  def testLeaveEvent: Unit ={
    var reachFlag = false
    room.onLeave({case (list,remover,date)=>
      reachFlag = true
      Assertions.assertEquals(removerId,remover.id)
    })

    val payload = new EventRoomLeavePayload
    payload.removeeIdList= Array(contactId)
    payload.removerId= removerId
    payload.roomId = roomId
    instance.puppet.emit(PuppetEventName.ROOM_LEAVE,payload)

    Assertions.assertTrue(reachFlag)
  }
  @Test
  def testTopicEvent: Unit ={
    var reachFlag = false
    room.onTopic({case (concact,date)=>
      reachFlag = true
      Assertions.assertEquals(inviterId,concact.id)
    })

    val payload = new EventRoomTopicPayload
    payload.changerId= inviterId
    payload.newTopic = "newTopic"
    payload.roomId = roomId
    instance.puppet.emit(PuppetEventName.ROOM_TOPIC,payload)

    Assertions.assertTrue(reachFlag)
  }
  @Test
  def testRoomSearch: Unit ={
    val roomId2 = "roomId2"
    val response = RoomListResponse.newBuilder()
    response.addIds(roomId)
    response.addIds(roomId2)

    stubFor(unaryMethod(PuppetGrpc.getRoomListMethod)
      .willReturn(response.build()))

    val roomPayloadRequest = RoomPayloadRequest.newBuilder()
    roomPayloadRequest.setId(roomId2)
    val roomPayloadResponse= RoomPayloadResponse.newBuilder()
    roomPayloadResponse.setId(roomId2)
    stubFor(unaryMethod(PuppetGrpc.getRoomPayloadMethod)
      .withRequest(roomPayloadRequest.build())
      .willReturn(roomPayloadResponse.build())
    )



    Assertions.assertEquals(roomId,Room.find(None).get.id)
    Assertions.assertEquals(roomId2,Room.find(_.id == roomId2).get.id)
  }
}
