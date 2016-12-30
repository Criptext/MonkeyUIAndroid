package com.criptext.monkeykitui.recycler

import com.criptext.monkeykitui.util.SimpleMonkeyItem
import org.amshove.kluent.`should equal`
import org.junit.Test

/**
 * Created by gesuwall on 12/30/16.
 */

class `MesssagesList Test` {

    @Test
    fun `get() should never return an EndItem object`() {
        val list = mutableListOf(SimpleMonkeyItem("7", 123), SimpleMonkeyItem("8", 124),
                SimpleMonkeyItem("9", 125))
        val messagesList = MessagesList("test")
        messagesList.insertMessages(list, false)

        (messagesList[0] is EndItem) `should equal` false
        messagesList[0].getMessageId() `should equal` "7"
        messagesList[1].getMessageId() `should equal` "8"
        messagesList[2].getMessageId() `should equal` "9"

        messagesList.hasReachedEnd = true

        (messagesList[0] is EndItem) `should equal` false
        messagesList[0].getMessageId() `should equal` "7"
        messagesList[1].getMessageId() `should equal` "8"
        messagesList[2].getMessageId() `should equal` "9"
    }


    @Test
    fun `getItemAt(0) will return an EndItem object if hasReachedEnd equals true`() {
        val list = mutableListOf(SimpleMonkeyItem("7", 123), SimpleMonkeyItem("8", 124),
                SimpleMonkeyItem("9", 125))
        val messagesList = MessagesList("test")
        messagesList.insertMessages(list, false)

        (messagesList.getItemAt(0) is EndItem) `should equal` true
    }
}