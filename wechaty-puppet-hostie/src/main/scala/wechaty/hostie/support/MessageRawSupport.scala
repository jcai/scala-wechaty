package wechaty.hostie.support

import com.typesafe.scalalogging.LazyLogging
import io.github.wechaty.grpc.PuppetGrpc
import io.github.wechaty.grpc.puppet.Base.DingRequest
import io.github.wechaty.grpc.puppet.Message
import io.github.wechaty.grpc.puppet.Message.MessagePayloadRequest
import wechaty.puppet.ResourceBox
import wechaty.puppet.schemas.Image.ImageType.Type
import wechaty.puppet.schemas.Message.{MessagePayload, MessageType}
import wechaty.puppet.schemas.MiniProgram.MiniProgramPayload
import wechaty.puppet.schemas.Puppet
import wechaty.puppet.schemas.Puppet._
import wechaty.puppet.schemas.UrlLink.UrlLinkPayload
import wechaty.puppet.support.MessageSupport

import scala.concurrent.Future

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2020-06-02
  */
trait MessageRawSupport {
  self: LazyLogging with GrpcSupport with MessageSupport =>
  /**
    * message
    */
  override def messageContact(messageId: String): String = {
    val request  = Message.MessageContactRequest.newBuilder()
      .setId(messageId)
      .build()
    val response = grpcClient.messageContact(request)
    response.getId
  }

  var count = 0

  override def messageFile(messageId: String): ResourceBox = {
    val request  = Message.MessageFileRequest.newBuilder().setId(messageId).build()
    val response = grpcClient.messageFile(request)
    ResourceBox.fromJson(response.getFilebox)
  }

  override def messageMiniProgram(messageId: String): MiniProgramPayload = {
    val request = Message.MessageMiniProgramRequest.newBuilder()
      .setId(messageId)
      .build()

    val response    = grpcClient.messageMiniProgram(request)
    val miniProgram = response.getMiniProgram
    logger.debug("MP json:{}", miniProgram)
    Puppet.objectMapper.readValue(miniProgram, classOf[MiniProgramPayload])
  }

  override def messageUrl(messageId: String): UrlLinkPayload = {
    val request = Message.MessageUrlRequest.newBuilder()
      .setId(messageId)
      .build()

    val response = grpcClient.messageUrl(request)
    val urlLink  = response.getUrlLink
    Puppet.objectMapper.readValue(urlLink, classOf[UrlLinkPayload])

  }

  override def messageSendContact(conversationId: String, contactId: String): String = {
    val request = Message.MessageSendContactRequest.newBuilder()
      .setContactId(contactId)
      .setConversationId(conversationId)
      .build()


    val response = grpcClient.messageSendContact(request)
    response.getId.getValue
  }

  override def messageSendFile(conversationId: String, file: ResourceBox): String = {
    val fileJson = file.toJson()

    val request = Message.MessageSendFileRequest.newBuilder()
      .setConversationId(conversationId)
      .setFilebox(fileJson)
      .build()

    val response = grpcClient.messageSendFile(request)
    response.getId.getValue
  }

  override def messageSendMiniProgram(conversationId: String, miniProgramPayload: MiniProgramPayload): String = {
    val request = Message.MessageSendMiniProgramRequest.newBuilder()
      .setConversationId(conversationId)
      .setMiniProgram(objectMapper.writeValueAsString(miniProgramPayload))
      .build()

    val response = grpcClient.messageSendMiniProgram(request)
    response.getId.getValue
  }

  override def messageSendText(conversationId: String, text: String, mentionIdList: Array[String]): Future[String] = {
    import scala.collection.JavaConverters._
    val request = Message.MessageSendTextRequest.newBuilder()
      .setConversationId(conversationId)
      .setText(text)
      .addAllMentonalIds(asJavaIterable(mentionIdList.toIterable))
      .build()

    asyncCall(PuppetGrpc.getMessageSendTextMethod,request)
      .map(response=>response.getId.getValue)
  }

  override def messageSendUrl(conversationId: String, urlLinkPayload: UrlLinkPayload): String = {
    val request = Message.MessageSendUrlRequest.newBuilder()
      .setConversationId(conversationId)
      .setUrlLink(objectMapper.writeValueAsString(urlLinkPayload))
      .build()

    val response = grpcClient.messageSendUrl(request)
    response.getId.getValue
  }

  override def messageRecall(messageId: String): Boolean = {
    val request = Message.MessageRecallRequest.newBuilder()
      .setId(messageId)
      .build()

    val response = grpcClient.messageRecall(request)
    response.getSuccess
  }


  override protected def messageRawPayload(id: String): MessagePayload = {
    logger.info("PuppetHostie MessagePayload({})", id)
    val response = grpcClient.messagePayload(MessagePayloadRequest.newBuilder().setId(id).build())
    logger.info("message payload:{}", response)
    val messagePayload = new MessagePayload
    messagePayload.id = response.getId
    messagePayload.mentionIdList = response.getMentionIdsList.toArray(Array[String]())
    messagePayload.filename = response.getFilename
    messagePayload.text = response.getText
    messagePayload.timestamp = response.getTimestamp
    if (response.getTypeValue > MessageType.Video.id)
      messagePayload.`type` = MessageType.Unknown
    else
      messagePayload.`type` = MessageType.apply(response.getTypeValue)

    messagePayload.fromId = response.getFromId
    messagePayload.roomId = response.getRoomId
    messagePayload.toId = response.getToId

    messagePayload
  }


  override def messageImage(messageId: String, imageType: Type): ResourceBox = {
    val request = Message.MessageImageRequest.newBuilder()
    request.setId(messageId)
    request.setType(Message.ImageType.forNumber(imageType.id))


    val response = this.grpcClient.messageImage(request.build())
    logger.debug("image response:{}", response)

    val jsonText = response.getFilebox
    ResourceBox.fromJson(jsonText)
  }

  override protected def ding(data: String): Unit = {
    val request = DingRequest.newBuilder().setData(data).build()
    grpcClient.ding(request)
  }
}
