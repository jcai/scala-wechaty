package wechaty.puppet.support

import com.github.benmanes.caffeine.cache.Cache
import wechaty.puppet.Puppet
import wechaty.puppet.schemas.Puppet
import wechaty.puppet.schemas.Room.RoomPayload

/**
  *
  * @author <a href="mailto:jcai@ganshane.com">Jun Tsai</a>
  * @since 2020-06-06
  */
trait RoomSupport {
  self:Puppet =>
  private val cacheRoomPayload = createCache().asInstanceOf[Cache[String, RoomPayload]]

  def roomAdd(roomId: String, contactId: String): Unit

  //   def roomAvatar (roomId: String)                          : FileBox>
  def roomCreate(contactIdList: Array[String], topic: String): String

  def roomDel(roomId: String, contactId: String): Unit

  def roomList(): Array[String]

  def roomQRCode(roomId: String): String

  def roomQuit(roomId: String): Unit

  def roomTopic(roomId: String): String

  def roomTopic(roomId: String, topic: String): Unit

  protected def roomRawPayload(roomId: String): RoomPayload
  def roomPayload ( roomId: String): RoomPayload =  {

    if (Puppet.isBlank(roomId)) {
      throw new Error("no room id")
    }

    /**
      * 1. Try to get from cache first
      */
    val cachedPayload = cacheRoomPayload.getIfPresent(roomId)
    if (cachedPayload != null) {
      return cachedPayload
    }

    /**
      * 2. Cache not found
      */
    val rawPayload = this.roomRawPayload(roomId)

    this.cacheRoomPayload.put(roomId, rawPayload)
    rawPayload
  }
  def roomPayloadDirty (roomId: String): Unit ={
    this.cacheRoomPayload.invalidate(roomId)
  }
}
