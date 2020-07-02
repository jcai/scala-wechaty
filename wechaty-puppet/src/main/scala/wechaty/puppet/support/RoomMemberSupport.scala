package wechaty.puppet.support

import com.github.benmanes.caffeine.cache.Cache
import wechaty.puppet.Puppet
import wechaty.puppet.schemas.Puppet.isBlank
import wechaty.puppet.schemas.Room.RoomMemberPayload

import scala.concurrent.{Future, Promise}

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2020-06-06
  */
trait RoomMemberSupport {
  self:Puppet =>
  private[puppet] val cacheRoomMemberPayload = createCache().asInstanceOf[Cache[String, RoomMemberPayload]]

  /**
    *
    * RoomMember
    *
    */
  def roomAnnounce(roomId: String): String

  def roomAnnounce(roomId: String, text: String): Unit

  def roomMemberList(roomId: String): Array[String]

  protected def roomMemberRawPayload(roomId: String, contactId: String): Future[RoomMemberPayload]

  /**
    * Concat roomId & contactId to one string
    */
  private def cacheKeyRoomMember (roomId    : String, contactId : String): String ={
    contactId + "@@@" + roomId
  }

  def roomMemberPayloadDirty (roomId: String): Unit =  {
    val contactIdList = this.roomMemberList(roomId)
    contactIdList.foreach(contactId => {
      val cacheKey = this.cacheKeyRoomMember(roomId, contactId)
      this.cacheRoomMemberPayload.invalidate(cacheKey)
    })
  }

  def roomMemberPayload ( roomId    : String, memberId : String): Future[RoomMemberPayload] = {

    if (isBlank(roomId)|| isBlank(memberId)) {
      throw new Error("roomId or memberId is blank")
    }

    /**
      * 1. Try to get from cache
      */
    val CACHE_KEY     = this.cacheKeyRoomMember(roomId, memberId)
    val cachedPayload = this.cacheRoomMemberPayload.getIfPresent(CACHE_KEY)

    if (cachedPayload != null) {
      return Promise[RoomMemberPayload].success(cachedPayload).future
    }

    /**
      * 2. Cache not found
      */
    import wechaty.puppet.schemas.Puppet.executionContext
    roomMemberRawPayload(roomId, memberId).map{ rawPayload=>
      this.cacheRoomMemberPayload.put(CACHE_KEY, rawPayload)
      rawPayload
    }
  }
}
