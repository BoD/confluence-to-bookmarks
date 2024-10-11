/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.confluencetobookmarks.main

import com.apollographql.apollo.ApolloClient
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jraf.confluencetobookmarks.PageTreeChildrenQuery
import java.util.Base64

private const val DEFAULT_PORT = 8080

private const val ENV_PORT = "PORT"

private const val PATH_DOMAIN = "domain"
private const val PATH_EMAIL_AND_API_TOKEN = "emailAndApiToken"
private const val PATH_PAGE_ID = "pageId"

fun main() {
  val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
  embeddedServer(Netty, listenPort) {
    install(DefaultHeaders)
    install(ContentNegotiation) {
      json(Json {
        prettyPrint = true
      })
    }
    install(StatusPages) {
      status(HttpStatusCode.NotFound) { call, status ->
        call.respondText(
          text = "Usage: ${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}/<Domain>/<Email:ApiToken>/<Page id>\n\nSee https://github.com/BoD/confluence-to-bookmarks for more info.",
          status = status
        )
      }
    }

    routing {
      get("{$PATH_DOMAIN}/{$PATH_EMAIL_AND_API_TOKEN}/{$PATH_PAGE_ID}") {
        val domain = call.parameters[PATH_DOMAIN]!!
        val emailAndApiToken = call.parameters[PATH_EMAIL_AND_API_TOKEN]!!
        val pageId = call.parameters[PATH_PAGE_ID]!!
        val jsonBookmarks = fetchPages(
          domain = domain,
          emailAndApiToken = emailAndApiToken,
          pageId = pageId,
        )
        call.respond(jsonBookmarks)
      }
    }
  }.start(wait = true)
}

suspend fun fetchPages(domain: String, emailAndApiToken: String, pageId: String): BookmarkRoot {
  val apolloClient = ApolloClient.Builder()
    .serverUrl("https://$domain.atlassian.net/cgraphql")
    .build()

  return apolloClient.use {
    apolloClient.query(PageTreeChildrenQuery(pageId = pageId))
      .addHttpHeader(
        "Authorization",
        "Basic ${Base64.getEncoder().encodeToString(emailAndApiToken.toByteArray())}"
      )
      .execute()
      .dataAssertNoErrors
      .ptpage!!
      .toBookmarkRoot(domain)
  }
}

@Suppress("DEPRECATION")
private fun PageTreeChildrenQuery.Ptpage.toBookmarkRoot(domain: String): BookmarkRoot {
  return BookmarkRoot(
    version = 1,
    bookmarks = listOf(
      Bookmark(
        title = (pageTreeInfoFragment.title ?: "(no title)") + " Home",
        url = "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui,
        bookmarks = null,
      )
    ) + (children?.nodes?.mapNotNull { it?.toBookmark(domain) } ?: emptyList()),
  )
}

@Suppress("DEPRECATION")
private fun PageTreeChildrenQuery.Node.toBookmark(domain: String): Bookmark {
  val hasChildren = children?.nodes?.isNotEmpty() == true
  return Bookmark(
    title = pageTreeInfoFragment.title ?: "(no title)",
    url = if (!hasChildren) {
      "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui
    } else {
      null
    },
    bookmarks = if (!hasChildren) {
      null
    } else {
      listOf(
        Bookmark(
          title = (pageTreeInfoFragment.title ?: "(no title)") + " Home",
          url = "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui,
          bookmarks = null,
        )
      ) + (children?.nodes?.mapNotNull { it?.toBookmark(domain) } ?: emptyList())
    },
  )
}

@Suppress("DEPRECATION")
private fun PageTreeChildrenQuery.Node1.toBookmark(domain: String): Bookmark {
  val hasChildren = children?.nodes?.isNotEmpty() == true
  return Bookmark(
    title = pageTreeInfoFragment.title ?: "(no title)",
    url = if (!hasChildren) {
      "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui
    } else {
      null
    },
    bookmarks = if (!hasChildren) {
      null
    } else {
      listOf(
        Bookmark(
          title = (pageTreeInfoFragment.title ?: "(no title)") + " Home",
          url = "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui,
          bookmarks = null,
        )
      ) + (children?.nodes?.mapNotNull { it?.toBookmark(domain) } ?: emptyList())
    },
  )
}

@Suppress("DEPRECATION")
private fun PageTreeChildrenQuery.Node2.toBookmark(domain: String): Bookmark {
  val hasChildren = children?.nodes?.isNotEmpty() == true
  return Bookmark(
    title = pageTreeInfoFragment.title ?: "(no title)",
    url = if (!hasChildren) {
      "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui
    } else {
      null
    },
    bookmarks = if (!hasChildren) {
      null
    } else {
      listOf(
        Bookmark(
          title = (pageTreeInfoFragment.title ?: "(no title)") + " Home",
          url = "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui,
          bookmarks = null,
        )
      ) + (children?.nodes?.mapNotNull { it?.toBookmark(domain) } ?: emptyList())
    },
  )
}

@Suppress("DEPRECATION")
private fun PageTreeChildrenQuery.Node3.toBookmark(domain: String): Bookmark {
  val hasChildren = children?.nodes?.isNotEmpty() == true
  return Bookmark(
    title = pageTreeInfoFragment.title ?: "(no title)",
    url = if (!hasChildren) {
      "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui
    } else {
      null
    },
    bookmarks = if (!hasChildren) {
      null
    } else {
      listOf(
        Bookmark(
          title = (pageTreeInfoFragment.title ?: "(no title)") + " Home",
          url = "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui,
          bookmarks = null,
        )
      ) + (children?.nodes?.mapNotNull { it?.toBookmark(domain) } ?: emptyList())
    },
  )
}

@Suppress("DEPRECATION")
private fun PageTreeChildrenQuery.Node4.toBookmark(domain: String): Bookmark {
  return Bookmark(
    title = pageTreeInfoFragment.title ?: "(no title)",
    url = "https://$domain.atlassian.net/wiki" + pageTreeInfoFragment.links?.webui,
    bookmarks = null,
  )
}


@Serializable
data class BookmarkRoot(
  val version: Int,
  val bookmarks: List<Bookmark>,
)

@Serializable
data class Bookmark(
  val title: String,
  val url: String?,
  val bookmarks: List<Bookmark>?,
)
