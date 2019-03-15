package account

import org.springframework.data.domain.Page

data class CreateAccount(val name: String)
data class CreateCommand(val accountId: Int, val transactionId: Long, val amount: Long)

data class PageResponse<T>(
        val content: List<T>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalPages: Int,
        val offset: Long,
        val numberOfElements: Int,
        val totalElements: Long,
        val first: Boolean,
        val last: Boolean
)

fun <T> Page<T>.response() = PageResponse<T>(
        content,
        number,
        size,
        totalPages,
        pageable.offset,
        numberOfElements,
        totalElements,
        isFirst,
        isLast
)
