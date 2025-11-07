package com.example.espdisplay.models

data class Group(
    val id: Int,
    val groupNumber: Int,
    val name: String,
    val imageCount: Int = 0
)

data class GroupImage(
    val id: Int,
    val groupId: Int,
    val groupNumber: Int,
    val imageUrl: String,
    val timestamp: Long,
    val fileName: String
)
data class CreateGroupRequest(
    val name: String
)

data class CreateGroupResponse(
    val success: Boolean,
    val message: String,
    val groupId: Int?
)

data class GroupsResponse(
    val success: Boolean,
    val groups: List<Group>
)

data class GroupImagesResponse(
    val success: Boolean,
    val images: List<GroupImage>
)

data class DeleteResponse(
    val success: Boolean,
    val message: String
)