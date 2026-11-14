package com.example.multipromptchat.data.local

import com.example.multipromptchat.domain.Conversation
import com.example.multipromptchat.domain.Message
import com.example.multipromptchat.domain.MessageRole
import com.example.multipromptchat.domain.PromptTemplate

fun PromptTemplateEntity.toDomain(): PromptTemplate = PromptTemplate(
    id = id,
    name = name,
    systemPrompt = systemPrompt,
    temperature = temperature,
    topP = topP,
    resetContextOnSwitch = resetContextOnSwitch,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PromptTemplate.toEntity(): PromptTemplateEntity = PromptTemplateEntity(
    id = id,
    name = name,
    systemPrompt = systemPrompt,
    temperature = temperature,
    topP = topP,
    resetContextOnSwitch = resetContextOnSwitch,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    promptId = promptId,
    promptSnapshot = promptSnapshot,
    createdAt = createdAt,
    updatedAt = updatedAt,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    cacheHitTokens = cacheHitTokens,
    cacheMissTokens = cacheMissTokens,
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    role = MessageRole.fromApi(role),
    content = content,
    reasoning = reasoning,
    createdAt = createdAt,
)
